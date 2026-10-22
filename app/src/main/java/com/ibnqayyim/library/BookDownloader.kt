package com.ibnqayyim.library

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object BookDownloader {
    fun targetFile(context: Context, book: Book): File = File(File(context.filesDir, "books"), "${book.id}.pdf")

    /** Downloads from official origin without proxy/redistribution. Atomic move prevents corrupt books. */
    suspend fun download(context: Context, book: Book): File = withContext(Dispatchers.IO) {
        require(BookUrlPolicy.isAllowedDownloadUrl(book.pdfUrl)) { "Недоверенный адрес PDF" }
        val target = targetFile(context, book)
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${book.id}.part")
        var connection: HttpURLConnection? = null
        try {
            val http = (URL(book.pdfUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "IbnQayyimLibrary/0.1 (Android)")
            }
            connection = http
            check(http.responseCode == HttpURLConnection.HTTP_OK) {
                "Ошибка сервера: ${http.responseCode}"
            }
            val headerSize = http.contentLengthLong
            check(headerSize <= 40L * 1024 * 1024 || headerSize == -1L) { "Файл слишком большой" }
            var total = 0L
            http.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        total += count
                        check(total <= 40L * 1024 * 1024) { "Файл слишком большой" }
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            check(temporary.length() > 100) { "Файл пуст или повреждён" }
            check(temporary.inputStream().use { input ->
                ByteArray(5).also { input.read(it) }.decodeToString() == "%PDF-"
            }) { "Сервер вернул не PDF" }
            check(temporary.renameTo(target)) { "Не удалось сохранить книгу" }
            target
        } catch (e: Exception) {
            temporary.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
    }
}
