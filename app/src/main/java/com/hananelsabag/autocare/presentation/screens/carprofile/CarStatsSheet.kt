package com.hananelsabag.autocare.presentation.screens.carprofile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CarRepair
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.RecordType
import com.hananelsabag.autocare.presentation.theme.StatusGreen
import com.hananelsabag.autocare.util.toFormattedDate

@Composable
fun CarStatsSheet(stats: CarDetailedStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 32.dp)
    ) {
        // ── Sheet title ───────────────────────────────────────────────
        Text(
            text = stringResource(R.string.stats_section_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
        )

        // ── Type breakdown (donut + legend) ───────────────────────────
        SheetSection(title = stringResource(R.string.stats_breakdown_title)) {
            TypeBreakdownSection(breakdown = stats.typeBreakdown)
        }

        SheetDivider()

        // ── Monthly spending ──────────────────────────────────────────
        SheetSection(title = stringResource(R.string.stats_monthly_title)) {
            if (stats.monthlySpend.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_no_monthly_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MonthlyBarChart(months = stats.monthlySpend)
            }
        }

        // ── KM section ────────────────────────────────────────────────
        if (stats.kmFrom != null && stats.kmTo != null && stats.kmFrom != stats.kmTo) {
            SheetDivider()
            SheetSection(title = stringResource(R.string.stats_km_title)) {
                KmRangeSection(from = stats.kmFrom, to = stats.kmTo)
            }
        }

        // ── Biggest expense ───────────────────────────────────────────
        if (stats.biggestExpense != null) {
            SheetDivider()
            SheetSection(title = stringResource(R.string.stats_biggest_expense_title)) {
                BiggestExpenseCard(record = stats.biggestExpense)
            }
        }
    }
}

// ─── Type Breakdown ──────────────────────────────────────────────────────────

@Composable
private fun TypeBreakdownSection(breakdown: List<TypeBreakdown>) {
    val total = breakdown.sumOf { it.count }.toFloat()
    val colors = breakdown.map { it.type.accentColor() }
    val fractions = breakdown.map { it.count / total }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Donut chart
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            DonutChart(
                segments = colors.zip(fractions),
                modifier = Modifier.size(130.dp)
            )
            // Center label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = breakdown.sumOf { it.count }.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.stats_total_records),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Legend
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            breakdown.forEach { item ->
                TypeLegendRow(item = item)
            }
        }
    }
}

@Composable
private fun TypeLegendRow(item: TypeBreakdown) {
    val color = item.type.accentColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(item.type.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.stats_records_count, item.count),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.totalCost?.let { cost ->
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.record_cost_format, cost.formatCost()),
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    segments: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 26.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
        val arcSize = Size(radius * 2, radius * 2)
        var startAngle = -90f
        val gapDeg = if (segments.size > 1) 4f else 0f

        segments.forEach { (color, fraction) ->
            val sweep = (fraction * 360f - gapDeg).coerceAtLeast(1f)
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
            startAngle += fraction * 360f
        }
    }
}

// ─── Monthly Bar Chart ────────────────────────────────────────────────────────

@Composable
private fun MonthlyBarChart(months: List<MonthlySpend>) {
    val maxAmount = months.maxOf { it.amount }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        months.forEach { month ->
            MonthlyBarRow(month = month, maxAmount = maxAmount)
        }
    }
}

@Composable
private fun MonthlyBarRow(month: MonthlySpend, maxAmount: Double) {
    val fraction = (month.amount / maxAmount).toFloat().coerceIn(0f, 1f)
    val barColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month label — fixed width
        Text(
            text = month.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
            maxLines = 1
        )
        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(barColor)
            )
        }
        // Amount label
        Text(
            text = stringResource(R.string.record_cost_format, month.amount.formatCost()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = barColor,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

// ─── KM Range ────────────────────────────────────────────────────────────────

@Composable
private fun KmRangeSection(from: Pair<Long, Int>, to: Pair<Long, Int>) {
    val driven = to.second - from.second

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KmMilestoneChip(
            label = stringResource(R.string.stats_km_from_label),
            km = from.second,
            date = from.first,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        KmMilestoneChip(
            label = stringResource(R.string.stats_km_to_label),
            km = to.second,
            date = to.first,
            modifier = Modifier.weight(1f)
        )
    }

    if (driven > 0) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            color = StatusGreen.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.stats_km_driven, driven),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = StatusGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun KmMilestoneChip(label: String, km: Int, date: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.record_km_format, km.toString()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = date.toFormattedDate(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Biggest Expense ─────────────────────────────────────────────────────────

@Composable
private fun BiggestExpenseCard(record: com.hananelsabag.autocare.data.local.entities.MaintenanceRecord) {
    val accentColor = record.type.accentColor()
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = record.type.iconVector(),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = record.date.toFormattedDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.record_cost_format, record.costAmount!!.formatCost()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

private fun Double.formatCost(): String =
    if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()

// RecordType helpers (mirrors MaintenanceHistoryScreen)
private fun RecordType.labelRes(): Int = when (this) {
    RecordType.MAINTENANCE -> R.string.record_type_maintenance
    RecordType.REPAIR -> R.string.record_type_repair
    RecordType.WEAR -> R.string.record_type_wear
    RecordType.UPGRADE -> R.string.record_type_upgrade
}

@Composable
private fun RecordType.accentColor(): Color = when (this) {
    RecordType.MAINTENANCE -> MaterialTheme.colorScheme.primary
    RecordType.REPAIR -> MaterialTheme.colorScheme.error
    RecordType.WEAR -> MaterialTheme.colorScheme.tertiary
    RecordType.UPGRADE -> MaterialTheme.colorScheme.secondary
}

private fun RecordType.iconVector(): ImageVector = when (this) {
    RecordType.MAINTENANCE -> Icons.Outlined.Build
    RecordType.REPAIR -> Icons.Outlined.CarRepair
    RecordType.WEAR -> Icons.Outlined.Autorenew
    RecordType.UPGRADE -> Icons.Outlined.Upgrade
}
