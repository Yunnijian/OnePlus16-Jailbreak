package com.pyb110.onekey

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UpdateAsset(
    val name: String,
    val url: String,
    val size: Long,
)

data class UpdateInfo(
    val version: String,
    val changelog: String,
    val pageUrl: String,
    val apk: UpdateAsset,
    val sha256: String?,
    val source: String,
)

sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Failed : UpdateCheckResult
}

object UpdateChecker {
    private const val GITEE_API =
        "https://gitee.com/api/v5/repos/puci1/Shark/releases/latest"
    private const val GITHUB_API =
        "https://api.github.com/repos/Yunnijian/Shark/releases/latest"
    private const val USER_AGENT = "Shark-Updater"

    suspend fun check(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        val current = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            ?: return@withContext UpdateCheckResult.Failed
        val currentParts = parseVersion(current)
        var fetched = false

        val gitee = runCatching { fetch(GITEE_API, "Gitee", true) }.getOrNull()
        if (gitee != null) {
            fetched = true
            if (compareVersion(parseVersion(gitee.version), currentParts) > 0) {
                return@withContext UpdateCheckResult.Available(gitee)
            }
        }

        val github = runCatching { fetch(GITHUB_API, "GitHub", false) }.getOrNull()
        if (github != null) {
            fetched = true
            if (compareVersion(parseVersion(github.version), currentParts) > 0) {
                return@withContext UpdateCheckResult.Available(github)
            }
        }
        if (fetched) UpdateCheckResult.UpToDate else UpdateCheckResult.Failed
    }

    private fun fetch(api: String, source: String, gitee: Boolean): UpdateInfo? {
        val conn = (URL(api).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 10000
            setRequestProperty("User-Agent", USER_AGENT)
            if (!gitee) setRequestProperty("Accept", "application/vnd.github+json")
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        if (json.optBoolean("prerelease", false)) return null

        val tag = json.optString("tag_name").ifBlank { json.optString("name") }
        val version = tag.removePrefix("v").removePrefix("V")
        if (version.isBlank()) return null

        var apk: UpdateAsset? = null
        var shaAsset: UpdateAsset? = null
        val assets = json.optJSONArray("assets") ?: json.optJSONArray("attach_files")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val item = assets.optJSONObject(i) ?: continue
                val name = item.optString("name").ifBlank { item.optString("file_name") }
                val url = item.optString("browser_download_url")
                    .ifBlank { item.optString("download_url") }
                if (url.isBlank()) continue
                val asset = UpdateAsset(name, url, item.optLong("size", 0L))
                if (apk == null && name.endsWith(".apk", ignoreCase = true)) apk = asset
                if (shaAsset == null &&
                    (name.endsWith(".sha256", ignoreCase = true) ||
                        name.endsWith(".sha256.txt", ignoreCase = true))
                ) {
                    shaAsset = asset
                }
            }
        }
        val apkAsset = apk ?: return null
        val releaseBody = json.optString("body")
        val expectedSha = shaAsset
            ?.let { runCatching { downloadText(it.url) }.getOrNull() }
            ?.let(::extractSha256)
            ?: extractSha256(releaseBody)
        val pageUrl = json.optString("html_url").ifBlank {
            if (gitee) "https://gitee.com/puci1/Shark/releases"
            else "https://github.com/Yunnijian/Shark/releases/tag/$tag"
        }
        return UpdateInfo(
            version = version,
            changelog = releaseBody,
            pageUrl = pageUrl,
            apk = apkAsset,
            sha256 = expectedSha,
            source = source,
        )
    }

    private fun downloadText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 10000
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "update").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val safeName = info.apk.name
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .ifBlank { "Shark-${info.version}.apk" }
        val target = File(dir, safeName)

        val conn = (URL(info.apk.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 30000
            setRequestProperty("User-Agent", USER_AGENT)
        }
        val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.apk.size
        val digest = MessageDigest.getInstance("SHA-256")
        var lastPercent = -1
        conn.inputStream.use { input ->
            target.outputStream().use { output ->
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
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        val expected = info.sha256
        if (!expected.isNullOrBlank() && !actual.equals(expected, ignoreCase = true)) {
            target.delete()
            throw IllegalStateException("SHA-256 校验失败")
        }
        target
    }

    fun verifySignature(context: Context, apk: File): Boolean {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(
            apk.absolutePath,
            PackageManager.GET_SIGNING_CERTIFICATES
        ) ?: return false
        val incoming = archive.signingInfo?.apkContentsSigners ?: return false
        val current = pm.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        ).signingInfo?.apkContentsSigners ?: return false
        fun hashes(signatures: Array<android.content.pm.Signature>): Set<String> =
            signatures.map {
                MessageDigest.getInstance("SHA-256")
                    .digest(it.toByteArray())
                    .joinToString("") { b -> "%02x".format(b) }
            }.toSet()
        return hashes(incoming) == hashes(current)
    }

    private fun extractSha256(text: String): String? =
        Regex("(?i)sha-?256\\s*[:：=]?\\s*([0-9a-f]{64})")
            .find(text)
            ?.groupValues
            ?.getOrNull(1)

    private fun parseVersion(version: String): List<Int> =
        Regex("\\d+").findAll(version).map { it.value.toInt() }.take(4).toList()

    private fun compareVersion(a: List<Int>, b: List<Int>): Int {
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val left = a.getOrElse(i) { 0 }
            val right = b.getOrElse(i) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }
}
