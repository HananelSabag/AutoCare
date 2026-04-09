package com.hananelsabag.autocare.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.presentation.theme.StatusGreen
import com.hananelsabag.autocare.presentation.theme.StatusGreenContainer
import com.hananelsabag.autocare.presentation.theme.StatusRed
import com.hananelsabag.autocare.presentation.theme.StatusRedContainer
import com.hananelsabag.autocare.presentation.theme.StatusYellow
import com.hananelsabag.autocare.presentation.theme.StatusYellowContainer
import com.hananelsabag.autocare.util.StatusLevel
import com.hananelsabag.autocare.util.carColorToComposeColor
import com.hananelsabag.autocare.util.getStatusLevel

@Composable
fun CarCard(
    car: Car,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val worstLevel = listOf(
        getStatusLevel(car.testExpiryDate),
        getStatusLevel(car.insuranceExpiryDate)
    ).maxByOrNull { it.ordinal } ?: StatusLevel.UNKNOWN

    val accentColor = when (worstLevel) {
        StatusLevel.GREEN -> StatusGreen
        StatusLevel.YELLOW -> StatusYellow
        StatusLevel.RED, StatusLevel.EXPIRED -> StatusRed
        StatusLevel.UNKNOWN -> MaterialTheme.colorScheme.primary
    }

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        // Row with IntrinsicSize.Min so the accent bar can fillMaxHeight
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // ── Left accent bar — full card height ───────────────────
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            // ── Main content ─────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {

                // Photo / placeholder hero area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                ) {
                    if (car.photoUri != null) {
                        AsyncImage(
                            model = car.photoUri,
                            contentDescription = stringResource(R.string.content_description_car_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Gradient placeholder — no photo
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f)
                            )
                        }
                    }

                    // Gradient scrim — transparent at top, dark at bottom
                    // Gives the text overlay contrast on any photo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .align(Alignment.BottomStart)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                )
                            )
                    )

                    // Make + Model overlaid on bottom of hero
                    Text(
                        text = "${car.make} ${car.model}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                // ── Info row ─────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(
                        start = 14.dp, end = 14.dp,
                        top = 10.dp, bottom = 12.dp
                    )
                ) {
                    // Year · Plate · KM
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = car.year.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SeparatorDot()
                        LicensePlateBadge(plate = car.licensePlate)
                        if (car.currentKm != null) {
                            SeparatorDot()
                            Text(
                                text = stringResource(
                                    R.string.car_card_km_format,
                                    car.currentKm.toString()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status chips + color dot
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(
                            label = stringResource(R.string.car_card_test_label),
                            level = getStatusLevel(car.testExpiryDate)
                        )
                        if (car.insuranceExpiryDate != null) {
                            StatusChip(
                                label = stringResource(R.string.car_card_insurance_label),
                                level = getStatusLevel(car.insuranceExpiryDate)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (car.color != null) {
                            ColorDot(colorName = car.color)
                        }
                    }
                }
            }
        }
    }
}

/** Yellow Israeli license plate badge — shared across all car UI surfaces. */
@Composable
fun IsraeliLicensePlate(plate: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFFFD700), MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = plate,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = TextUnit(1.5f, TextUnitType.Sp)
        )
    }
}

@Composable
private fun SeparatorDot() {
    Box(
        modifier = Modifier
            .size(4.dp)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                CircleShape
            )
    )
}

@Composable
private fun LicensePlateBadge(plate: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = plate,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun StatusChip(label: String, level: StatusLevel) {
    val dotColor = when (level) {
        StatusLevel.GREEN -> StatusGreen
        StatusLevel.YELLOW -> StatusYellow
        StatusLevel.RED, StatusLevel.EXPIRED -> StatusRed
        StatusLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val containerColor = when (level) {
        StatusLevel.GREEN -> StatusGreenContainer
        StatusLevel.YELLOW -> StatusYellowContainer
        StatusLevel.RED, StatusLevel.EXPIRED -> StatusRedContainer
        StatusLevel.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(color = containerColor, shape = MaterialTheme.shapes.extraSmall) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = dotColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ColorDot(colorName: String) {
    val dotColor = carColorToComposeColor(colorName) ?: return
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(2.dp)
            .background(dotColor, CircleShape)
    )
}
