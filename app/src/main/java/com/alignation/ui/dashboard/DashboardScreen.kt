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
import androidx.compose.runtime.remember
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
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
            DashboardTab.DAILY -> DailyView(uiState, onSelectDate = viewModel::selectDate)
            DashboardTab.WEEKLY -> WeeklyView(uiState, onDrillIntoDate = viewModel::drillIntoDate)
            DashboardTab.MONTHLY -> MonthlyView(uiState, onDrillIntoDate = viewModel::drillIntoDate)
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
private fun DailyView(
    uiState: DashboardUiState,
    onSelectDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val selectedStats = uiState.allDayStats[uiState.selectedDate]
    val recentDays = (6 downTo 0).map { today.minusDays(it.toLong()) }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Recent Days",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            recentDays.forEach { date ->
                val isToday = date == today
                val isSelected = date == uiState.selectedDate
                val dayStats = uiState.allDayStats[date]

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectDate(date) }
                        .background(
                            when {
                                isToday -> MaterialTheme.colorScheme.primary
                                isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                else -> Color.Transparent
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dayStats?.status?.toColor()?.copy(alpha = if (isToday) 1f else 0.2f) ?: Color.Transparent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedStats?.let { stats ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (uiState.selectedDate == today) "Today's Budget" else "${uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))} Budget",
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
        } ?: Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No data for ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
private fun WeeklyView(
    uiState: DashboardUiState,
    onDrillIntoDate: (LocalDate) -> Unit
) {
    val maxAllowance = uiState.settings?.maxAllowanceMinutes ?: 180
    val today = LocalDate.now()
    val treatmentStart = uiState.settings?.treatmentStartDate ?: today
    val weekStarts = remember(treatmentStart, today) {
        buildWeekStarts(treatmentStart, today)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        weekStarts.forEach { weekStart ->
            val days = (0..6).map { weekStart.plusDays(it.toLong()) }
            val isCurrentWeek = !today.isBefore(weekStart) && !today.isAfter(weekStart.plusDays(6))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentWeek) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Week of ${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentWeek) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEach { date ->
                            val stats = uiState.allDayStats[date]
                            val heightFraction = ((stats?.timeOut?.toMinutes() ?: 0L).toFloat() / maxAllowance).coerceIn(0f, 1f)
                            val isFuture = date.isAfter(today)
                            val canOpen = !date.isAfter(today) && !date.isBefore(treatmentStart)
                            val barAlpha = when {
                                isFuture -> 0.12f
                                isCurrentWeek -> 0.9f
                                else -> 0.3f
                            }
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = canOpen) { onDrillIntoDate(date) }
                                    .padding(horizontal = 2.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height((120 * heightFraction).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background((stats?.status ?: DayStatus.NO_DATA).toColor().copy(alpha = barAlpha))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (date == today) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCurrentWeek) 0.8f else 0.55f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MonthlyView(
    uiState: DashboardUiState,
    onDrillIntoDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val treatmentStart = uiState.settings?.treatmentStartDate ?: today
    val months = remember(treatmentStart, today) {
        buildMonthList(treatmentStart, today)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        months.forEach { yearMonth ->
            val isCurrentMonth = yearMonth == YearMonth.from(today)
            val firstDayOfMonth = yearMonth.atDay(1)
            val daysInMonth = yearMonth.lengthOfMonth()
            val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
            val totalCells = startDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            Text(
                text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            val isBeforeTreatment = date.isBefore(treatmentStart)
                            val stats = if (isBeforeTreatment) null else uiState.allDayStats[date]
                            val canOpen = !date.isAfter(today) && !isBeforeTreatment

                            CalendarDay(
                                day = dayOfMonth,
                                status = stats?.status ?: DayStatus.NO_DATA,
                                isToday = date == today,
                                isSelected = date == uiState.selectedDate,
                                onClick = { onDrillIntoDate(date) },
                                enabled = canOpen,
                                toneDown = !isCurrentMonth,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    status: DayStatus,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    toneDown: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primary
                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                    status != DayStatus.NO_DATA -> status.toColor().copy(alpha = if (toneDown) 0.16f else 0.3f)
                    else -> Color.Transparent
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = when {
                isToday -> MaterialTheme.colorScheme.onPrimary
                isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                toneDown -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.onSurface
            }
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

private fun buildWeekStarts(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    var cursor = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val earliestWeek = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weeks = mutableListOf<LocalDate>()

    while (!cursor.isBefore(earliestWeek)) {
        weeks.add(cursor)
        cursor = cursor.minusWeeks(1)
    }

    return weeks
}

private fun buildMonthList(startDate: LocalDate, endDate: LocalDate): List<YearMonth> {
    var cursor = YearMonth.from(endDate)
    val first = YearMonth.from(startDate)
    val months = mutableListOf<YearMonth>()

    while (!cursor.isBefore(first)) {
        months.add(cursor)
        cursor = cursor.minusMonths(1)
    }

    return months
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
