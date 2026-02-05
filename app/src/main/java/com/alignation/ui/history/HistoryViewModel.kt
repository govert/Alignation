package com.alignation.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alignation.data.model.AlignmentEvent
import com.alignation.data.model.EventType
import com.alignation.data.repository.AlignmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class HistoryUiState(
    val events: List<AlignmentEvent> = emptyList(),
    val isLoading: Boolean = true,
    val showManualEntryDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingEvent: AlignmentEvent? = null,
    val manualEntryType: EventType = EventType.REMOVED
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: AlignmentRepository
) : ViewModel() {

    private val _showManualEntryDialog = MutableStateFlow(false)
    private val _showEditDialog = MutableStateFlow(false)
    private val _editingEvent = MutableStateFlow<AlignmentEvent?>(null)
    private val _manualEntryType = MutableStateFlow(EventType.REMOVED)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getAllEvents(),
        _showManualEntryDialog,
        _showEditDialog,
        _editingEvent,
        _manualEntryType
    ) { events, showManual, showEdit, editing, entryType ->
        HistoryUiState(
            events = events,
            isLoading = false,
            showManualEntryDialog = showManual,
            showEditDialog = showEdit,
            editingEvent = editing,
            manualEntryType = entryType
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun showManualEntryDialog(type: EventType) {
        _manualEntryType.value = type
        _showManualEntryDialog.value = true
    }

    fun hideManualEntryDialog() {
        _showManualEntryDialog.value = false
    }

    fun showEditDialog(event: AlignmentEvent) {
        _editingEvent.value = event
        _showEditDialog.value = true
    }

    fun hideEditDialog() {
        _showEditDialog.value = false
        _editingEvent.value = null
    }

    fun addManualEntry(date: LocalDate, time: LocalTime, type: EventType, notes: String?) {
        viewModelScope.launch {
            val dateTime = LocalDateTime.of(date, time)
            val instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()

            when (type) {
                EventType.REMOVED -> repository.logRemoval(instant, isManual = true, notes = notes)
                EventType.REPLACED -> repository.logReplacement(instant, isManual = true, notes = notes)
            }

            _showManualEntryDialog.value = false
        }
    }

    fun updateEvent(event: AlignmentEvent, newDate: LocalDate, newTime: LocalTime, newNotes: String?) {
        viewModelScope.launch {
            val dateTime = LocalDateTime.of(newDate, newTime)
            val instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()

            val updatedEvent = event.copy(
                timestamp = instant,
                notes = newNotes
            )
            repository.updateEvent(updatedEvent)

            _showEditDialog.value = false
            _editingEvent.value = null
        }
    }

    fun deleteEvent(event: AlignmentEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            _showEditDialog.value = false
            _editingEvent.value = null
        }
    }
}
