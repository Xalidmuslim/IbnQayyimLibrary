package com.ibnqayyim.library

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

/** Uses Android's native PDF engine; does not modify the source document. */
object PdfPages {
    fun count(file: File): Int {
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            return renderer.pageCount
        }
    }

    fun render(file: File, requestedPage: Int): Bitmap {
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            require(renderer.pageCount > 0)
            renderer.openPage(ReaderLogic.clampPage(requestedPage, renderer.pageCount)).use { page ->
                val targetWidth = 1600
                val targetHeight = (page.height.toDouble() * targetWidth / page.width)
                    .toInt().coerceIn(1, 4000)
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        }
    }
}
