package com.alignation.ui.feedback

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class FeedbackUiState(
    val feedbackText: String = "",
    val category: FeedbackCategory = FeedbackCategory.GENERAL,
    val isSending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null
)

enum class FeedbackCategory(val label: String) {
    BUG("Bug Report"),
    FEATURE("Feature Request"),
    GENERAL("General Feedback")
}

@HiltViewModel
class FeedbackViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun updateFeedbackText(text: String) {
        _uiState.value = _uiState.value.copy(feedbackText = text)
    }

    fun updateCategory(category: FeedbackCategory) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun submitFeedback() {
        val current = _uiState.value
        if (current.feedbackText.isBlank()) {
            _uiState.value = current.copy(error = "Please enter some feedback")
            return
        }

        _uiState.value = current.copy(isSending = true, error = null)

        // TODO: Send to backend when API is available
        // For now, mark as sent (stub)
        _uiState.value = current.copy(isSending = false, sent = true)
    }

    fun resetForm() {
        _uiState.value = FeedbackUiState()
    }
}
