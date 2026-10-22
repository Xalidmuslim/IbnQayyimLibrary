package com.ibnqayyim.library

/** Source URLs are official IslamHouse pages, and PDF URLs point to their hosted originals. */
data class Book(
    val id: String,
    val title: String,
    val description: String,
    val credit: String,
    val category: String,
    val pages: Int,
    val sourceUrl: String,
    val pdfUrl: String,
)

object Books {
    val all: List<Book> = listOf(
        Book(
            id = "rain",
            title = "Благодатный дождь прекрасных слов",
            description = "О поминании Аллаха и его достоинствах",
            credit = "Перевод: И. Ф. Сарбулатов · Источник: IslamHouse",
            category = "Поминание Аллаха",
            pages = 126,
            sourceUrl = "https://islamhouse.com/ru/books/185792/",
            pdfUrl = "https://d1.islamhouse.com/data/ru/ih_books/single/ru_these_words_cross_barrage_of_good.pdf"
        ),
        Book(
            id = "prayer",
            title = "Тайны молитвы",
            description = "О духовном наслаждении молитвой и Кораном",
            credit = "Перевод: Вильдан Аетдинов · Источник: IslamHouse",
            category = "Молитва",
            pages = 39,
            sourceUrl = "https://islamhouse.com/ru/books/722469",
            pdfUrl = "https://d1.islamhouse.com/data/ru/ih_books/single/ru_Asrar_Assalah.pdf"
        ),
        Book(
            id = "complaint",
            title = "Жалоба невежды",
            description = "Краткое наставление из серии «Пользы от Ибн Кайима»",
            credit = "Перевод: Абу Абдуррахман ад-Дагестани · Источник: IslamHouse",
            category = "Наставления",
            pages = 4,
            sourceUrl = "https://islamhouse.com/ru/books/280422/",
            pdfUrl = "https://d1.islamhouse.com/data/ru/ih_articles/single/ru_jaloba_nevezhdi.pdf"
        ),
        Book(
            id = "mercies",
            title = "Виды милостей",
            description = "Краткое наставление о милостях",
            credit = "Перевод: Абу Абдуррахман ад-Дагестани · Источник: IslamHouse",
            category = "Наставления",
            pages = 3,
            sourceUrl = "https://islamhouse.com/ru/books/280420/",
            pdfUrl = "https://d1.islamhouse.com/data/ru/ih_articles/single/ru_vidi_milostey.pdf"
        ),
    )
}

/** Never automatically download arbitrary URLs, including redirect targets. */
object BookUrlPolicy {
    fun isAllowedDownloadUrl(url: String): Boolean = try {
        val parsed = java.net.URI(url)
        parsed.scheme == "https" &&
            parsed.host == "d1.islamhouse.com" &&
            parsed.rawUserInfo == null &&
            parsed.port == -1 &&
            parsed.path.startsWith("/data/ru/") &&
            parsed.path.endsWith(".pdf", ignoreCase = true)
    } catch (_: Exception) { false }
}

object ReaderLogic {
    fun clampPage(page: Int, pageCount: Int) = if (pageCount <= 0) 0 else page.coerceIn(0, pageCount - 1)
    fun nextPage(page: Int, pageCount: Int) = clampPage(page + 1, pageCount)
    fun previousPage(page: Int) = (page - 1).coerceAtLeast(0)
    fun readingProgress(page: Int, pageCount: Int) =
        if (pageCount <= 0) 0 else (((clampPage(page, pageCount) + 1) * 100.0) / pageCount).toInt()
}
