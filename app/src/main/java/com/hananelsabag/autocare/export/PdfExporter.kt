package com.hananelsabag.autocare.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.data.local.entities.RecordType
import com.hananelsabag.autocare.data.local.entities.TestRecord
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import com.hananelsabag.autocare.domain.repository.TestRecordRepository
import com.hananelsabag.autocare.util.daysFromNow
import com.hananelsabag.autocare.util.toFormattedDate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ── Export result ─────────────────────────────────────────────────────────────

sealed class PdfExportResult {
    /** API 29+: saved to MediaStore Downloads. Open with ACTION_VIEW. */
    data class SavedToDownloads(val uri: Uri, val fileName: String) : PdfExportResult()
    /** API < 29: share via chooser (FileProvider fallback). */
    data class Share(val intent: Intent) : PdfExportResult()
}

// ── Page constants ────────────────────────────────────────────────────────────

private const val PAGE_WIDTH    = 595
private const val PAGE_HEIGHT   = 842
private const val MARGIN        = 40f
private const val CONTENT_RIGHT = PAGE_WIDTH - MARGIN
private const val CONTENT_LEFT  = MARGIN
private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

// Table column x-positions
private const val COL_DATE_X = CONTENT_RIGHT - 4f   // RIGHT-aligned
private const val COL_DESC_X = CONTENT_RIGHT - 90f  // RIGHT-aligned
private const val COL_KM_X   = CONTENT_LEFT + 175f  // RIGHT-aligned
private const val COL_COST_X = CONTENT_LEFT + 4f    // LEFT-aligned

// ── Brand colors (AutoCare blue) ──────────────────────────────────────────────

private val COLOR_PRIMARY        = Color.parseColor("#0057A8")
private val COLOR_PRIMARY_DARK   = Color.parseColor("#003D7A")
private val COLOR_PRIMARY_LIGHT  = Color.parseColor("#D5E3FF")
private val COLOR_ON_PRIMARY     = Color.WHITE

private val COLOR_GREEN          = Color.parseColor("#2E7D32")
private val COLOR_GREEN_BG       = Color.parseColor("#E8F5E9")
private val COLOR_YELLOW         = Color.parseColor("#F57F17")
private val COLOR_YELLOW_BG      = Color.parseColor("#FFF8E1")
private val COLOR_RED            = Color.parseColor("#C62828")
private val COLOR_RED_BG         = Color.parseColor("#FFEBEE")

private val COLOR_SURFACE_VAR    = Color.parseColor("#EEF3FA")
private val COLOR_TEXT_PRIMARY   = Color.parseColor("#1C1B1F")
private val COLOR_TEXT_SECONDARY = Color.parseColor("#49454F")
private val COLOR_DIVIDER        = Color.parseColor("#C4C8D0")
private val COLOR_CARD           = Color.parseColor("#EDF3FF")

private val COLOR_STRIP_DIVIDER  = Color.parseColor("#1E6FBF")
private val COLOR_STRIP_LABEL    = Color.parseColor("#B3D4FF")
private val COLOR_STRIP_UNIT     = Color.parseColor("#8AB4F8")

// ─────────────────────────────────────────────────────────────────────────────

