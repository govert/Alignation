package com.alignation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alignation.data.model.AlignmentEvent
import com.alignation.data.model.EventType
import com.alignation.data.model.UserSettings
import com.alignation.data.repository.AlignmentRepository
import com.alignation.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

enum class DashboardTab {
    DAILY, WEEKLY, MONTHLY
}

data class DayStats(
    val date: LocalDate,
    val timeOut: Duration,
    val dailyAllowanceMinutes: Int = 120,
    val maxAllowanceMinutes: Int = 180
) {
    val budgetProgress: Float get() = timeOut.toMinutes().toFloat() / dailyAllowanceMinutes.toFloat()
    val status: DayStatus get() = when {
        timeOut.toMinutes() > maxAllowanceMinutes -> DayStatus.PROBLEM      // >3h = problem day
        timeOut.toMinutes() > dailyAllowanceMinutes -> DayStatus.WARNING    // 2-3h = warning
        else -> DayStatus.GOOD                                               // <2h = good
    }
}

enum class DayStatus {
    GOOD, WARNING, PROBLEM, NO_DATA
}

data class DashboardUiState(
    val selectedTab: DashboardTab = DashboardTab.DAILY,
    val selectedDate: LocalDate = LocalDate.now(),
    val todayStats: DayStats? = null,
    val allDayStats: Map<LocalDate, DayStats> = emptyMap(),
    val weekStats: List<DayStats> = emptyList(),
    val monthStats: Map<LocalDate, DayStats> = emptyMap(),
    val averageTimeOut: Duration = Duration.ZERO,
    val problemDays: Int = 0,
    val currentStreak: Int = 0,
    val totalDaysTracked: Int = 0,
    val settings: UserSettings? = null,
    val isLoading: Boolean = true,
    val dailyTimeline: List<TimelineSegment> = emptyList()
)

