package com.hananelsabag.autocare.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.presentation.theme.StatusGreen
import com.hananelsabag.autocare.presentation.theme.StatusRed
import com.hananelsabag.autocare.presentation.theme.StatusYellow
import com.hananelsabag.autocare.util.StatusLevel
import com.hananelsabag.autocare.util.daysFromNow
import com.hananelsabag.autocare.util.getStatusLevel

@Composable
fun rememberCarPagerState(carCount: Int): PagerState =
    rememberPagerState(pageCount = { carCount + 1 })

@Composable
fun CarPager(
    cars: List<Car>,
    nextServiceDueMsByCarId: Map<Int, Long?>,
    onCarClick: (Int) -> Unit,
    onAddCar: () -> Unit,
    onCarLongPress: (Car) -> Unit,
    pagerState: PagerState = rememberCarPagerState(cars.size),
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val isCurrentPage = pagerState.currentPage == page
            val scale by animateFloatAsState(
                targetValue = if (isCurrentPage) 1f else 0.93f,
                animationSpec = tween(250),
                label = "scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isCurrentPage) 1f else 0.55f,
                animationSpec = tween(250),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .padding(vertical = 10.dp)
            ) {
                if (page < cars.size) {
                    CarPagerCard(
                        car = cars[page],
                        nextServiceDueMs = nextServiceDueMsByCarId[cars[page].id],
                        onClick = { onCarClick(cars[page].id) },
                        onLongClick = { onCarLongPress(cars[page]) }
                    )
                } else {
                    AddCarPagerCard(onClick = onAddCar)
                }
            }
        }

        // Dots indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { index ->
                val isSelected = pagerState.currentPage == index
                val dotWidth by animateFloatAsState(
                    targetValue = if (isSelected) 20f else 7f,
                    animationSpec = tween(250),
                    label = "dot"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(7.dp)
                        .width(dotWidth.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CarPagerCard(car: Car, nextServiceDueMs: Long?, onClick: () -> Unit, onLongClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Hero photo (~44% of card) ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.44f)
            ) {
                if (car.photoUri != null) {
                    AsyncImage(
                        model = car.photoUri,
                        contentDescription = stringResource(R.string.content_description_car_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }

                // Gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f)
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                            )
                        )
                )

                // Car name overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "${car.make} ${car.model}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = car.year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

            }

            // ── Details section (~56% of card) ────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.56f)
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // License plate + KM row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IsraeliLicensePlate(plate = car.licensePlate)
                    if (car.currentKm != null) {
                        Text(
                            text = stringResource(R.string.car_card_km_format, car.currentKm.toString()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ── Status progress bars ──────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    car.testExpiryDate?.let { expiry ->
                        StatusProgressRow(
                            label = stringResource(R.string.car_pager_test_label),
                            expiryMs = expiry
                        )
                    }
                    car.insuranceExpiryDate?.let { expiry ->
                        StatusProgressRow(
                            label = stringResource(R.string.car_pager_insurance_label),
                            expiryMs = expiry
                        )
                    }
                    nextServiceDueMs?.let { expiry ->
                        StatusProgressRow(
                            label = stringResource(R.string.car_pager_service_label),
                            expiryMs = expiry
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusProgressRow(label: String, expiryMs: Long) {
    val days = expiryMs.daysFromNow()
    val level = getStatusLevel(expiryMs)

    val barColor = when (level) {
        StatusLevel.GREEN -> StatusGreen
        StatusLevel.YELLOW -> StatusYellow
        StatusLevel.RED, StatusLevel.EXPIRED -> StatusRed
        StatusLevel.UNKNOWN -> MaterialTheme.colorScheme.outlineVariant
    }

    // Progress: 365 days = 100%, clamped 0..1
    val progress = (days.coerceIn(0, 365) / 365f)

    val daysLabel = when {
        days < 0 -> stringResource(R.string.car_pager_expired)
        days == 0L -> stringResource(R.string.car_pager_today)
        else -> stringResource(R.string.car_pager_days_left, days)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(50)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
        Text(
            text = daysLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = barColor,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

@Composable
private fun AddCarPagerCard(onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = stringResource(R.string.car_pager_add_card_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.car_pager_add_card_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

