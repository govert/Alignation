package com.alignation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alignation.data.model.UserSettings
import com.alignation.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings? = null,
    val isLoading: Boolean = true,
    val showDatePicker: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val reminderDelayMinutes: Int = 30
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _showDatePicker = MutableStateFlow(false)
    private val _notificationsEnabled = MutableStateFlow(true)
    private val _reminderDelayMinutes = MutableStateFlow(30)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getSettings(),
        _showDatePicker,
        _notificationsEnabled,
        _reminderDelayMinutes
    ) { settings, showDatePicker, notificationsEnabled, reminderDelay ->
        SettingsUiState(
            settings = settings,
            isLoading = false,
            showDatePicker = showDatePicker,
            notificationsEnabled = notificationsEnabled,
            reminderDelayMinutes = reminderDelay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun showDatePicker() {
        _showDatePicker.value = true
    }

    fun hideDatePicker() {
        _showDatePicker.value = false
    }

    fun setTreatmentStartDate(date: LocalDate) {
        viewModelScope.launch {
            settingsRepository.updateStartDate(date)
            _showDatePicker.value = false
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun setReminderDelay(minutes: Int) {
        _reminderDelayMinutes.value = minutes
    }
}
