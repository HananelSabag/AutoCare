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

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val CONTENT_RIGHT = PAGE_WIDTH - MARGIN   // RTL: draw from here leftward
private const val CONTENT_LEFT = MARGIN
private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

// Status colors matching the app
private val COLOR_GREEN = Color.parseColor("#4CAF50")
private val COLOR_YELLOW = Color.parseColor("#FFC107")
private val COLOR_RED = Color.parseColor("#F44336")
private val COLOR_GRAY_LIGHT = Color.parseColor("#F5F5F5")
private val COLOR_GRAY_MID = Color.parseColor("#E0E0E0")
private val COLOR_TEXT_PRIMARY = Color.parseColor("#1C1B1F")
private val COLOR_TEXT_SECONDARY = Color.parseColor("#49454F")
private val COLOR_DIVIDER = Color.parseColor("#CAC4D0")

class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carRepository: CarRepository,
    private val maintenanceRepository: MaintenanceRecordRepository,
    private val testRecordRepository: TestRecordRepository,
) {

    // ── Paints ──────────────────────────────────────────────────────────────────

    private val paintHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 22f
        isFakeBoldText = true
        textAlign = Paint.Align.RIGHT
    }
    private val paintSubheader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 16f
        isFakeBoldText = true
        textAlign = Paint.Align.RIGHT
    }
    private val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 11f
        textAlign = Paint.Align.RIGHT
    }
    private val paintBodySecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_SECONDARY
        textSize = 10f
        textAlign = Paint.Align.RIGHT
    }
    private val paintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_SECONDARY
        textSize = 9f
        textAlign = Paint.Align.RIGHT
    }
    private val paintAppTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6750A4")
        textSize = 28f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val paintSectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6750A4")
        textSize = 13f
        isFakeBoldText = true
        textAlign = Paint.Align.RIGHT
    }
    private val paintFill = Paint().apply { style = Paint.Style.FILL }
    private val paintDivider = Paint().apply {
        color = COLOR_DIVIDER
        style = Paint.Style.FILL
    }
    private val paintStatusText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        isFakeBoldText = true
        textAlign = Paint.Align.RIGHT
    }

    // ── State ────────────────────────────────────────────────────────────────────

    private lateinit var document: PdfDocument
    private lateinit var canvas: Canvas
    private lateinit var page: PdfDocument.Page
    private var pageNum = 0
    private var y = 0f

    // ── Public API ───────────────────────────────────────────────────────────────

    suspend fun exportCar(carId: Int): Intent {
        val car = carRepository.getCarById(carId).first() ?: error("Car not found")
        val allRecords = maintenanceRepository.getRecordsForCar(carId).first()
        val testRecords = testRecordRepository.getByCarId(carId).first()

        document = PdfDocument()
        startNewPage()

        // ── Page 1: Car Summary ──────────────────────────────────────────────────
        drawAppHeader()
        drawDivider()
        advanceY(16f)

        drawCarDetails(car)
        advanceY(12f)
        drawStatusSection(car.testExpiryDate, car.insuranceExpiryDate)
        advanceY(12f)
        drawGeneratedAt()

        // ── Maintenance Records ──────────────────────────────────────────────────
        val byType = allRecords.groupBy { it.type }
        val order = listOf(RecordType.MAINTENANCE, RecordType.REPAIR, RecordType.WEAR)
        for (type in order) {
            val records = byType[type] ?: continue
            if (records.isEmpty()) continue
            ensureSpaceOrNewPage(60f)
            advanceY(20f)
            drawRecordsSection(type, records)
        }

        // ── Test History ─────────────────────────────────────────────────────────
        if (testRecords.isNotEmpty()) {
            ensureSpaceOrNewPage(60f)
            advanceY(20f)
            drawTestHistorySection(testRecords)
        }

        // ── Receipt Images Appendix ──────────────────────────────────────────────
        val recordsWithReceipts = allRecords.filter { it.receiptUri != null }
        if (recordsWithReceipts.isNotEmpty()) {
            startNewPage()
            advanceY(8f)
            drawSectionHeader("צילומי קבלות")
            advanceY(8f)
            for (record in recordsWithReceipts) {
                val bmp = loadBitmap(record.receiptUri!!) ?: continue
                ensureSpaceOrNewPage(220f)
                drawReceiptImage(bmp, record)
                advanceY(12f)
            }
        }

        page.let { document.finishPage(it) }

        // Write to cache
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fileName = "autocare_${car.make}_${car.model}_$dateStr.pdf"
            .replace(" ", "_")
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // ── Page management ──────────────────────────────────────────────────────────

    private fun startNewPage() {
        if (pageNum > 0) document.finishPage(page)
        pageNum++
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        page = document.startPage(info)
        canvas = page.canvas
        y = MARGIN
    }

    private fun advanceY(amount: Float) {
        y += amount
    }

    private fun ensureSpaceOrNewPage(neededHeight: Float) {
        if (y + neededHeight > PAGE_HEIGHT - MARGIN) {
            startNewPage()
        }
    }

    // ── Drawing helpers ──────────────────────────────────────────────────────────

    private fun drawDivider() {
        advanceY(4f)
        canvas.drawRect(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y + 1f, paintDivider)
        advanceY(4f)
    }

    private fun drawText(text: String, paint: Paint, x: Float = CONTENT_RIGHT) {
        canvas.drawText(text, x, y, paint)
    }

    private fun drawTextLeft(text: String, paint: Paint) {
        val leftAligned = Paint(paint).apply { textAlign = Paint.Align.LEFT }
        canvas.drawText(text, CONTENT_LEFT, y, leftAligned)
    }

    // ── Content sections ─────────────────────────────────────────────────────────

    private fun drawAppHeader() {
        advanceY(paintAppTitle.textSize)
        canvas.drawText("AutoCare", PAGE_WIDTH / 2f, y, paintAppTitle)
        advanceY(6f)
        paintSmall.textAlign = Paint.Align.CENTER
        canvas.drawText("דוח רכב אישי", PAGE_WIDTH / 2f, y, paintSmall)
        paintSmall.textAlign = Paint.Align.RIGHT
        advanceY(10f)
    }

    private fun drawCarDetails(car: com.hananelsabag.autocare.data.local.entities.Car) {
        drawSectionHeader("פרטי הרכב")
        advanceY(14f)

        // Make + model + year (large)
        val title = "${car.make} ${car.model} (${car.year})"
        drawText(title, paintHeader)
        advanceY(20f)

        // License plate
        drawText("לוחית רישוי: ${car.licensePlate}", paintBody)
        advanceY(16f)

        // Color + KM row
        val details = buildString {
            if (car.color != null) append("צבע: ${car.color}")
            if (car.currentKm != null) {
                if (car.color != null) append("   |   ")
                append("קילומטראז': ${formatNumber(car.currentKm)} ק\"מ")
            }
        }
        if (details.isNotEmpty()) {
            drawText(details, paintBodySecondary)
            advanceY(16f)
        }

        if (car.notes != null) {
            drawText("הערות: ${car.notes}", paintBodySecondary)
            advanceY(16f)
        }
    }

    private fun drawStatusSection(testExpiry: Long?, insuranceExpiry: Long?) {
        drawSectionHeader("סטטוס")
        advanceY(14f)
        drawStatusRow("טסט", testExpiry)
        advanceY(18f)
        drawStatusRow("ביטוח חובה", insuranceExpiry)
        advanceY(6f)
    }

    private fun drawStatusRow(label: String, expiryMs: Long?) {
        // Label on right
        drawText(label, paintBody)

        if (expiryMs == null) {
            paintStatusText.color = COLOR_GRAY_MID
            val leftPaint = Paint(paintStatusText).apply { textAlign = Paint.Align.LEFT }
            canvas.drawText("לא הוגדר", CONTENT_LEFT, y, leftPaint)
            return
        }

        val days = expiryMs.daysFromNow()
        val dateStr = expiryMs.toFormattedDate()
        val (statusText, statusColor) = when {
            days < 0 -> "פג תוקף!" to COLOR_RED
            days == 0L -> "פג תוקף היום" to COLOR_RED
            days <= 7 -> "עוד $days ימים · $dateStr" to COLOR_RED
            days <= 30 -> "עוד $days ימים · $dateStr" to COLOR_YELLOW
            else -> "תקף עד $dateStr · עוד $days ימים" to COLOR_GREEN
        }
        paintStatusText.color = statusColor
        val leftPaint = Paint(paintStatusText).apply { textAlign = Paint.Align.LEFT }
        canvas.drawText(statusText, CONTENT_LEFT, y, leftPaint)
    }

    private fun drawGeneratedAt() {
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        advanceY(4f)
        drawText("הופק ב: $dateStr", paintSmall)
        advanceY(10f)
    }

    private fun drawSectionHeader(title: String) {
        drawText(title, paintSectionTitle)
        advanceY(4f)
        // Underline
        canvas.drawRect(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y + 1.5f, paintDivider)
        // (caller is responsible for advanceY after calling this)
    }

    private fun drawRecordsSection(type: RecordType, records: List<MaintenanceRecord>) {
        val sectionTitle = when (type) {
            RecordType.MAINTENANCE -> "טיפולים תקופתיים"
            RecordType.REPAIR -> "תיקונים"
            RecordType.WEAR -> "בלאי"
        }
        drawSectionHeader(sectionTitle)
        advanceY(16f)

        // Column headers
        drawTableRowHeader()
        advanceY(18f)
        drawDivider()

        var totalCost = 0.0
        records.forEachIndexed { index, record ->
            ensureSpaceOrNewPage(22f)

            // Alternating row background
            if (index % 2 == 0) {
                paintFill.color = COLOR_GRAY_LIGHT
                canvas.drawRect(CONTENT_LEFT, y - 13f, CONTENT_LEFT + CONTENT_WIDTH, y + 5f, paintFill)
            }

            drawTableRow(record)
            record.costAmount?.let { totalCost += it }
            advanceY(20f)
        }

        // Total
        if (totalCost > 0) {
            drawDivider()
            advanceY(4f)
            val leftPaint = Paint(paintBody).apply {
                textAlign = Paint.Align.LEFT
                isFakeBoldText = true
            }
            canvas.drawText("סה\"כ: ₪${formatNumber(totalCost.toInt())}", CONTENT_LEFT, y, leftPaint)
            advanceY(12f)
        }
    }

    private fun drawTableRowHeader() {
        // Columns (RTL order): date | description | km | cost
        val col1 = CONTENT_RIGHT          // Date (right)
        val col2 = CONTENT_RIGHT - 80f   // Description
        val col3 = CONTENT_LEFT + 80f    // KM (left area)
        val col4 = CONTENT_LEFT          // Cost (leftmost)

        val headerPaint = Paint(paintSmall).apply {
            isFakeBoldText = true
            color = COLOR_TEXT_SECONDARY
        }
        val leftHeader = Paint(headerPaint).apply { textAlign = Paint.Align.LEFT }

        canvas.drawText("תאריך", col1, y, headerPaint)
        canvas.drawText("תיאור", col2, y, headerPaint)
        canvas.drawText("ק\"מ", col3, y, headerPaint)
        canvas.drawText("עלות", col4, y, leftHeader)
    }

    private fun drawTableRow(record: MaintenanceRecord) {
        val col1 = CONTENT_RIGHT
        val col2 = CONTENT_RIGHT - 80f
        val col3 = CONTENT_LEFT + 80f
        val col4 = CONTENT_LEFT

        val leftPaint = Paint(paintBody).apply { textAlign = Paint.Align.LEFT }

        canvas.drawText(record.date.toFormattedDate(), col1, y, paintBody)

        // Description — truncate if too long
        val desc = if (record.description.length > 28) record.description.take(25) + "..." else record.description
        canvas.drawText(desc, col2, y, paintBody)

        val kmText = record.km?.let { formatNumber(it) + " ק\"מ" } ?: "—"
        canvas.drawText(kmText, col3, y, paintBody)

        val costText = record.costAmount?.let { "₪${formatNumber(it.toInt())}" } ?: "—"
        canvas.drawText(costText, col4, y, leftPaint)

        // Receipt indicator on next line (small)
        if (record.receiptUri != null) {
            advanceY(11f)
            val receiptPaint = Paint(paintSmall).apply { color = COLOR_GREEN }
            canvas.drawText("קבלה מצורפת ✓", col1, y, receiptPaint)
        }

        // Notes on next line (small)
        if (record.notes != null) {
            advanceY(11f)
            canvas.drawText("הערה: ${record.notes.take(50)}", col1, y, paintSmall)
        }
    }

    private fun drawTestHistorySection(records: List<TestRecord>) {
        drawSectionHeader("היסטוריית טסטים")
        advanceY(16f)

        records.forEach { test ->
            ensureSpaceOrNewPage(22f)

            val dateStr = test.date.toFormattedDate()
            val resultText = if (test.passed) "עבר ✓" else "נכשל ✗"
            val resultColor = if (test.passed) COLOR_GREEN else COLOR_RED

            drawText(dateStr, paintBody)

            val resultPaint = Paint(paintBody).apply {
                color = resultColor
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText(resultText, CONTENT_LEFT, y, resultPaint)

            if (test.notes != null) {
                advanceY(12f)
                canvas.drawText("הערה: ${test.notes.take(60)}", CONTENT_RIGHT, y, paintSmall)
            }
            advanceY(18f)
        }
    }

    private fun drawReceiptImage(bmp: Bitmap, record: MaintenanceRecord) {
        // Label
        val label = "${record.date.toFormattedDate()} · ${record.description.take(40)}"
        drawText(label, paintBodySecondary)
        advanceY(8f)

        // Scale image to fit within content width, max 200dp height
        val maxW = CONTENT_WIDTH
        val maxH = 200f
        val scale = minOf(maxW / bmp.width, maxH / bmp.height)
        val drawW = bmp.width * scale
        val drawH = bmp.height * scale

        val left = CONTENT_LEFT + (CONTENT_WIDTH - drawW) / 2f
        val rect = RectF(left, y, left + drawW, y + drawH)
        canvas.drawBitmap(bmp, null, rect, null)
        advanceY(drawH + 4f)
    }

    // ── Image loading ─────────────────────────────────────────────────────────────

    private fun loadBitmap(uriString: String): Bitmap? = try {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (_: Exception) {
        null
    }

    // ── Formatters ────────────────────────────────────────────────────────────────

    private fun formatNumber(n: Int): String =
        String.format(Locale.getDefault(), "%,d", n)
}
