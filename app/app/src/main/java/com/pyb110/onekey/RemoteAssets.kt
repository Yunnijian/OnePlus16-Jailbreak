package com.pyb110.onekey

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Resolves the deployable toolchain from a signed manifest: exploit binaries stay in
 * the APK, while ko/ksud/syms/scripts are downloaded and hash-verified.
 */
class RemoteAssets(private val ctx: Context, private val log: (String) -> Unit) {

    data class Entry(val name: String, val url: String, val sha256: String, val size: Long)
    data class Manager(val label: String, val version: String, val files: List<Entry>)
    data class Manifest(
        val kernel: String,
        val generated: String,
        val shared: List<Entry>,
        val managers: Map<String, Manager>
    )

    private val cacheDir: File get() = File(ctx.filesDir, "remote").apply { mkdirs() }
    private val manifestFile: File get() = File(cacheDir, "manifest.json")
    private val manifestSigFile: File get() = File(cacheDir, "manifest.sig")

    fun resolve(base: String, managerKey: String, kernelRelease: String): List<Pair<String, File>> {
        val manifest = loadManifest(base)
        if (!kernelRelease.contains(manifest.kernel)) {
            throw IllegalStateException("清单适配内核 ${manifest.kernel}，本机是 $kernelRelease")
        }
        val manager = manifest.managers[managerKey]
            ?: throw IllegalStateException("清单里没有管理器 $managerKey")
        log("素材清单 ${manifest.generated}：${manager.label} ${manager.version}")

        val wanted = manifest.shared + manager.files
        val resolved = ArrayList<Pair<String, File>>(wanted.size)
        for (entry in wanted) {
            resolved += entry.name to obtain(entry, manifest.kernel)
        }
        prune(resolved.map { it.second.name }.toSet())
        return resolved
    }

    private fun loadManifest(base: String): Manifest {
        return try {
            val json = http(base + "/manifest.json", MAX_MANIFEST)
            val sig = http(base + "/manifest.sig", MAX_MANIFEST)
            if (!verify(json, sig)) throw IllegalStateException("manifest 签名无效")
            manifestFile.writeBytes(json)
            manifestSigFile.writeBytes(sig)
            parse(json)
        } catch (t: Throwable) {
            log("拉取素材清单失败（${t.message}），尝试本地缓存")
            if (!manifestFile.exists() || !manifestSigFile.exists()) throw t
            val json = manifestFile.readBytes()
            if (!verify(json, manifestSigFile.readBytes())) throw IllegalStateException("缓存清单签名无效")
            parse(json)
        }
    }

    private fun parse(json: ByteArray): Manifest {
        val root = JSONObject(String(json, Charsets.UTF_8))
        if (root.optInt("schema") != 1) throw IllegalStateException("不支持的清单版本")
        val shared = root.getJSONArray("shared").let { arr ->
            (0 until arr.length()).map { parseEntry(arr.getJSONObject(it)) }
        }
        val managersJson = root.getJSONObject("managers")
        val managers = managersJson.keys().asSequence().associateWith { key ->
            val o = managersJson.getJSONObject(key)
            val files = o.getJSONArray("files").let { arr ->
                (0 until arr.length()).map { parseEntry(arr.getJSONObject(it)) }
            }
            Manager(o.getString("label"), o.getString("version"), files)
        }
        return Manifest(root.getString("kernel"), root.getString("generated"), shared, managers)
    }

    private fun parseEntry(o: JSONObject) = Entry(
        name = o.getString("name"),
        url = o.getString("url"),
        sha256 = o.getString("sha256").lowercase(),
        size = o.getLong("size")
    )

    private fun obtain(entry: Entry, kernel: String): File {
        val target = File(cacheDir, entry.sha256)
        if (target.isFile && target.length() == entry.size && sha256(target) == entry.sha256) {
            log("${entry.name}: 命中缓存")
            return target
        }
        log("下载 ${entry.name} (${entry.size / 1024} KB) …")
        val tmp = File(cacheDir, "${entry.sha256}.part")
        try {
            download(entry.url, tmp)
            if (tmp.length() != entry.size) throw IllegalStateException("大小不符")
            val digest = sha256(tmp)
            if (digest != entry.sha256) throw IllegalStateException("sha256 不符: $digest")
            if (entry.name.endsWith(".ko") && !containsVermagic(tmp, kernel)) {
                throw IllegalStateException("ko vermagic 与内核 $kernel 不匹配")
            }
            if (!tmp.renameTo(target)) throw IllegalStateException("缓存写入失败")
            return target
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    private fun containsVermagic(ko: File, kernel: String): Boolean {
        val needle = "vermagic=$kernel".toByteArray(Charsets.ISO_8859_1)
        val hay = ko.readBytes()
        outer@ for (i in 0..hay.size - needle.size) {
            for (j in needle.indices) if (hay[i + j] != needle[j]) continue@outer
            return true
        }
        return false
    }

    private fun verify(json: ByteArray, signature: ByteArray): Boolean = runCatching {
        val key = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(Base64.decode(PUBLIC_KEY, Base64.NO_WRAP)))
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(key)
            update(json)
            verify(signature)
        }
    }.getOrDefault(false)

    private fun download(url: String, out: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "PYB110-OneClick")
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
        } finally {
            conn.disconnect()
        }
    }

    private fun http(url: String, limit: Int): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "PYB110-OneClick")
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            conn.inputStream.use { input ->
                val buf = java.io.ByteArrayOutputStream()
                val tmp = ByteArray(8192)
                var total = 0
                while (true) {
                    val n = input.read(tmp)
                    if (n < 0) break
                    total += n
                    if (total > limit) throw IllegalStateException("响应过大")
                    buf.write(tmp, 0, n)
                }
                return buf.toByteArray()
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun prune(keep: Set<String>) {
        cacheDir.listFiles()?.forEach { f ->
            val name = f.name
            if (name.startsWith("manifest")) return@forEach
            if (name.endsWith(".part") || name !in keep) f.delete()
        }
    }

    companion object {
        const val DEFAULT_BASE = "https://raw.githubusercontent.com/Yunnijian/OnePlus16-Jailbreak/refs/heads/main/assets"
        private const val MAX_MANIFEST = 1 shl 20
        private const val PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEJaqp8eG6jpfneQwKq4LttEOuaLsbFl1OY3i6D913TFaVOBnKEACIkEHEsWlmaZuHOesrfYHr+qAyvpjlMRL/Ww=="
    }
}
