package com.alignation.ui.sharing

import android.content.Context
import android.content.Intent
import com.alignation.data.model.AlignmentEvent
import com.alignation.data.model.EventType
import com.alignation.data.model.UserSettings
import com.alignation.data.repository.AlignmentRepository
import com.alignation.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alignmentRepository: AlignmentRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun buildProgressSummary(): String {
        val settings = settingsRepository.getSettingsOnce() ?: return "No data available yet."
        val today = LocalDate.now()
        val daysElapsed = ChronoUnit.DAYS.between(settings.treatmentStartDate, today).toInt().coerceAtLeast(0)
        val totalDays = settings.treatmentWeeks * 7
        val weekNumber = (daysElapsed / 7) + 1

        return buildString {
            appendLine("Alignation Progress Update")
            appendLine("========================")
            appendLine("Week $weekNumber of ${settings.treatmentWeeks}")
            appendLine("Day $daysElapsed of $totalDays")
            appendLine()
            appendLine("Daily target: ${settings.dailyAllowanceMinutes} min out")
            appendLine("Max before problem: ${settings.maxAllowanceMinutes} min out")
        }
    }

    suspend fun exportCsv(): String {
        val events = alignmentRepository.getEventsForDateRangeOnce(
            LocalDate.of(2020, 1, 1),
            LocalDate.now().plusDays(1)
        )
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())

        return buildString {
            appendLine("id,timestamp,eventType,isManualEntry,notes")
            events.forEach { event ->
                appendLine("${event.id},${formatter.format(event.timestamp)},${event.eventType},${event.isManualEntry},${event.notes ?: ""}")
            }
        }
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
