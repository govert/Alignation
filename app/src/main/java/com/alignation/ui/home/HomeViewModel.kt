package com.alignation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alignation.data.model.AlignmentEvent
import com.alignation.data.model.EventType
import com.alignation.data.model.UserSettings
import com.alignation.data.repository.AlignmentRepository
import com.alignation.data.repository.SettingsRepository
import com.alignation.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class BudgetStatus {
    COMFORTABLE,      // <1h used
    GETTING_THERE,    // 1-1.5h used
    APPROACHING,      // 1.5-2h used
    WARNING,          // 2-2.5h used (over target)
    DANGER,           // 2.5-3h used (streak at risk)
    PROBLEM           // >3h used (problem day)
}

data class HomeUiState(
    val isAlignerIn: Boolean = true,
    val currentSessionDuration: Duration = Duration.ZERO,

    // Budget view
    val todayTimeOut: Duration = Duration.ZERO,
    val dailyAllowanceMinutes: Int = 120,
    val maxAllowanceMinutes: Int = 180,
    val graceMinutes: Int = 0,
    val budgetStatus: BudgetStatus = BudgetStatus.COMFORTABLE,

    // Treatment progress
    val settings: UserSettings? = null,
    val weekNumber: Int = 1,
    val totalWeeks: Int = 16,
    val daysRemaining: Int = 112,
    val treatmentProgress: Float = 0f,

    // Streak & stats
    val currentStreak: Int = 0,
    val problemDays: Int = 0,
    val totalDaysTracked: Int = 0,

    val lastEvent: AlignmentEvent? = null,
    val isLoading: Boolean = true,
    val needsSetup: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AlignmentRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _currentTime = MutableStateFlow(Instant.now())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getLatestEvent(),
        repository.getEventsForToday(),
        settingsRepository.getSettings(),
        _currentTime
    ) { latestEvent, todayEvents, settings, currentTime ->

        if (settings == null) {
            return@combine HomeUiState(
                isLoading = false,
                needsSetup = true
            )
        }

        val isAlignerIn = latestEvent?.eventType != EventType.REMOVED
        val currentSessionDuration = if (latestEvent != null) {
            Duration.between(latestEvent.timestamp, currentTime)
        } else {
            Duration.ZERO
        }

        // Calculate time OUT today
        val todayTimeOut = calculateTimeOut(todayEvents, currentTime, isAlignerIn)

        // Calculate grace from yesterday
        val graceMinutes = calculateGraceFromYesterday(settings)

        // Determine budget status
        val effectiveAllowance = settings.dailyAllowanceMinutes + graceMinutes
        val budgetStatus = getBudgetStatus(todayTimeOut.toMinutes().toInt(), effectiveAllowance, settings.maxAllowanceMinutes)

        // Treatment progress
        val today = LocalDate.now()
        val daysElapsed = ChronoUnit.DAYS.between(settings.treatmentStartDate, today).toInt().coerceAtLeast(0)
        val totalDays = settings.treatmentWeeks * 7
        val daysRemaining = (totalDays - daysElapsed).coerceAtLeast(0)
        val weekNumber = (daysElapsed / 7) + 1
        val treatmentProgress = (daysElapsed.toFloat() / totalDays).coerceIn(0f, 1f)

        // Calculate streak and problem days
        val (streak, problemDays) = calculateStreakAndProblemDays(settings)

        HomeUiState(
            isAlignerIn = isAlignerIn,
            currentSessionDuration = currentSessionDuration,
            todayTimeOut = todayTimeOut,
            dailyAllowanceMinutes = settings.dailyAllowanceMinutes,
            maxAllowanceMinutes = settings.maxAllowanceMinutes,
            graceMinutes = graceMinutes,
            budgetStatus = budgetStatus,
            settings = settings,
            weekNumber = weekNumber.coerceIn(1, settings.treatmentWeeks),
            totalWeeks = settings.treatmentWeeks,
            daysRemaining = daysRemaining,
            treatmentProgress = treatmentProgress,
            currentStreak = streak,
            problemDays = problemDays,
            totalDaysTracked = daysElapsed,
            lastEvent = latestEvent,
            isLoading = false,
            needsSetup = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        // Update current time every second for live duration display
        viewModelScope.launch {
            while (isActive) {
                _currentTime.value = Instant.now()
                delay(1000)
            }
        }
    }

    fun onRemoveClicked() {
        viewModelScope.launch {
            repository.logRemoval()
            reminderScheduler.scheduleTimedReminders()
        }
    }

    fun onReplaceClicked() {
        viewModelScope.launch {
            repository.logReplacement()
            reminderScheduler.cancelAllReminders()
        }
    }

    private suspend fun calculateTimeOut(
        events: List<AlignmentEvent>,
        currentTime: Instant,
        isCurrentlyIn: Boolean
    ): Duration {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()

        // Determine state at midnight by looking at yesterday's last event
        val yesterday = today.minusDays(1)
        val yesterdayEvents = repository.getEventsForDateOnce(yesterday)
        val wasOutAtMidnight = yesterdayEvents.lastOrNull()?.eventType == EventType.REMOVED

        if (events.isEmpty()) {
            // No events today
            return if (wasOutAtMidnight) {
                // Aligners were out at midnight and still out (or put back in but no event recorded)
                if (!isCurrentlyIn) {
                    Duration.between(startOfDay, currentTime)
                } else {
                    // Were out at midnight but now in - this shouldn't happen without an event
                    // Assume they were put in at midnight (edge case)
                    Duration.ZERO
                }
            } else {
                // Aligners were in at midnight
                if (!isCurrentlyIn) {
                    // Now out but no event - shouldn't happen, but count from midnight
                    Duration.between(startOfDay, currentTime)
                } else {
                    // Were in at midnight, still in - no time out
                    Duration.ZERO
                }
            }
        }

        var totalTimeOut = Duration.ZERO

        // Handle time from midnight to first event
        val firstEvent = events.first()
        if (wasOutAtMidnight && firstEvent.eventType == EventType.REPLACED) {
            // Was out at midnight, then put back in - count that time
            totalTimeOut += Duration.between(startOfDay, firstEvent.timestamp)
        } else if (!wasOutAtMidnight && firstEvent.eventType == EventType.REPLACED) {
            // Was in at midnight, first event is REPLACED - ignore (duplicate/error)
        }
        // If first event is REMOVED, no time to add before it (aligners were in)

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

        // If last event is REMOVED (currently out), add time to now
        val lastEvent = events.last()
        if (lastEvent.eventType == EventType.REMOVED) {
            totalTimeOut += Duration.between(lastEvent.timestamp, currentTime)
        }

        return totalTimeOut
    }

    private suspend fun calculateGraceFromYesterday(settings: UserSettings): Int {
        val yesterday = LocalDate.now().minusDays(1)
        val yesterdayEvents = repository.getEventsForDateOnce(yesterday)

        if (yesterdayEvents.isEmpty()) return 0

        // Get the state at midnight (start of yesterday) to calculate full day
        val dayBeforeYesterday = yesterday.minusDays(1)
        val eventBeforeYesterday = repository.getEventsForDateOnce(dayBeforeYesterday).lastOrNull()
        val wasOutAtYesterdayMidnight = eventBeforeYesterday?.eventType == EventType.REMOVED

        val yesterdayTimeOut = calculateTimeOutForCompletedDay(yesterday, yesterdayEvents, wasOutAtYesterdayMidnight)

        // Grace = unused budget from yesterday (if under 2h target)
        // If I used only 1h 40m yesterday, I get 20 min grace (capped at maxGraceMinutes)
        val unusedBudget = settings.dailyAllowanceMinutes - yesterdayTimeOut.toMinutes().toInt()

        return if (unusedBudget > 0) {
            unusedBudget.coerceAtMost(settings.maxGraceMinutes)
        } else {
            0
        }
    }

    private fun calculateTimeOutForCompletedDay(
        date: LocalDate,
        events: List<AlignmentEvent>,
        wasOutAtMidnight: Boolean
    ): Duration {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        if (events.isEmpty()) {
            // No events this day - if was out at midnight, count whole day as out
            return if (wasOutAtMidnight) Duration.between(startOfDay, endOfDay) else Duration.ZERO
        }

        var totalTimeOut = Duration.ZERO

        // Handle time from midnight to first event
        val firstEvent = events.first()
        if (wasOutAtMidnight && firstEvent.eventType == EventType.REPLACED) {
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

        // Handle time from last event to end of day
        val lastEvent = events.last()
        if (lastEvent.eventType == EventType.REMOVED) {
            totalTimeOut += Duration.between(lastEvent.timestamp, endOfDay)
        }

        return totalTimeOut
    }

    private fun getBudgetStatus(minutesOut: Int, effectiveAllowance: Int, maxAllowance: Int): BudgetStatus {
        return when {
            minutesOut >= maxAllowance -> BudgetStatus.PROBLEM        // >3h = problem day
            minutesOut >= maxAllowance - 30 -> BudgetStatus.DANGER   // 2.5-3h
            minutesOut >= effectiveAllowance -> BudgetStatus.WARNING // 2-2.5h (over target)
            minutesOut >= 90 -> BudgetStatus.APPROACHING              // 1.5-2h
            minutesOut >= 60 -> BudgetStatus.GETTING_THERE            // 1-1.5h
            else -> BudgetStatus.COMFORTABLE                          // <1h
        }
    }

    private suspend fun calculateStreakAndProblemDays(settings: UserSettings): Pair<Int, Int> {
        val today = LocalDate.now()
        var streak = 0
        var problemDays = 0
        var currentDate = today.minusDays(1) // Start from yesterday
        var streakBroken = false

        // Go back through days since treatment started
        while (!currentDate.isBefore(settings.treatmentStartDate)) {
            val events = repository.getEventsForDateOnce(currentDate)

            // Get state at midnight by looking at previous day's last event
            val previousDay = currentDate.minusDays(1)
            val previousDayEvents = repository.getEventsForDateOnce(previousDay)
            val wasOutAtMidnight = previousDayEvents.lastOrNull()?.eventType == EventType.REMOVED

            val timeOut = calculateTimeOutForCompletedDay(currentDate, events, wasOutAtMidnight)

            val isProblemDay = timeOut.toMinutes() > settings.maxAllowanceMinutes

            if (isProblemDay) {
                problemDays++
                streakBroken = true
            } else if (!streakBroken && events.isNotEmpty()) {
                streak++
            }

            currentDate = currentDate.minusDays(1)
        }

        return Pair(streak, problemDays)
    }
}