data class TimelineSegment(
    val startTime: Instant,
    val endTime: Instant,
    val isWearing: Boolean
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AlignmentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun selectTab(tab: DashboardTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadDailyTimeline(date)
    }

    fun drillIntoDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            selectedTab = DashboardTab.DAILY
        )
        loadDailyTimeline(date)
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val settings = settingsRepository.getSettings().first()

            val today = LocalDate.now()
            val monthStart = YearMonth.from(today).atDay(1)

            // Determine starting date for calculations
            val treatmentStart = settings?.treatmentStartDate ?: today

            // Load today's stats
            val todayEvents = repository.getEventsForDateOnce(today)
            val yesterdayEvents = repository.getEventsForDateOnce(today.minusDays(1))
            val wasOutAtMidnight = yesterdayEvents.lastOrNull()?.eventType == EventType.REMOVED
            val todayStats = calculateDayStats(today, todayEvents, wasOutAtMidnight, settings)

            // Load week stats (last 7 days)
            val weekStats = mutableListOf<DayStats>()
            for (daysAgo in 6 downTo 0) {
                val date = today.minusDays(daysAgo.toLong())
                val events = repository.getEventsForDateOnce(date)
                val prevDayEvents = repository.getEventsForDateOnce(date.minusDays(1))
                val wasOut = prevDayEvents.lastOrNull()?.eventType == EventType.REMOVED
                weekStats.add(calculateDayStats(date, events, wasOut, settings))
            }

            val allDayStats = mutableMapOf<LocalDate, DayStats>()
            var currentDate = treatmentStart
            while (!currentDate.isAfter(today)) {
                val events = repository.getEventsForDateOnce(currentDate)
                val prevDayEvents = repository.getEventsForDateOnce(currentDate.minusDays(1))
                val wasOut = prevDayEvents.lastOrNull()?.eventType == EventType.REMOVED
                allDayStats[currentDate] = calculateDayStats(currentDate, events, wasOut, settings)
                currentDate = currentDate.plusDays(1)
            }

            val monthStats = allDayStats.filterKeys { !it.isBefore(monthStart) }

            // Calculate averages and stats since treatment start
            val validDays = weekStats.filter { it.timeOut > Duration.ZERO || it.date >= treatmentStart }
            val averageTimeOut = if (validDays.isNotEmpty()) {
                Duration.ofMinutes(validDays.map { it.timeOut.toMinutes() }.average().toLong())
            } else Duration.ZERO

            // Calculate streak and problem days from treatment start
            var streak = 0
            var problemDays = 0
            var totalDays = 0
            var streakBroken = false
            var checkDate = today.minusDays(1) // Start from yesterday

            while (!checkDate.isBefore(treatmentStart)) {
                val events = repository.getEventsForDateOnce(checkDate)
                val prevDayEvents = repository.getEventsForDateOnce(checkDate.minusDays(1))
                val wasOut = prevDayEvents.lastOrNull()?.eventType == EventType.REMOVED
                val dayStats = calculateDayStats(checkDate, events, wasOut, settings)

                totalDays++

                if (dayStats.status == DayStatus.PROBLEM) {
                    problemDays++
                    streakBroken = true
                } else if (!streakBroken && events.isNotEmpty()) {
                    streak++
                }

                checkDate = checkDate.minusDays(1)
            }

            // Load daily timeline
            val timeline = buildTimeline(todayEvents, today)

            _uiState.value = DashboardUiState(
                selectedTab = _uiState.value.selectedTab,
                selectedDate = today,
                todayStats = todayStats,
                allDayStats = allDayStats,
                weekStats = weekStats,
                monthStats = monthStats,
                averageTimeOut = averageTimeOut,
                problemDays = problemDays,
                currentStreak = streak,
                totalDaysTracked = totalDays,
                settings = settings,
                isLoading = false,
                dailyTimeline = timeline
            )
        }
    }

    private fun loadDailyTimeline(date: LocalDate) {
        viewModelScope.launch {
            val events = repository.getEventsForDateOnce(date)
            val timeline = buildTimeline(events, date)
            _uiState.value = _uiState.value.copy(dailyTimeline = timeline)
        }
    }

    private fun calculateDayStats(
        date: LocalDate,
        events: List<AlignmentEvent>,
        wasOutAtMidnight: Boolean,
        settings: UserSettings?
    ): DayStats {
        val timeOut = calculateTimeOutForDay(date, events, wasOutAtMidnight)
        return DayStats(
            date = date,
            timeOut = timeOut,
            dailyAllowanceMinutes = settings?.dailyAllowanceMinutes ?: 120,
            maxAllowanceMinutes = settings?.maxAllowanceMinutes ?: 180
        )
    }

    private fun calculateTimeOutForDay(
        date: LocalDate,
        events: List<AlignmentEvent>,
        wasOutAtMidnight: Boolean
    ): Duration {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        val effectiveEnd = if (date == LocalDate.now() && now.isBefore(endOfDay)) now else endOfDay

        if (events.isEmpty()) {
            // No events this day - if was out at midnight, count time until end
            return if (wasOutAtMidnight) {
                Duration.between(startOfDay, effectiveEnd)
            } else {
                Duration.ZERO
            }
        }

        var totalTimeOut = Duration.ZERO

        // Handle time from midnight to first event
        val firstEvent = events.first()
        if (wasOutAtMidnight && firstEvent.eventType == EventType.REPLACED) {
            // Was out at midnight, then put back in - count that time
            totalTimeOut += Duration.between(startOfDay, firstEvent.timestamp)
        }

        // Process pairs: REMOVED -> REPLACED = time out
        for (i in events.indices) {
            if (i > 0) {
                val prevEvent = events[i - 1]
                val currentEvent = events[i]
                if (prevEvent.eventType == EventType.REMOVED) {
                    totalTimeOut += Duration.between(prevEvent.timestamp, currentEvent.timestamp)
                }
            }
        }

        // If last event is REMOVED (currently out), add time to end
        val lastEvent = events.last()
        if (lastEvent.eventType == EventType.REMOVED) {
            totalTimeOut += Duration.between(lastEvent.timestamp, effectiveEnd)
        }

        return totalTimeOut
    }

    private fun buildTimeline(events: List<AlignmentEvent>, date: LocalDate): List<TimelineSegment> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        val effectiveEnd = if (date == LocalDate.now() && now.isBefore(endOfDay)) now else endOfDay

        if (events.isEmpty()) {
            return emptyList()
        }

        val segments = mutableListOf<TimelineSegment>()

        // First segment from midnight
        val firstEvent = events.first()
        val wasInAtStart = firstEvent.eventType == EventType.REMOVED
        segments.add(TimelineSegment(startOfDay, firstEvent.timestamp, wasInAtStart))

        // Middle segments
        for (i in 0 until events.size - 1) {
            val current = events[i]
            val next = events[i + 1]
            val isWearing = current.eventType == EventType.REPLACED
            segments.add(TimelineSegment(current.timestamp, next.timestamp, isWearing))
        }

        // Last segment to end of day
        val lastEvent = events.last()
        val isWearingNow = lastEvent.eventType == EventType.REPLACED
        segments.add(TimelineSegment(lastEvent.timestamp, effectiveEnd, isWearingNow))

        return segments
    }
}