class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carRepository: CarRepository,
    private val maintenanceRepository: MaintenanceRecordRepository,
    private val testRecordRepository: TestRecordRepository,
) {
    /**
     * Each call creates a fresh [ExportJob] with its own [PdfDocument].
     * No class-level mutable state — re-entrant calls are safe and a failed
     * export never poisons the next one.
     */
    suspend fun exportCar(carId: Int): PdfExportResult = ExportJob().run(carId)

    // ── Inner job ─────────────────────────────────────────────────────────────

    private inner class ExportJob {

        private val document = PdfDocument()
        private lateinit var canvas: Canvas
        private lateinit var page: PdfDocument.Page
        private var pageNum = 0
        private var y = 0f

        // String helpers — uses the current app locale automatically
        private fun str(@androidx.annotation.StringRes id: Int) = context.getString(id)
        private fun str(@androidx.annotation.StringRes id: Int, vararg args: Any) = context.getString(id, *args)

        // ── Paints ────────────────────────────────────────────────────────────

        private val paintFill = Paint().apply { style = Paint.Style.FILL }

        private val paintCarTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY; textSize = 20f
            isFakeBoldText = true; textAlign = Paint.Align.RIGHT
        }
        private val paintSectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ON_PRIMARY; textSize = 12f
            isFakeBoldText = true; textAlign = Paint.Align.RIGHT
        }
        private val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY; textSize = 11f; textAlign = Paint.Align.RIGHT
        }
        private val paintBodySecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY; textSize = 10f; textAlign = Paint.Align.RIGHT
        }
        private val paintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY; textSize = 9f; textAlign = Paint.Align.RIGHT
        }
        private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY; textSize = 9f
            isFakeBoldText = true; textAlign = Paint.Align.RIGHT
        }
        private val paintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY; textSize = 11f
            isFakeBoldText = true; textAlign = Paint.Align.RIGHT
        }
        private val paintStatusBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f; isFakeBoldText = true; textAlign = Paint.Align.LEFT
        }
        private val paintPageNum = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY; textSize = 8f; textAlign = Paint.Align.CENTER
        }

        // ── Entry point ───────────────────────────────────────────────────────

        suspend fun run(carId: Int): PdfExportResult {
            return try {
                val car         = carRepository.getCarById(carId).first() ?: error("Car not found: $carId")
                val allRecords  = maintenanceRepository.getRecordsForCar(carId).first()
                val testRecords = testRecordRepository.getByCarId(carId).first()

                startNewPage()
                drawCoverHeader()
                drawCarCard(car)
                advanceY(14f)
                drawStatusSection(car.testExpiryDate, car.insuranceExpiryDate)
                advanceY(14f)
                drawStatsStrip(allRecords, testRecords)
                advanceY(20f)

                val byType = allRecords.groupBy { it.type }
                for (type in listOf(RecordType.MAINTENANCE, RecordType.REPAIR, RecordType.WEAR)) {
                    val records = byType[type]?.takeIf { it.isNotEmpty() } ?: continue
                    ensureSpaceOrNewPage(80f)
                    drawRecordsSection(type, records)
                    advanceY(16f)
                }

                if (testRecords.isNotEmpty()) {
                    ensureSpaceOrNewPage(80f)
                    drawTestHistorySection(testRecords)
                    advanceY(16f)
                }

                val withReceipts = allRecords.filter { it.receiptUri != null }
                if (withReceipts.isNotEmpty()) {
                    startNewPage()
                    drawSectionBanner(str(R.string.pdf_section_receipts))
                    advanceY(16f)
                    for (record in withReceipts) {
                        val bmp = loadBitmap(record.receiptUri!!) ?: continue
                        ensureSpaceOrNewPage(230f)
                        drawReceiptImage(bmp, record)
                        advanceY(14f)
                    }
                }

                finishDocumentWithFooters()

                val dateStr  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val fileName = "AutoCare_${car.make}_${car.model}_$dateStr.pdf".replace(" ", "_")
                saveDocument(fileName)

            } finally {
                runCatching { document.close() }
            }
        }

        // ── Save ──────────────────────────────────────────────────────────────

        private fun saveDocument(fileName: String): PdfExportResult {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = context.contentResolver.insert(collection, values)
                    ?: error("MediaStore insert failed")
                context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                PdfExportResult.SavedToDownloads(uri, fileName)
            } else {
                val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(exportsDir, fileName)
                FileOutputStream(file).use { document.writeTo(it) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                PdfExportResult.Share(intent)
            }
        }

        // ── Page management ───────────────────────────────────────────────────

        private fun startNewPage() {
            if (pageNum > 0) document.finishPage(page)
            pageNum++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page   = document.startPage(info)
            canvas = page.canvas
            y      = MARGIN
        }

        private fun finishDocumentWithFooters() {
            drawPageFooter()
            document.finishPage(page)
        }

        private fun advanceY(amount: Float) { y += amount }

        private fun ensureSpaceOrNewPage(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN - 20f) {
                drawPageFooter()
                startNewPage()
            }
        }

        private fun drawPageFooter() {
            val footerY = PAGE_HEIGHT - 18f
            paintFill.color = COLOR_DIVIDER
            canvas.drawRect(CONTENT_LEFT, footerY - 6f, CONTENT_LEFT + CONTENT_WIDTH, footerY - 5f, paintFill)
            canvas.drawText(str(R.string.pdf_footer), CONTENT_LEFT,  footerY, Paint(paintPageNum).apply { textAlign = Paint.Align.LEFT })
            canvas.drawText(str(R.string.pdf_page, pageNum), CONTENT_RIGHT, footerY, Paint(paintPageNum).apply { textAlign = Paint.Align.RIGHT })
        }

        // ── Cover header ──────────────────────────────────────────────────────

        private fun drawCoverHeader() {
            val headerH = 80f

            paintFill.color = COLOR_PRIMARY
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerH, paintFill)
            paintFill.color = COLOR_PRIMARY_DARK
            canvas.drawRect(0f, headerH - 4f, PAGE_WIDTH.toFloat(), headerH, paintFill)

            // Logo — white ring + circular clip
            val logoBmp  = loadAppLogo()
            val logoSize = 52f
            val logoX    = CONTENT_RIGHT - logoSize / 2f
            val logoY    = headerH / 2f
            if (logoBmp != null) {
                paintFill.color = Color.WHITE
                canvas.drawCircle(logoX, logoY, logoSize / 2f + 3f, paintFill)
                canvas.save()
                canvas.clipPath(Path().apply { addCircle(logoX, logoY, logoSize / 2f, Path.Direction.CW) })
                canvas.drawBitmap(
                    logoBmp, null,
                    RectF(logoX - logoSize / 2f, logoY - logoSize / 2f, logoX + logoSize / 2f, logoY + logoSize / 2f),
                    null
                )
                canvas.restore()
            }

            canvas.drawText(
                "AutoCare",
                PAGE_WIDTH / 2f - 30f, headerH / 2f + 10f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ON_PRIMARY; textSize = 26f; isFakeBoldText = true }
            )
            canvas.drawText(
                str(R.string.pdf_report_subtitle),
                PAGE_WIDTH / 2f - 30f, headerH / 2f + 26f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_STRIP_LABEL; textSize = 11f }
            )

            y = headerH + 16f
        }

        private fun loadAppLogo(): Bitmap? = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.autocare_logo)
        }.getOrNull()

        // ── Car details card ──────────────────────────────────────────────────

        private fun drawCarCard(car: com.hananelsabag.autocare.data.local.entities.Car) {
            val cardH   = 74f
            val cardTop = y

            paintFill.color = COLOR_CARD
            canvas.drawRoundRect(RectF(CONTENT_LEFT, cardTop, CONTENT_LEFT + CONTENT_WIDTH, cardTop + cardH), 10f, 10f, paintFill)
            paintFill.color = COLOR_PRIMARY
            canvas.drawRoundRect(RectF(CONTENT_LEFT, cardTop, CONTENT_LEFT + 4f, cardTop + cardH), 2f, 2f, paintFill)

            y = cardTop + 16f
            canvas.drawText("${car.make} ${car.model} · ${car.year}", CONTENT_RIGHT, y, paintCarTitle)
            advanceY(18f)

            // License plate badge
            val plateText  = "  ${car.licensePlate}  "
            val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_PRIMARY; textSize = 10f; isFakeBoldText = true; textAlign = Paint.Align.LEFT
            }
            paintFill.color = Color.WHITE
            val plateWidth = platePaint.measureText(plateText)
            canvas.drawRoundRect(RectF(CONTENT_LEFT + 8f, y - 11f, CONTENT_LEFT + 8f + plateWidth + 2f, y + 3f), 4f, 4f, paintFill)
            canvas.drawText(plateText, CONTENT_LEFT + 8f, y, platePaint)

            // Details (color / km) — right side
            val detailParts = buildList {
                car.color?.let    { add("${str(R.string.pdf_color_label)} $it") }
                car.currentKm?.let { add("${str(R.string.pdf_km_label)} ${formatNumber(it)}") }
            }
            if (detailParts.isNotEmpty()) {
                canvas.drawText(detailParts.joinToString("   ·   "), CONTENT_RIGHT - 8f, y, paintBodySecondary)
            }

            advanceY(20f)
            car.notes?.let { canvas.drawText("${str(R.string.pdf_notes_label)} ${it.take(70)}", CONTENT_RIGHT - 8f, y, paintSmall) }

            y = cardTop + cardH
        }

        // ── Status section ────────────────────────────────────────────────────

        private fun drawStatusSection(testExpiry: Long?, insuranceExpiry: Long?) {
            drawSectionBanner(str(R.string.pdf_section_status))
            advanceY(14f)

            val rowH = 32f
            drawStatusRow(str(R.string.pdf_status_test), testExpiry, y, rowH)
            advanceY(rowH + 8f)
            drawStatusRow(str(R.string.pdf_status_insurance), insuranceExpiry, y, rowH)
            advanceY(rowH)
        }

        private fun drawStatusRow(label: String, expiryMs: Long?, rowY: Float, rowH: Float) {
            paintFill.color = COLOR_SURFACE_VAR
            canvas.drawRoundRect(RectF(CONTENT_LEFT, rowY, CONTENT_LEFT + CONTENT_WIDTH, rowY + rowH), 8f, 8f, paintFill)

            val textY = rowY + rowH / 2f + 4f
            canvas.drawText(label, CONTENT_RIGHT - 8f, textY, paintBold)

            if (expiryMs == null) {
                paintStatusBadge.color = COLOR_TEXT_SECONDARY
                canvas.drawText(str(R.string.pdf_status_no_date), CONTENT_LEFT + 8f, textY, paintStatusBadge)
                return
            }

            val days    = expiryMs.daysFromNow()
            val dateStr = expiryMs.toFormattedDate()
            val (statusText, textColor, bgColor) = when {
                days < 0L  -> Triple(str(R.string.pdf_status_expired), COLOR_RED, COLOR_RED_BG)
                days == 0L -> Triple(str(R.string.pdf_status_expires_today), COLOR_RED, COLOR_RED_BG)
                days <= 7  -> Triple(str(R.string.pdf_status_days_short, days, dateStr), COLOR_RED, COLOR_RED_BG)
                days <= 30 -> Triple(str(R.string.pdf_status_days_short, days, dateStr), COLOR_YELLOW, COLOR_YELLOW_BG)
                else       -> Triple(str(R.string.pdf_status_valid_until, dateStr, days), COLOR_GREEN, COLOR_GREEN_BG)
            }

            val badgePaint = Paint(paintStatusBadge).apply { color = textColor }
            val badgeWidth = badgePaint.measureText("  $statusText  ")
            paintFill.color = bgColor
            canvas.drawRoundRect(RectF(CONTENT_LEFT + 4f, rowY + 6f, CONTENT_LEFT + 4f + badgeWidth, rowY + rowH - 6f), 6f, 6f, paintFill)
            canvas.drawText("  $statusText", CONTENT_LEFT + 4f, textY, badgePaint)
        }

        // ── Stats strip ───────────────────────────────────────────────────────

        private fun drawStatsStrip(records: List<MaintenanceRecord>, testRecords: List<TestRecord>) {
            val totalCost       = records.mapNotNull { it.costAmount }.sum()
            val totalRecords    = records.size
            val lastMaintenance = records.filter { it.type == RecordType.MAINTENANCE }.maxByOrNull { it.date }?.date
            val testsPassed     = testRecords.count { it.passed }

            val stripH = 56f
            val colW   = CONTENT_WIDTH / 4f

            paintFill.color = COLOR_PRIMARY
            canvas.drawRoundRect(RectF(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y + stripH), 10f, 10f, paintFill)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_STRIP_LABEL; textSize = 8f;  textAlign = Paint.Align.CENTER }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ON_PRIMARY;  textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            val unitPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_STRIP_UNIT;  textSize = 8f;  textAlign = Paint.Align.CENTER }

            paintFill.color = COLOR_STRIP_DIVIDER
            for (i in 1..3) {
                val divX = CONTENT_LEFT + colW * i
                canvas.drawRect(divX - 0.5f, y + 10f, divX + 0.5f, y + stripH - 10f, paintFill)
            }

            fun drawCol(cx: Float, value: String, unit: String, label: String, smallValue: Boolean = false) {
                canvas.drawText(value, cx, y + 24f, if (smallValue) Paint(valuePaint).apply { textSize = 11f } else valuePaint)
                canvas.drawText(unit,  cx, y + 36f, unitPaint)
                canvas.drawText(label, cx, y + 48f, labelPaint)
            }

            drawCol(CONTENT_LEFT + colW * 0.5f, "$totalRecords",
                str(R.string.pdf_stat_total_records), str(R.string.pdf_stat_total))
            drawCol(CONTENT_LEFT + colW * 1.5f,
                if (totalCost > 0) "₪${formatNumber(totalCost.toInt())}" else "—",
                str(R.string.pdf_stat_total_cost), str(R.string.pdf_stat_total))
            drawCol(CONTENT_LEFT + colW * 2.5f,
                lastMaintenance?.toFormattedDate() ?: "—",
                str(R.string.pdf_stat_last_service), "",
                smallValue = true)
            drawCol(CONTENT_LEFT + colW * 3.5f, "$testsPassed/${testRecords.size}",
                str(R.string.pdf_stat_passed), str(R.string.pdf_stat_tests))

            advanceY(stripH)
        }

        // ── Records section ───────────────────────────────────────────────────

        private fun drawRecordsSection(type: RecordType, records: List<MaintenanceRecord>) {
            val title = when (type) {
                RecordType.MAINTENANCE -> str(R.string.export_section_maintenance)
                RecordType.REPAIR      -> str(R.string.export_section_repair)
                RecordType.WEAR        -> str(R.string.export_section_wear)
                RecordType.UPGRADE     -> str(R.string.pdf_section_upgrades)
            }
            drawSectionBanner(title)
            advanceY(16f)

            drawTableHeader()
            advanceY(14f)
            paintFill.color = COLOR_DIVIDER
            canvas.drawRect(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y + 1f, paintFill)
            advanceY(8f)

            var totalCost = 0.0
            records.forEachIndexed { idx, record ->
                val extraLines = (if (record.receiptUri != null) 1 else 0) + (if (record.notes != null) 1 else 0)
                val rowH = 20f + extraLines * 13f
                ensureSpaceOrNewPage(rowH + 4f)

                if (idx % 2 == 0) {
                    paintFill.color = Color.parseColor("#F5F8FF")
                    canvas.drawRect(CONTENT_LEFT, y - 3f, CONTENT_LEFT + CONTENT_WIDTH, y + rowH - 3f, paintFill)
                }

                drawTableRow(record)
                record.costAmount?.let { totalCost += it }
                advanceY(rowH)
            }

            if (totalCost > 0) {
                paintFill.color = COLOR_PRIMARY_LIGHT
                canvas.drawRect(CONTENT_LEFT, y - 2f, CONTENT_LEFT + CONTENT_WIDTH, y + 18f, paintFill)
                canvas.drawText(
                    "${str(R.string.pdf_total_expenses)} ₪${formatNumber(totalCost.toInt())}",
                    CONTENT_LEFT + 8f, y + 12f,
                    Paint(paintBold).apply { textAlign = Paint.Align.LEFT; color = COLOR_PRIMARY }
                )
                advanceY(22f)
            }
        }

        private fun drawTableHeader() {
            val h = Paint(paintLabel)
            canvas.drawText(str(R.string.pdf_col_date), COL_DATE_X, y, h)
            canvas.drawText(str(R.string.pdf_col_desc), COL_DESC_X, y, h)
            canvas.drawText(str(R.string.pdf_col_km),   COL_KM_X,   y, h)
            canvas.drawText(str(R.string.pdf_col_cost), COL_COST_X,  y, Paint(h).apply { textAlign = Paint.Align.LEFT })
        }

        private fun drawTableRow(record: MaintenanceRecord) {
            canvas.drawText(record.date.toFormattedDate(), COL_DATE_X, y, paintBody)

            val rawDesc = record.description
            val desc = if (rawDesc.length > 20) rawDesc.take(18) + "…" else rawDesc
            canvas.drawText(desc, COL_DESC_X, y, paintBody)

            val kmText   = record.km?.let { "${formatNumber(it)} ${str(R.string.pdf_km_unit)}" } ?: "—"
            val costText = record.costAmount?.let { "₪${formatNumber(it.toInt())}" } ?: "—"
            canvas.drawText(kmText,   COL_KM_X,   y, paintBodySecondary)
            canvas.drawText(costText, COL_COST_X,  y, Paint(paintBold).apply { textAlign = Paint.Align.LEFT })

            if (record.receiptUri != null) {
                advanceY(13f)
                canvas.drawText(str(R.string.export_receipt_attached), COL_DATE_X, y, Paint(paintSmall).apply { color = COLOR_GREEN })
            }
            if (record.notes != null) {
                advanceY(13f)
                canvas.drawText("${str(R.string.pdf_note_label)} ${record.notes.take(55)}", COL_DATE_X, y, paintSmall)
            }
        }

        // ── Test history ──────────────────────────────────────────────────────

        private fun drawTestHistorySection(records: List<TestRecord>) {
            drawSectionBanner(str(R.string.export_section_test_history))
            advanceY(14f)

            records.forEach { test ->
                ensureSpaceOrNewPage(28f)

                val rowH = 24f
                paintFill.color = if (test.passed) COLOR_GREEN_BG else COLOR_RED_BG
                canvas.drawRoundRect(RectF(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y + rowH), 6f, 6f, paintFill)

                val textY = y + rowH / 2f + 4f
                canvas.drawText(test.date.toFormattedDate(), CONTENT_RIGHT - 8f, textY, paintBody)

                val (resultText, resultColor) = if (test.passed)
                    "${str(R.string.export_test_passed)} ✓" to COLOR_GREEN
                else
                    "${str(R.string.export_test_failed)} ✗" to COLOR_RED
                canvas.drawText(resultText, CONTENT_LEFT + 8f, textY, Paint(paintBold).apply { color = resultColor; textAlign = Paint.Align.LEFT })

                if (test.notes != null) {
                    advanceY(rowH + 2f)
                    canvas.drawText("${str(R.string.pdf_note_label)} ${test.notes.take(60)}", CONTENT_RIGHT - 8f, y + 10f, paintSmall)
                    advanceY(12f)
                } else {
                    advanceY(rowH + 6f)
                }
            }
        }

        // ── Receipt image ─────────────────────────────────────────────────────

        private fun drawReceiptImage(bmp: Bitmap, record: MaintenanceRecord) {
            canvas.drawText(
                "${record.date.toFormattedDate()}  ·  ${record.description.take(45)}",
                CONTENT_RIGHT, y, paintBodySecondary
            )
            advanceY(10f)

            val scale = minOf(CONTENT_WIDTH / bmp.width, 200f / bmp.height)
            val drawW = bmp.width * scale
            val drawH = bmp.height * scale
            val left  = CONTENT_LEFT + (CONTENT_WIDTH - drawW) / 2f
            val rect  = RectF(left, y, left + drawW, y + drawH)

            paintFill.color = COLOR_DIVIDER
            canvas.drawRoundRect(RectF(rect.left - 1f, rect.top - 1f, rect.right + 1f, rect.bottom + 1f), 4f, 4f, paintFill)
            canvas.drawBitmap(bmp, null, rect, null)
            advanceY(drawH + 4f)
        }

        // ── Section banner ────────────────────────────────────────────────────

        private fun drawSectionBanner(title: String) {
            val bannerH = 22f
            paintFill.color = COLOR_PRIMARY
            canvas.drawRoundRect(RectF(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y + bannerH), 6f, 6f, paintFill)
            paintFill.color = COLOR_PRIMARY_DARK
            canvas.drawCircle(CONTENT_LEFT + 14f, y + bannerH / 2f, 7f, paintFill)
            canvas.drawText(title, CONTENT_RIGHT - 8f, y + bannerH / 2f + 4f, paintSectionTitle)
            advanceY(bannerH)
        }

        // ── Image loading ─────────────────────────────────────────────────────

        private fun loadBitmap(uriString: String): Bitmap? = runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()

        // ── Formatters ────────────────────────────────────────────────────────

        private fun formatNumber(n: Int): String = String.format(Locale.getDefault(), "%,d", n)
    }
}
