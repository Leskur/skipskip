package com.skipskip.app

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * 向 GitHub Releases 询问有没有比当前 [BuildConfig.VERSION_NAME] 更新的版本。
 * 只在用户点击时调用；包含预发布 tag（0.0.x 会标成 prerelease）。
 */
object UpdateChecker {

    const val RELEASES_PAGE = "https://github.com/Leskur/skipskip/releases"
    private const val RELEASES_API =
        "https://api.github.com/repos/Leskur/skipskip/releases?per_page=5"

    sealed class Result {
        data class Latest(val current: String) : Result()
        data class Available(
            val current: String,
            val latest: String,
            val pageUrl: String,
        ) : Result()
        data object Failed : Result()
    }

    fun check(currentVersion: String = BuildConfig.VERSION_NAME): Result {
        return runCatching {
            val body = httpGet(RELEASES_API)
            val latest = newestRelease(JSONArray(body)) ?: return Result.Failed
            when (compareVersions(latest.tag, currentVersion)) {
                1 -> Result.Available(currentVersion, latest.tag, latest.url)
                else -> Result.Latest(currentVersion)
            }
        }.getOrElse { Result.Failed }
    }

    internal fun newestRelease(array: JSONArray): ReleaseRef? {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optBoolean("draft", false)) continue
            val tag = obj.optString("tag_name").trim()
            if (tag.isEmpty()) continue
            val url = obj.optString("html_url").ifBlank { RELEASES_PAGE }
            return ReleaseRef(tag = stripV(tag), url = url)
        }
        return null
    }

    /** 返回 1 表示 [remote] 更新，0 相同，-1 表示本地更新。 */
    internal fun compareVersions(remote: String, local: String): Int {
        val a = versionParts(remote)
        val b = versionParts(local)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val left = a.getOrElse(i) { 0 }
            val right = b.getOrElse(i) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    private fun versionParts(raw: String): List<Int> =
        stripV(raw).split('.', '-', '_')
            .mapNotNull { token -> token.takeWhile { it.isDigit() }.toIntOrNull() }
            .ifEmpty { listOf(0) }

    private fun stripV(tag: String): String =
        tag.trim().removePrefix("v").removePrefix("V")

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "SkipSkip/${BuildConfig.VERSION_NAME}")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("HTTP $code")
            return text
        } finally {
            connection.disconnect()
        }
    }

    internal data class ReleaseRef(val tag: String, val url: String)
}
