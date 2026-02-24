package com.alignation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alignation.data.model.AuditLogEntry
import com.alignation.data.repository.AlignmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    repository: AlignmentRepository
) : ViewModel() {

    val auditLog: StateFlow<List<AuditLogEntry>> = repository.getRecentAuditLog(200)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
