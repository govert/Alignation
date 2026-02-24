package com.alignation.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alignation.ui.theme.BudgetComfortable
import com.alignation.ui.theme.BudgetProblem
import com.alignation.ui.theme.BudgetWarning
import com.alignation.ui.theme.StatusGreen
import com.alignation.ui.theme.StatusRed
import com.alignation.ui.theme.StreakFire
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stats summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Avg Out",
                value = formatDurationShort(uiState.averageTimeOut),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Streak",
                value = "${uiState.currentStreak}",
                valueColor = if (uiState.currentStreak > 0) StreakFire else null,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Problem",
                value = "${uiState.problemDays}",
                valueColor = if (uiState.problemDays > 0) BudgetProblem else BudgetComfortable,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab row
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal
        ) {
            DashboardTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab content
        when (uiState.selectedTab) {
            DashboardTab.DAILY -> DailyView(uiState)
            DashboardTab.WEEKLY -> WeeklyView(uiState)
            DashboardTab.MONTHLY -> MonthlyView(uiState)
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor ?: MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DailyView(uiState: DashboardUiState) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        // Today's summary
        uiState.todayStats?.let { stats ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Today's Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusIndicator(status = stats.status)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stats.timeOut.toMinutes()}min",
                            style = MaterialTheme.typography.headlineSmall,
                            color = stats.status.toColor()
                        )
                        Text(
                            text = " of ${stats.dailyAllowanceMinutes}min allowance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (stats.timeOut.toMinutes() > stats.dailyAllowanceMinutes) {
                        Text(
                            text = "Over budget by ${stats.timeOut.toMinutes() - stats.dailyAllowanceMinutes}min",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (stats.status == DayStatus.PROBLEM) BudgetProblem else BudgetWarning
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timeline
        Text(
            text = "Timeline",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        TimelineChart(segments = uiState.dailyTimeline)
    }
}

@Composable
private fun TimelineChart(segments: List<TimelineSegment>) {
    if (segments.isEmpty()) {
        Text(
            text = "No data for today",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Hour labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("12a", "6a", "12p", "6p", "12a").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timeline bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val totalMinutes = 24 * 60f

                segments.forEach { segment ->
                    val startMinute = segment.startTime
                        .atZone(ZoneId.systemDefault())
                        .let { it.hour * 60 + it.minute }
                    val endMinute = segment.endTime
                        .atZone(ZoneId.systemDefault())
                        .let { it.hour * 60 + it.minute }
                        .let { if (it == 0 && segment.endTime > segment.startTime) 24 * 60 else it }

                    val startX = (startMinute / totalMinutes) * size.width
                    val endX = (endMinute / totalMinutes) * size.width

                    drawRect(
                        color = if (segment.isWearing) StatusGreen else StatusRed.copy(alpha = 0.3f),
                        topLeft = Offset(startX, 0f),
                        size = Size(endX - startX, size.height)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(StatusGreen, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wearing", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(StatusRed.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Out", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun WeeklyView(uiState: DashboardUiState) {
    val maxAllowance = uiState.settings?.maxAllowanceMinutes ?: 180

    Column {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Last 7 Days - Time Out",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Bar chart showing time OUT (higher = worse)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    uiState.weekStats.forEach { stats ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Bar - scaled to max allowance (3h)
                            val heightFraction = (stats.timeOut.toMinutes().toFloat() / maxAllowance).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height((120 * heightFraction).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(stats.status.toColor())
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Day label
                            Text(
                                text = stats.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Legend
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BudgetComfortable, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("<2h", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BudgetWarning, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("2-3h", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BudgetProblem, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(">3h", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun MonthlyView(uiState: DashboardUiState) {
    val today = LocalDate.now()
    val treatmentStart = uiState.settings?.treatmentStartDate
    val yearMonth = YearMonth.from(today)
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Sunday

    Column {
        Text(
            text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Day of week headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        val totalCells = startDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        Column {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayOfMonth = cellIndex - startDayOfWeek + 1

                        if (dayOfMonth in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayOfMonth)
                            // Don't show color overlays for days before treatment start
                            val isBeforeTreatment = treatmentStart != null && date.isBefore(treatmentStart)
                            val stats = if (isBeforeTreatment) null else uiState.monthStats[date]

                            CalendarDay(
                                day = dayOfMonth,
                                status = stats?.status ?: DayStatus.NO_DATA,
                                isToday = date == today,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    status: DayStatus,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primary
                    status != DayStatus.NO_DATA -> status.toColor().copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusIndicator(status: DayStatus) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(status.toColor())
    )
}

private fun DayStatus.toColor(): Color = when (this) {
    DayStatus.GOOD -> BudgetComfortable
    DayStatus.WARNING -> BudgetWarning
    DayStatus.PROBLEM -> BudgetProblem
    DayStatus.NO_DATA -> Color.Gray
}

private fun formatDurationShort(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    return "${hours}h ${minutes}m"
}

private fun formatDurationLong(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    return "${hours}h ${minutes}m"
}
