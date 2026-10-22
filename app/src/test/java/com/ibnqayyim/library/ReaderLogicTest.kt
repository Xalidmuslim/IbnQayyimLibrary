package com.ibnqayyim.library

import org.junit.Assert.*
import org.junit.Test

class ReaderLogicTest {
    @Test fun `page limits enforced`() {
        assertEquals(0, ReaderLogic.clampPage(-1, 10))
        assertEquals(9, ReaderLogic.clampPage(999, 10))
        assertEquals(0, ReaderLogic.clampPage(5, 0))
    }
    @Test fun `navigation stays in book`() {
        assertEquals(9, ReaderLogic.nextPage(9, 10))
        assertEquals(0, ReaderLogic.previousPage(0))
    }
    @Test fun `progress calculated from human-visible pages`() {
        assertEquals(10, ReaderLogic.readingProgress(0, 10))
        assertEquals(100, ReaderLogic.readingProgress(9, 10))
    }
    @Test fun `only official pdf origin allowed`() {
        assertTrue(Books.all.all { BookUrlPolicy.isAllowedDownloadUrl(it.pdfUrl) })
        assertFalse(BookUrlPolicy.isAllowedDownloadUrl("https://d1.islamhouse.com.evil.com/x.pdf"))
        assertFalse(BookUrlPolicy.isAllowedDownloadUrl("http://d1.islamhouse.com/data/ru/x.pdf"))
    }
    @Test fun `catalog carries book credits`() {
        assertEquals(Books.all.size, Books.all.map { it.id }.distinct().size)
        assertTrue(Books.all.all { it.credit.contains("Источник: IslamHouse") })
    }
}
