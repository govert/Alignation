package com.alignation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alignation.data.model.AlignerSet
import com.alignation.data.model.UserSettings
import com.alignation.data.repository.AlignmentRepository
import com.alignation.data.repository.SettingsRepository
import com.alignation.ui.sharing.ShareHelper
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
    val reminderDelayMinutes: Int = 30,
    val setHistory: List<AlignerSet> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val alignmentRepository: AlignmentRepository,
    private val shareHelper: ShareHelper
) : ViewModel() {

    private val _showDatePicker = MutableStateFlow(false)
    private val _notificationsEnabled = MutableStateFlow(true)
    private val _reminderDelayMinutes = MutableStateFlow(30)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getSettings(),
        _showDatePicker,
        _notificationsEnabled,
        alignmentRepository.getAllSets()
    ) { settings, showDatePicker, notificationsEnabled, setHistory ->
        SettingsUiState(
            settings = settings,
            isLoading = false,
            showDatePicker = showDatePicker,
            notificationsEnabled = notificationsEnabled,
            reminderDelayMinutes = _reminderDelayMinutes.value,
            setHistory = setHistory
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

    fun updateDailyAllowance(minutes: Int) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsOnce() ?: return@launch
            settingsRepository.updateSettings(current.copy(dailyAllowanceMinutes = minutes))
        }
    }

    fun updateMaxAllowance(minutes: Int) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsOnce() ?: return@launch
            settingsRepository.updateSettings(current.copy(maxAllowanceMinutes = minutes))
        }
    }

    fun updateMaxGrace(minutes: Int) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsOnce() ?: return@launch
            settingsRepository.updateSettings(current.copy(maxGraceMinutes = minutes))
        }
    }

    fun updateEnableGraceTime(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsOnce() ?: return@launch
            settingsRepository.updateSettings(current.copy(enableGraceTime = enabled))
        }
    }

    fun shareProgress() {
        viewModelScope.launch {
            val summary = shareHelper.buildProgressSummary()
            shareHelper.shareText(summary)
        }
    }

    fun exportData() {
        viewModelScope.launch {
            val csv = shareHelper.exportCsv()
            shareHelper.shareText(csv)
        }
    }

    fun updateAlarmToggle(
        alarm30m: Boolean? = null,
        alarm1h: Boolean? = null,
        alarm15mSoft: Boolean? = null,
        alarm15mHard: Boolean? = null,
        alarm5mHard: Boolean? = null
    ) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsOnce() ?: return@launch
            settingsRepository.updateSettings(
                current.copy(
                    enableAlarm30m = alarm30m ?: current.enableAlarm30m,
                    enableAlarm1h = alarm1h ?: current.enableAlarm1h,
                    enableAlarm15mBeforeSoft = alarm15mSoft ?: current.enableAlarm15mBeforeSoft,
                    enableAlarm15mBeforeHard = alarm15mHard ?: current.enableAlarm15mBeforeHard,
                    enableAlarm5mBeforeHard = alarm5mHard ?: current.enableAlarm5mBeforeHard
                )
            )
        }
    }
}
