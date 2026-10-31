package com.pyb110.onekey

import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import roro.stellar.Stellar
import roro.stellar.StellarHelper
import java.io.InputStream
import java.io.OutputStream

enum class PrivBackend(val label: String) {
    STELLAR("Stellar"),
    SHIZUKU("Shizuku")
}

class PrivProcess(
    private val inS: InputStream,
    private val outS: OutputStream,
    private val errS: InputStream,
    private val waiter: () -> Int
) {
    fun getInputStream(): InputStream = inS
    fun getOutputStream(): OutputStream = outS
    fun getErrorStream(): InputStream = errS
    fun waitFor(): Int = waiter()
}

object PrivService {
    fun pick(): PrivBackend? = when {
        runCatching { Stellar.pingBinder() }.getOrDefault(false) -> PrivBackend.STELLAR
        runCatching { Shizuku.pingBinder() }.getOrDefault(false) -> PrivBackend.SHIZUKU
        else -> null
    }

    fun granted(): Boolean = when (pick()) {
        PrivBackend.STELLAR -> runCatching { Stellar.checkSelfPermission("stellar") }.getOrDefault(false)
        PrivBackend.SHIZUKU -> runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        null -> false
    }

    fun request(code: Int) {
        when (pick()) {
            PrivBackend.STELLAR -> Stellar.requestPermission("stellar", code)
            PrivBackend.SHIZUKU -> Shizuku.requestPermission(code)
            null -> Unit
        }
    }

    fun newProcess(cmd: Array<String>, env: Array<String>?, dir: String?): PrivProcess =
        when (pick()) {
            PrivBackend.STELLAR -> {
                @Suppress("UNCHECKED_CAST")
                val p = Stellar.newProcess(cmd as Array<String?>, env as Array<String?>?, dir)
                PrivProcess(p.getInputStream(), p.getOutputStream(), p.getErrorStream()) { p.waitFor() }
            }
            PrivBackend.SHIZUKU -> {
                val binder = Shizuku.getBinder() ?: error("Shizuku binder null")
                val svc = IShizukuService.Stub.asInterface(binder) ?: error("Shizuku service null")
                val p = svc.newProcess(cmd, env, dir)
                PrivProcess(
                    ParcelFileDescriptor.AutoCloseInputStream(p.getInputStream()),
                    ParcelFileDescriptor.AutoCloseOutputStream(p.getOutputStream()),
                    ParcelFileDescriptor.AutoCloseInputStream(p.getErrorStream())
                ) { p.waitFor() }
            }
            null -> error("no privileged backend (Stellar/Shizuku)")
        }

    fun openManager(context: Context) {
        if (runCatching { StellarHelper.openManager(context) }.getOrDefault(false)) return
        val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) {
            runCatching { context.startActivity(intent) }
        }
    }
}
