package com.pyb110.onekey

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryEntry(
    val fileName: String,
    val startTime: String,
    val manager: String,
    val exploit: String,
    val result: String,
    val durationMs: Long?,
    val kernel: String,
    val model: String
)

object HistoryStore {
    private val fileFmt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    private val displayFmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)
    private val metaKeys = setOf("MANAGER", "EXPLOIT", "KERNEL", "MODEL", "START", "RESULT", "EXIT", "DURATION")
    private val lock = Any()

    @Volatile
    private var current: File? = null

    private fun dir(context: Context): File =
        File(context.filesDir, "history").apply { mkdirs() }

    fun start(context: Context, manager: String, exploit: String, kernel: String, model: String): File {
        val f = File(dir(context), "${fileFmt.format(Date())}.log")
        synchronized(lock) {
            runCatching {
                f.writeText(
                    "MANAGER=$manager\nEXPLOIT=$exploit\nKERNEL=$kernel\nMODEL=$model\n" +
                        "START=${displayFmt.format(Date())}\n"
                )
            }
            current = f
        }
        return f
    }

    fun append(line: String) {
        synchronized(lock) {
            val f = current ?: return
            runCatching { f.appendText(line + "\n") }
        }
    }

    fun finish(success: Boolean, exit: Int, durationMs: Long) {
        synchronized(lock) {
            val f = current ?: return
            runCatching {
                f.appendText(
                    "RESULT=${if (success) "SUCCESS" else "FAIL"}\nEXIT=$exit\nDURATION=${durationMs}ms\n"
                )
            }
            current = null
        }
    }

    fun list(context: Context): List<HistoryEntry> {
        val files = dir(context).listFiles { f -> f.isFile && f.name.endsWith(".log") } ?: return emptyList()
        return files.sortedByDescending { it.name }.mapNotNull { parse(it) }
    }

    fun read(context: Context, fileName: String): String =
        runCatching { File(dir(context), fileName).readText() }.getOrDefault("")

    fun delete(context: Context, fileName: String): Boolean =
        File(dir(context), fileName).delete()

    private fun parse(f: File): HistoryEntry? {
        val text = runCatching { f.readText() }.getOrNull() ?: return null
        val meta = mutableMapOf<String, String>()
        for (line in text.lineSequence()) {
            val i = line.indexOf('=')
            if (i <= 0) continue
            val key = line.substring(0, i)
            if (key in metaKeys) meta[key] = line.substring(i + 1)
        }
        val ts = f.name.removeSuffix(".log")
        val start = runCatching { displayFmt.format(fileFmt.parse(ts)!!) }.getOrDefault(ts)
        return HistoryEntry(
            fileName = f.name,
            startTime = meta["START"] ?: start,
            manager = meta["MANAGER"] ?: "?",
            exploit = meta["EXPLOIT"] ?: "?",
            result = meta["RESULT"] ?: "RUNNING",
            durationMs = meta["DURATION"]?.removeSuffix("ms")?.toLongOrNull(),
            kernel = meta["KERNEL"] ?: "",
            model = meta["MODEL"] ?: ""
        )
    }
}
