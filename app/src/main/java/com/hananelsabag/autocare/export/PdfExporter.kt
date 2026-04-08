package com.hananelsabag.autocare.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
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

// ── Page constants ────────────────────────────────────────────────────────────
private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val CONTENT_RIGHT = PAGE_WIDTH - MARGIN
private const val CONTENT_LEFT = MARGIN
private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

// ── Brand colors ──────────────────────────────────────────────────────────────
private val COLOR_PRIMARY        = Color.parseColor("#6750A4")   // Material You purple
private val COLOR_PRIMARY_DARK   = Color.parseColor("#4F378B")
private val COLOR_PRIMARY_LIGHT  = Color.parseColor("#EADDFF")
private val COLOR_ON_PRIMARY     = Color.WHITE

private val COLOR_GREEN          = Color.parseColor("#2E7D32")
private val COLOR_GREEN_BG       = Color.parseColor("#E8F5E9")
private val COLOR_YELLOW         = Color.parseColor("#F57F17")
private val COLOR_YELLOW_BG      = Color.parseColor("#FFF8E1")
private val COLOR_RED            = Color.parseColor("#C62828")
private val COLOR_RED_BG         = Color.parseColor("#FFEBEE")

private val COLOR_SURFACE        = Color.parseColor("#FFFBFE")
private val COLOR_SURFACE_VAR    = Color.parseColor("#F4EFF4")
private val COLOR_TEXT_PRIMARY   = Color.parseColor("#1C1B1F")
private val COLOR_TEXT_SECONDARY = Color.parseColor("#49454F")
private val COLOR_DIVIDER        = Color.parseColor("#CAC4D0")
private val COLOR_CARD           = Color.parseColor("#F6F2FF")   // light purple tint

