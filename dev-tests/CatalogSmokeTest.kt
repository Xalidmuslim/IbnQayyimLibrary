package com.ibnqayyim.library

fun main() {
    check(Books.all.size == 4) { "Four verified books must exist" }
    check(Books.all.map { it.id }.distinct().size == Books.all.size)
    check(Books.all.all { it.title.isNotBlank() && it.credit.isNotBlank() && it.sourceUrl.startsWith("https://islamhouse.com/") })
    check(Books.all.all { BookUrlPolicy.isAllowedDownloadUrl(it.pdfUrl) })
    check(!BookUrlPolicy.isAllowedDownloadUrl("http://d1.islamhouse.com/a.pdf"))
    check(!BookUrlPolicy.isAllowedDownloadUrl("https://d1.islamhouse.com.evil.test/file.pdf"))
    check(!BookUrlPolicy.isAllowedDownloadUrl("https://google.com/file.pdf"))
    check(ReaderLogic.clampPage(-2, 15) == 0)
    check(ReaderLogic.clampPage(99, 15) == 14)
    check(ReaderLogic.clampPage(4, 0) == 0)
    check(ReaderLogic.nextPage(14, 15) == 14)
    check(ReaderLogic.previousPage(0) == 0)
    check(ReaderLogic.readingProgress(0, 10) == 10)
    check(ReaderLogic.readingProgress(9, 10) == 100)
    println("PASS: 13 catalog/security/reader checks")
}
