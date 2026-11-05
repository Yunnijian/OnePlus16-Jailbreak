package com.pyb110.onekey

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class DroidspacesRelease(
    val version: String,
    val changelog: String,
    val pageUrl: String,
    val assetName: String,
    val assetUrl: String,
    val assetSize: Long,
    val sha256: String?,
)

object DroidspacesModule {
    private const val API =
        "https://api.github.com/repos/Yunnijian/Droidspaces-LKM/releases/latest"
    private const val USER_AGENT = "Shark-Droidspaces"
    private const val DOWNLOAD_DIR = "Shark"

    suspend fun latest(): DroidspacesRelease = withContext(Dispatchers.IO) {
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 15000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val version = json.optString("tag_name").ifBlank { json.optString("name") }
        if (version.isBlank()) throw IllegalStateException("release 没有版本号")

        var zip: JSONObject? = null
        var shaAsset: JSONObject? = null
        val assets = json.optJSONArray("assets")
            ?: throw IllegalStateException("release 没有附件")
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            if (url.isBlank()) continue
            if (name.endsWith(".zip", ignoreCase = true)) zip = asset
            if (name.endsWith(".sha256", ignoreCase = true)) shaAsset = asset
        }
        val apk = zip ?: throw IllegalStateException("release 中没有模块 zip")
        val sha = shaAsset
            ?.let { runCatching { downloadText(it.optString("browser_download_url")) }.getOrNull() }
            ?.let(::extractSha256)
        DroidspacesRelease(
            version = version,
            changelog = json.optString("body"),
            pageUrl = json.optString("html_url").ifBlank {
                "https://github.com/Yunnijian/Droidspaces-LKM/releases"
            },
            assetName = apk.optString("name"),
            assetUrl = apk.optString("browser_download_url"),
            assetSize = apk.optLong("size", 0L),
            sha256 = sha,
        )
    }

    suspend fun download(
        context: Context,
        release: DroidspacesRelease,
        onProgress: (Float) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val safeName = release.assetName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .ifBlank { "droidspaces_lkm-${release.version}.zip" }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/" + DOWNLOAD_DIR
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("无法在 Download 目录创建文件")

        try {
            val conn = (URL(release.assetUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 30000
                setRequestProperty("User-Agent", USER_AGENT)
            }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.assetSize
            val digest = MessageDigest.getInstance("SHA-256")
            var lastPercent = -1
            conn.inputStream.use { input ->
                resolver.openOutputStream(uri)?.use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    while (true) {
                        read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val progress = (done.toFloat() / total).coerceIn(0f, 1f)
                            val percent = (progress * 100).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(progress)
                            }
                        }
                    }
                } ?: throw IllegalStateException("无法写入 Download 文件")
            }

            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!release.sha256.isNullOrBlank() &&
                !actual.equals(release.sha256, ignoreCase = true)
            ) {
                throw IllegalStateException("SHA-256 校验失败")
            }

            val doneValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, doneValues, null, null)
            queryPath(context, uri, safeName)
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
    }

    private fun queryPath(context: Context, uri: android.net.Uri, fallback: String): String {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0) ?: fallback
                val relative = cursor.getString(1)
                    ?: (Environment.DIRECTORY_DOWNLOADS + "/" + DOWNLOAD_DIR + "/")
                return File(
                    Environment.getExternalStorageDirectory(),
                    relative + name
                ).absolutePath
            }
        }
        return File(
            Environment.getExternalStorageDirectory(),
            Environment.DIRECTORY_DOWNLOADS + "/" + DOWNLOAD_DIR + "/" + fallback
        ).absolutePath
    }

    private fun downloadText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 15000
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun extractSha256(text: String): String? =
        Regex("(?i)sha-?256\\s*[:：=]?\\s*([0-9a-f]{64})")
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
}
