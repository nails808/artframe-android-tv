package cc.wallhaven.artframe

data class Wallpaper(
    val id: String,
    val url: String,
    val path: String,
    val width: Int,
    val height: Int,
    val ratio: Double,
    val category: String,
    val purity: String,
    val fileSize: Long
)

data class SearchPage(
    val wallpapers: List<Wallpaper>,
    val total: Int,
    val currentPage: Int
)