class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carRepository: CarRepository,
    private val maintenanceRepository: MaintenanceRecordRepository,
    private val testRecordRepository: TestRecordRepository,
) {

    // ── Paints ────────────────────────────────────────────────────────────────

    private val paintFill = Paint().apply { style = Paint.Style.FILL }
    private val paintDivider = Paint().apply {
        color = COLOR_DIVIDER; style = Paint.Style.FILL
    }

    private val paintCarTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY; textSize = 20f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
    }
    private val paintSectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_ON_PRIMARY; textSize = 12f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
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
        color = COLOR_TEXT_SECONDARY; textSize = 9f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
    }
    private val paintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY; textSize = 11f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
    }
    private val paintPrimary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_PRIMARY; textSize = 11f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
    }
    private val paintStatusBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f; isFakeBoldText = true; textAlign = Paint.Align.LEFT
    }
    private val paintPageNum = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_SECONDARY; textSize = 8f; textAlign = Paint.Align.CENTER
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private lateinit var document: PdfDocument
    private lateinit var canvas: Canvas
    private lateinit var page: PdfDocument.Page
    private var pageNum = 0
    private var y = 0f

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun exportCar(carId: Int): Intent {
        val car = carRepository.getCarById(carId).first() ?: error("Car not found")
        val allRecords = maintenanceRepository.getRecordsForCar(carId).first()
        val testRecords = testRecordRepository.getByCarId(carId).first()

        document = PdfDocument()
        startNewPage()

        // ── Cover header ─────────────────────────────────────────────────────
        drawCoverHeader()

        // ── Car details card ─────────────────────────────────────────────────
        drawCarCard(car)
        advanceY(14f)

        // ── Status badges ────────────────────────────────────────────────────
        drawStatusSection(car.testExpiryDate, car.insuranceExpiryDate)
        advanceY(14f)

        // ── Stats strip ──────────────────────────────────────────────────────
        drawStatsStrip(allRecords, testRecords)
        advanceY(20f)

        // ── Maintenance records ──────────────────────────────────────────────
        val byType = allRecords.groupBy { it.type }
        val order = listOf(RecordType.MAINTENANCE, RecordType.REPAIR, RecordType.WEAR)
        for (type in order) {
            val records = byType[type] ?: continue
            if (records.isEmpty()) continue
            ensureSpaceOrNewPage(80f)
            drawRecordsSection(type, records)
            advanceY(16f)
        }

        // ── Test history ─────────────────────────────────────────────────────
        if (testRecords.isNotEmpty()) {
            ensureSpaceOrNewPage(80f)
            drawTestHistorySection(testRecords)
            advanceY(16f)
        }

        // ── Receipts appendix ────────────────────────────────────────────────
        val withReceipts = allRecords.filter { it.receiptUri != null }
        if (withReceipts.isNotEmpty()) {
            startNewPage()
            drawSectionBanner("צילומי קבלות ואסמכתאות")
            advanceY(10f)
            for (record in withReceipts) {
                val bmp = loadBitmap(record.receiptUri!!) ?: continue
                ensureSpaceOrNewPage(230f)
                drawReceiptImage(bmp, record)
                advanceY(14f)
            }
        }

        finishDocumentWithFooters()

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fileName = "AutoCare_${car.make}_${car.model}_$dateStr.pdf".replace(" ", "_")
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // ── Page management ───────────────────────────────────────────────────────

    private fun startNewPage() {
        if (pageNum > 0) document.finishPage(page)
        pageNum++
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        page = document.startPage(info)
        canvas = page.canvas
        y = MARGIN
    }

    /** Finish all pages at the end, adding footers retroactively is not possible in PdfDocument.
     *  Instead we draw the footer on each page during finishPage via a wrapper. */
    private fun finishDocumentWithFooters() {
        drawPageFooter()          // draw footer on the last (current) page
        document.finishPage(page)
    }

    private fun advanceY(amount: Float) { y += amount }

    private fun ensureSpaceOrNewPage(neededHeight: Float) {
        if (y + neededHeight > PAGE_HEIGHT - MARGIN - 20f) {
            drawPageFooter()
            startNewPage()
        }
    }

    private fun drawPageFooter() {
        val footerY = PAGE_HEIGHT - 18f
        // Divider line
        paintFill.color = COLOR_DIVIDER
        canvas.drawRect(CONTENT_LEFT, footerY - 6f, CONTENT_LEFT + CONTENT_WIDTH, footerY - 5f, paintFill)
        // Left: app name
        val leftPaint = Paint(paintPageNum).apply { textAlign = Paint.Align.LEFT }
        canvas.drawText("AutoCare · דוח רכב אישי", CONTENT_LEFT, footerY, leftPaint)
        // Right: page number
        val rightPaint = Paint(paintPageNum).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("עמוד $pageNum", CONTENT_RIGHT, footerY, rightPaint)
    }

    // ── Cover header ──────────────────────────────────────────────────────────

    private fun drawCoverHeader() {
        val headerH = 80f

        // Gradient-style banner (two-tone rectangles)
        paintFill.color = COLOR_PRIMARY
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerH, paintFill)
        paintFill.color = COLOR_PRIMARY_DARK
        canvas.drawRect(0f, headerH - 4f, PAGE_WIDTH.toFloat(), headerH, paintFill)

        // Logo circle on right side of banner
        val logoBmp = loadAppLogo()
        val logoSize = 52f
        val logoX = CONTENT_RIGHT - logoSize / 2f
        val logoY = headerH / 2f
        if (logoBmp != null) {
            // White circle behind logo
            paintFill.color = Color.WHITE
            canvas.drawCircle(logoX, logoY, logoSize / 2f + 3f, paintFill)
            val logoRect = RectF(
                logoX - logoSize / 2f, logoY - logoSize / 2f,
                logoX + logoSize / 2f, logoY + logoSize / 2f
            )
            canvas.drawBitmap(logoBmp, null, logoRect, null)
        }

        // App name — centered vertically in banner, to the right of center (RTL)
        val appNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ON_PRIMARY; textSize = 26f; isFakeBoldText = true
        }
        canvas.drawText("AutoCare", PAGE_WIDTH / 2f - 30f, headerH / 2f + 10f, appNamePaint)

        // Subtitle
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8DEF8"); textSize = 11f
        }
        canvas.drawText("דוח רכב אישי", PAGE_WIDTH / 2f - 30f, headerH / 2f + 26f, subtitlePaint)

        y = headerH + 16f
    }

    private fun loadAppLogo(): Bitmap? = try {
        BitmapFactory.decodeResource(context.resources, R.drawable.autocare_logo)
    } catch (_: Exception) {
        null
    }

    // ── Car details card ──────────────────────────────────────────────────────

    private fun drawCarCard(car: com.hananelsabag.autocare.data.local.entities.Car) {
        val cardH = 74f
        val cardTop = y

        // Card background
        paintFill.color = COLOR_CARD
        canvas.drawRoundRect(
            RectF(CONTENT_LEFT, cardTop, CONTENT_LEFT + CONTENT_WIDTH, cardTop + cardH),
            10f, 10f, paintFill
        )
        // Left accent stripe
        paintFill.color = COLOR_PRIMARY
        canvas.drawRoundRect(
            RectF(CONTENT_LEFT, cardTop, CONTENT_LEFT + 4f, cardTop + cardH),
            2f, 2f, paintFill
        )

        y = cardTop + 16f

        // Car title (make + model + year)
        val title = "${car.make} ${car.model} · ${car.year}"
        drawText(title, paintCarTitle)
        advanceY(18f)

        // License plate badge
        val plateText = "  ${car.licensePlate}  "
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY; textSize = 10f; isFakeBoldText = true; textAlign = Paint.Align.LEFT
        }
        paintFill.color = Color.WHITE
        val plateWidth = platePaint.measureText(plateText)
        canvas.drawRoundRect(
            RectF(CONTENT_LEFT + 8f, y - 11f, CONTENT_LEFT + 8f + plateWidth + 2f, y + 3f),
            4f, 4f, paintFill
        )
        canvas.drawText(plateText, CONTENT_LEFT + 8f, y, platePaint)

        // Details (color | km | notes)
        val detailParts = buildList {
            car.color?.let { add("צבע: $it") }
            car.currentKm?.let { add("ק\"מ: ${formatNumber(it)}") }
        }
        if (detailParts.isNotEmpty()) {
            val detailText = detailParts.joinToString("   ·   ")
            val detailRight = CONTENT_RIGHT - 8f
            canvas.drawText(detailText, detailRight, y, paintBodySecondary)
        }

        advanceY(20f)

        if (car.notes != null) {
            val noteRight = CONTENT_RIGHT - 8f
            canvas.drawText("הערות: ${car.notes.take(70)}", noteRight, y, paintSmall)
        }

        y = cardTop + cardH
    }

    // ── Status section ────────────────────────────────────────────────────────

    private fun drawStatusSection(testExpiry: Long?, insuranceExpiry: Long?) {
        drawSectionBanner("סטטוס רכב")
        advanceY(10f)

        val rowH = 32f
        drawStatusRow("טסט", testExpiry, y, rowH)
        advanceY(rowH + 8f)
        drawStatusRow("ביטוח חובה", insuranceExpiry, y, rowH)
        advanceY(rowH)
    }

    private fun drawStatusRow(label: String, expiryMs: Long?, rowY: Float, rowH: Float) {
        // Row background
        paintFill.color = COLOR_SURFACE_VAR
        canvas.drawRoundRect(
            RectF(CONTENT_LEFT, rowY, CONTENT_LEFT + CONTENT_WIDTH, rowY + rowH),
            8f, 8f, paintFill
        )

        val textY = rowY + rowH / 2f + 4f

        // Label on right
        canvas.drawText(label, CONTENT_RIGHT - 8f, textY, paintBold)

        if (expiryMs == null) {
            paintStatusBadge.color = COLOR_TEXT_SECONDARY
            canvas.drawText("לא הוגדר", CONTENT_LEFT + 8f, textY, paintStatusBadge)
            return
        }

        val days = expiryMs.daysFromNow()
        val dateStr = expiryMs.toFormattedDate()
        val (statusText, textColor, bgColor) = when {
            days < 0    -> Triple("פג תוקף!", COLOR_RED, COLOR_RED_BG)
            days == 0L  -> Triple("פג תוקף היום!", COLOR_RED, COLOR_RED_BG)
            days <= 7   -> Triple("עוד $days ימים · $dateStr", COLOR_RED, COLOR_RED_BG)
            days <= 30  -> Triple("עוד $days ימים · $dateStr", COLOR_YELLOW, COLOR_YELLOW_BG)
            else        -> Triple("תקף עד $dateStr · עוד $days ימים", COLOR_GREEN, COLOR_GREEN_BG)
        }

        // Badge bg
        paintFill.color = bgColor
        val badgeTextPaint = Paint(paintStatusBadge).apply { color = textColor }
        val badgeWidth = badgeTextPaint.measureText("  $statusText  ")
        canvas.drawRoundRect(
            RectF(CONTENT_LEFT + 4f, rowY + 6f, CONTENT_LEFT + 4f + badgeWidth, rowY + rowH - 6f),
            6f, 6f, paintFill
        )
        canvas.drawText("  $statusText", CONTENT_LEFT + 4f, textY, badgeTextPaint)
    }

    // ── Stats strip ───────────────────────────────────────────────────────────

    private fun drawStatsStrip(records: List<MaintenanceRecord>, testRecords: List<TestRecord>) {
        val totalCost = records.mapNotNull { it.costAmount }.sum()
        val totalRecords = records.size
        val lastMaintenanceDate = records.filter { it.type == RecordType.MAINTENANCE }
            .maxByOrNull { it.date }?.date
        val testsPassed = testRecords.count { it.passed }

        val stripH = 56f
        val colW = CONTENT_WIDTH / 4f

        // Strip background
        paintFill.color = COLOR_PRIMARY
        canvas.drawRoundRect(
            RectF(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y + stripH),
            10f, 10f, paintFill
        )

        val statLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8DEF8"); textSize = 8f; textAlign = Paint.Align.CENTER
        }
        val statValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ON_PRIMARY; textSize = 14f; isFakeBoldText = true; textAlign = Paint.Align.CENTER
        }
        val statUnitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D0BCFF"); textSize = 8f; textAlign = Paint.Align.CENTER
        }

        // Dividers between columns
        paintFill.color = Color.parseColor("#7965AF")
        for (i in 1..3) {
            val divX = CONTENT_LEFT + colW * i
            canvas.drawRect(divX - 0.5f, y + 10f, divX + 0.5f, y + stripH - 10f, paintFill)
        }

        // Column 1: total records
        val col1X = CONTENT_LEFT + colW * 0.5f
        canvas.drawText("$totalRecords", col1X, y + 24f, statValuePaint)
        canvas.drawText("רשומות", col1X, y + 36f, statUnitPaint)
        canvas.drawText("סה\"כ", col1X, y + 48f, statLabelPaint)

        // Column 2: total cost
        val col2X = CONTENT_LEFT + colW * 1.5f
        val costStr = if (totalCost > 0) "₪${formatNumber(totalCost.toInt())}" else "—"
        canvas.drawText(costStr, col2X, y + 24f, statValuePaint)
        canvas.drawText("הוצאות", col2X, y + 36f, statUnitPaint)
        canvas.drawText("סה\"כ", col2X, y + 48f, statLabelPaint)

        // Column 3: last maintenance
        val col3X = CONTENT_LEFT + colW * 2.5f
        val lastStr = lastMaintenanceDate?.toFormattedDate() ?: "—"
        canvas.drawText(lastStr, col3X, y + 24f, Paint(statValuePaint).apply { textSize = 11f })
        canvas.drawText("טיפול", col3X, y + 36f, statUnitPaint)
        canvas.drawText("אחרון", col3X, y + 48f, statLabelPaint)

        // Column 4: tests passed
        val col4X = CONTENT_LEFT + colW * 3.5f
        canvas.drawText("$testsPassed/${testRecords.size}", col4X, y + 24f, statValuePaint)
        canvas.drawText("עברו", col4X, y + 36f, statUnitPaint)
        canvas.drawText("טסטים", col4X, y + 48f, statLabelPaint)

        advanceY(stripH)
    }

    // ── Records section ───────────────────────────────────────────────────────

    private fun drawRecordsSection(type: RecordType, records: List<MaintenanceRecord>) {
        val title = when (type) {
            RecordType.MAINTENANCE -> "טיפולים תקופתיים"
            RecordType.REPAIR      -> "תיקונים"
            RecordType.WEAR        -> "בלאי"
            RecordType.UPGRADE     -> "שדרוגים"
        }
        drawSectionBanner(title)
        advanceY(10f)

        // Table header
        drawTableHeader()
        advanceY(16f)
        paintFill.color = COLOR_DIVIDER
        canvas.drawRect(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y + 1f, paintFill)
        advanceY(6f)

        var totalCost = 0.0
        records.forEachIndexed { idx, record ->
            val extraLines = (if (record.receiptUri != null) 1 else 0) + (if (record.notes != null) 1 else 0)
            val rowHeight = 18f + extraLines * 12f
            ensureSpaceOrNewPage(rowHeight + 4f)

            if (idx % 2 == 0) {
                paintFill.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(CONTENT_LEFT, y - 2f, CONTENT_LEFT + CONTENT_WIDTH, y + rowHeight - 2f, paintFill)
            }

            drawTableRow(record)
            record.costAmount?.let { totalCost += it }
            advanceY(rowHeight)
        }

        // Total row
        if (totalCost > 0) {
            paintFill.color = COLOR_PRIMARY_LIGHT
            canvas.drawRect(CONTENT_LEFT, y - 2f, CONTENT_LEFT + CONTENT_WIDTH, y + 18f, paintFill)
            val totalPaint = Paint(paintBold).apply { textAlign = Paint.Align.LEFT; color = COLOR_PRIMARY }
            canvas.drawText("₪${formatNumber(totalCost.toInt())}  :סה\"כ הוצאות", CONTENT_LEFT + 8f, y + 12f, totalPaint)
            advanceY(22f)
        }
    }

    private fun drawTableHeader() {
        val headerPaint = Paint(paintLabel).apply { color = COLOR_TEXT_SECONDARY }
        val headerLeft = Paint(headerPaint).apply { textAlign = Paint.Align.LEFT }

        canvas.drawText("תאריך", CONTENT_RIGHT - 4f, y, headerPaint)
        canvas.drawText("תיאור", CONTENT_RIGHT - 85f, y, headerPaint)
        canvas.drawText("ק\"מ", CONTENT_LEFT + 90f, y, headerPaint)
        canvas.drawText("עלות", CONTENT_LEFT + 4f, y, headerLeft)
    }

    private fun drawTableRow(record: MaintenanceRecord) {
        canvas.drawText(record.date.toFormattedDate(), CONTENT_RIGHT - 4f, y, paintBody)

        val desc = if (record.description.length > 28) record.description.take(25) + "…" else record.description
        canvas.drawText(desc, CONTENT_RIGHT - 85f, y, paintBody)

        val kmText = record.km?.let { "${formatNumber(it)} ק\"מ" } ?: "—"
        canvas.drawText(kmText, CONTENT_LEFT + 90f, y, paintBodySecondary)

        val costText = record.costAmount?.let { "₪${formatNumber(it.toInt())}" } ?: "—"
        val leftPaint = Paint(paintBold).apply { textAlign = Paint.Align.LEFT }
        canvas.drawText(costText, CONTENT_LEFT + 4f, y, leftPaint)

        if (record.receiptUri != null) {
            advanceY(12f)
            val receiptPaint = Paint(paintSmall).apply { color = COLOR_GREEN }
            canvas.drawText("✓ קבלה מצורפת", CONTENT_RIGHT - 4f, y, receiptPaint)
        }
        if (record.notes != null) {
            advanceY(12f)
            canvas.drawText("הערה: ${record.notes.take(55)}", CONTENT_RIGHT - 4f, y, paintSmall)
        }
    }

    // ── Test history ──────────────────────────────────────────────────────────

    private fun drawTestHistorySection(records: List<TestRecord>) {
        drawSectionBanner("היסטוריית טסטים")
        advanceY(10f)

        records.forEach { test ->
            ensureSpaceOrNewPage(28f)

            val rowH = 24f
            paintFill.color = if (test.passed) COLOR_GREEN_BG else COLOR_RED_BG
            canvas.drawRoundRect(
                RectF(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y + rowH),
                6f, 6f, paintFill
            )

            val textY = y + rowH / 2f + 4f
            canvas.drawText(test.date.toFormattedDate(), CONTENT_RIGHT - 8f, textY, paintBody)

            val (resultText, resultColor) = if (test.passed) "עבר ✓" to COLOR_GREEN else "נכשל ✗" to COLOR_RED
            val resultPaint = Paint(paintBold).apply { color = resultColor; textAlign = Paint.Align.LEFT }
            canvas.drawText(resultText, CONTENT_LEFT + 8f, textY, resultPaint)

            if (test.notes != null) {
                advanceY(rowH + 2f)
                canvas.drawText("הערה: ${test.notes.take(60)}", CONTENT_RIGHT - 8f, y + 10f, paintSmall)
                advanceY(12f)
            } else {
                advanceY(rowH + 6f)
            }
        }
    }

    // ── Receipt image ─────────────────────────────────────────────────────────

    private fun drawReceiptImage(bmp: Bitmap, record: MaintenanceRecord) {
        val label = "${record.date.toFormattedDate()}  ·  ${record.description.take(45)}"
        canvas.drawText(label, CONTENT_RIGHT, y, paintBodySecondary)
        advanceY(10f)

        val maxW = CONTENT_WIDTH
        val maxH = 200f
        val scale = minOf(maxW / bmp.width, maxH / bmp.height)
        val drawW = bmp.width * scale
        val drawH = bmp.height * scale
        val left = CONTENT_LEFT + (CONTENT_WIDTH - drawW) / 2f
        val rect = RectF(left, y, left + drawW, y + drawH)

        // Shadow-like border
        paintFill.color = COLOR_DIVIDER
        canvas.drawRoundRect(RectF(rect.left - 1f, rect.top - 1f, rect.right + 1f, rect.bottom + 1f), 4f, 4f, paintFill)
        canvas.drawBitmap(bmp, null, rect, null)
        advanceY(drawH + 4f)
    }

    // ── Section banner ────────────────────────────────────────────────────────

    private fun drawSectionBanner(title: String) {
        val bannerH = 22f
        paintFill.color = COLOR_PRIMARY
        canvas.drawRoundRect(
            RectF(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y + bannerH),
            6f, 6f, paintFill
        )
        // Icon circle
        paintFill.color = COLOR_PRIMARY_DARK
        canvas.drawCircle(CONTENT_LEFT + 14f, y + bannerH / 2f, 7f, paintFill)

        canvas.drawText(title, CONTENT_RIGHT - 8f, y + bannerH / 2f + 4f, paintSectionTitle)
        advanceY(bannerH)
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private fun drawText(text: String, paint: Paint, x: Float = CONTENT_RIGHT) {
        canvas.drawText(text, x, y, paint)
    }

    // ── Image loading ─────────────────────────────────────────────────────────

    private fun loadBitmap(uriString: String): Bitmap? = try {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
        null
    }

    // ── Formatters ────────────────────────────────────────────────────────────

    private fun formatNumber(n: Int): String =
        String.format(Locale.getDefault(), "%,d", n)
}
