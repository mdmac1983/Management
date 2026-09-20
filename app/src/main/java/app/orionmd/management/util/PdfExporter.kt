package app.orionmd.management.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import app.orionmd.management.R

/**
 * Generates a simple, single-column PDF report (used by Schedule, Revenue, and Analytics'
 * "Save as PDF" buttons) with the app's brand image as a large, translucent, centered watermark
 * on every page. Uses the platform's built-in PdfDocument - no extra dependency needed.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 612 // US Letter at 72dpi
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 40f

    /** One row of the report body; each entry's [lines] are kept together on one page when possible. */
    data class Entry(val lines: List<String>)

    fun export(
        context: Context,
        uri: Uri,
        title: String,
        subtitle: String,
        summaryLines: List<String> = emptyList(),
        sectionLabel: String? = null,
        entries: List<Entry>,
        emptyMessage: String
    ): Result<Unit> {
        return try {
            val document = PdfDocument()
            val watermark = loadWatermark(context)
            val contentWidth = PAGE_WIDTH - MARGIN * 2

            val titlePaint = TextPaint().apply { isAntiAlias = true; textSize = 20f; color = 0xFF201C18.toInt(); isFakeBoldText = true }
            val subtitlePaint = TextPaint().apply { isAntiAlias = true; textSize = 13f; color = 0xFF5A5550.toInt() }
            val summaryPaint = TextPaint().apply { isAntiAlias = true; textSize = 13f; color = 0xFF201C18.toInt(); isFakeBoldText = true }
            val sectionPaint = TextPaint().apply { isAntiAlias = true; textSize = 14f; color = 0xFF201C18.toInt(); isFakeBoldText = true }
            val bodyPaint = TextPaint().apply { isAntiAlias = true; textSize = 11f; color = 0xFF2A2622.toInt() }
            val emptyPaint = TextPaint().apply { isAntiAlias = true; textSize = 12f; color = 0xFF8A8580.toInt() }
            val rulePaint = Paint().apply { color = 0xFFE0DCD3.toInt(); strokeWidth = 0.75f }

            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            var canvas = page.canvas
            drawWatermark(canvas, watermark)

            var y = MARGIN + 10f
            canvas.drawText(title, MARGIN, y, titlePaint)
            y += 20f
            canvas.drawText(subtitle, MARGIN, y, subtitlePaint)
            y += 22f

            summaryLines.forEach { line ->
                canvas.drawText(line, MARGIN, y, summaryPaint)
                y += 18f
            }
            if (summaryLines.isNotEmpty()) y += 8f

            if (sectionLabel != null) {
                canvas.drawText(sectionLabel, MARGIN, y, sectionPaint)
                y += 8f
                canvas.drawLine(MARGIN, y, MARGIN + contentWidth, y, rulePaint)
                y += 16f
            }

            fun newPage() {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawWatermark(canvas, watermark)
                y = MARGIN + 10f
            }

            if (entries.isEmpty()) {
                canvas.drawText(emptyMessage, MARGIN, y, emptyPaint)
            } else {
                entries.forEach { entry ->
                    val text = entry.lines.joinToString("\n")
                    val layout = StaticLayout.Builder
                        .obtain(text, 0, text.length, bodyPaint, contentWidth.toInt())
                        .setLineSpacing(2f, 1f)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .build()
                    val blockHeight = layout.height.toFloat()

                    if (y + blockHeight > PAGE_HEIGHT - MARGIN) {
                        newPage()
                    }

                    canvas.save()
                    canvas.translate(MARGIN, y)
                    layout.draw(canvas)
                    canvas.restore()
                    y += blockHeight + 6f
                    canvas.drawLine(MARGIN, y, MARGIN + contentWidth, y, rulePaint)
                    y += 10f
                }
            }

            document.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                document.writeTo(out)
            } ?: return Result.failure(IllegalStateException("Couldn't open the selected file for writing"))
            document.close()
            watermark?.recycle()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun loadWatermark(context: Context): Bitmap? =
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.rentals_brand)
        } catch (t: Throwable) {
            null
        }

    /** Draws the brand image large, centered, and translucent behind the page content. */
    private fun drawWatermark(canvas: Canvas, watermark: Bitmap?) {
        watermark ?: return
        val targetWidth = PAGE_WIDTH * 0.82f
        val scale = targetWidth / watermark.width
        val targetHeight = watermark.height * scale
        val left = (PAGE_WIDTH - targetWidth) / 2f
        val top = (PAGE_HEIGHT - targetHeight) / 2f
        val dest = Rect(left.toInt(), top.toInt(), (left + targetWidth).toInt(), (top + targetHeight).toInt())
        val paint = Paint().apply { alpha = 32; isAntiAlias = true; isFilterBitmap = true }
        canvas.drawBitmap(watermark, null, dest, paint)
    }
}
