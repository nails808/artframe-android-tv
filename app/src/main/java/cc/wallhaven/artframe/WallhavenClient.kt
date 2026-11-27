package cc.wallhaven.artframe

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ln

class WallhavenClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun search(
        query: String,
        apiKey: String,
        includeSfw: Boolean,
        includeSketchy: Boolean,
        includeNsfw: Boolean,
        page: Int = 1,
        targetWidth: Int = 1920,
        targetHeight: Int = 1080
    ): SearchPage {
        if (query.isBlank()) throw IllegalArgumentException("Enter a search term.")
        if (!includeSfw && !includeSketchy && !includeNsfw) {
            throw IllegalArgumentException("Select at least one content category.")
        }

        val purity = buildString {
            append(if (includeSfw) "1" else "0")
            append(if (includeSketchy) "1" else "0")
            append(if (includeNsfw) "1" else "0")
        }
        val url = "https://wallhaven.cc/api/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query.trim())
            .addQueryParameter("categories", "111")
            .addQueryParameter("purity", purity)
            .addQueryParameter("ratios", "16x9")
            .addQueryParameter("atleast", "${targetWidth}x${targetHeight}")
            .addQueryParameter("sorting", "relevance")
            .addQueryParameter("order", "desc")
            .addQueryParameter("page", page.toString())
            .build()

        val requestBuilder = Request.Builder().url(url).get()
        if (apiKey.isNotBlank()) requestBuilder.header("X-API-Key", apiKey.trim())

        val response = http.newCall(requestBuilder.build()).execute()
        response.use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                val message = runCatching { JSONObject(body).optString("error") }.getOrNull()
                throw IOException(message?.takeIf { it.isNotBlank() } ?: "Wallhaven returned HTTP ${res.code}.")
            }
            val root = JSONObject(body)
            val data = root.optJSONArray("data") ?: return SearchPage(emptyList(), 0, page)
            val items = buildList {
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val dimension = item.optString("resolution").split("x")
                    val width = dimension.getOrNull(0)?.toIntOrNull() ?: continue
                    val height = dimension.getOrNull(1)?.toIntOrNull() ?: continue
                    val ratio = item.optString("ratio").toDoubleOrNull() ?: width.toDouble() / height
                    // Keep images close to 16:9 and large enough for a useful TV presentation.
                    val ratioError = abs(ratio - (16.0 / 9.0))
                    if (ratioError > 0.025 || width < 1920 || height < 1080) continue
                    val path = item.optString("path")
                    if (path.isBlank()) continue
                    add(
                        Wallpaper(
                            id = item.optString("id"),
                            url = item.optString("url"),
                            path = path,
                            width = width,
                            height = height,
                            ratio = ratio,
                            category = item.optString("category"),
                            purity = item.optString("purity"),
                            fileSize = item.optLong("file_size")
                        )
                    )
                }
            }.sortedBy { wallpaper ->
                abs(wallpaper.ratio - 16.0 / 9.0) * 100.0 +
                    abs(ln(wallpaper.width.toDouble() / targetWidth)) +
                    abs(ln(wallpaper.height.toDouble() / targetHeight))
            }
            if (items.isEmpty() && (targetWidth > 1920 || targetHeight > 1080)) {
                return search(query, apiKey, includeSfw, includeSketchy, includeNsfw, page, 1920, 1080)
            }
            val meta = root.optJSONObject("meta")
            return SearchPage(items, meta?.optInt("total", items.size) ?: items.size, page)
        }
    }
}
