package com.alignation.ui.photos

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alignation.data.database.PhotoSetDao
import com.alignation.data.model.AlignerSet
import com.alignation.data.model.PhotoSet
import com.alignation.data.repository.AlignmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import javax.inject.Inject

data class PhotoCaptureUiState(
    val currentSetNumber: Int = 1,
    val frontPhotoUri: Uri? = null,
    val leftPhotoUri: Uri? = null,
    val rightPhotoUri: Uri? = null,
    val pastPhotoSets: List<PhotoSet> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class PhotoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoSetDao: PhotoSetDao,
    private val alignmentRepository: AlignmentRepository
) : ViewModel() {

    private val _frontPhotoUri = MutableStateFlow<Uri?>(null)
    private val _leftPhotoUri = MutableStateFlow<Uri?>(null)
    private val _rightPhotoUri = MutableStateFlow<Uri?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _saved = MutableStateFlow(false)

    val uiState: StateFlow<PhotoCaptureUiState> = combine(
        alignmentRepository.getCurrentSet(),
        photoSetDao.getAllPhotoSets(),
        _frontPhotoUri,
        _leftPhotoUri,
        combine(_rightPhotoUri, _isSaving, _saved) { r, s, d -> Triple(r, s, d) }
    ) { flows ->
        val currentSet = flows[0] as AlignerSet?
        val pastSets = @Suppress("UNCHECKED_CAST") (flows[1] as List<PhotoSet>)
        val front = flows[2] as Uri?
        val left = flows[3] as Uri?
        val (right, saving, saved) = @Suppress("UNCHECKED_CAST") (flows[4] as Triple<Uri?, Boolean, Boolean>)

        PhotoCaptureUiState(
            currentSetNumber = currentSet?.setNumber ?: 1,
            frontPhotoUri = front,
            leftPhotoUri = left,
            rightPhotoUri = right,
            pastPhotoSets = pastSets,
            isSaving = saving,
            saved = saved
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PhotoCaptureUiState()
    )

    fun setFrontPhoto(uri: Uri) { _frontPhotoUri.value = uri }
    fun setLeftPhoto(uri: Uri) { _leftPhotoUri.value = uri }
    fun setRightPhoto(uri: Uri) { _rightPhotoUri.value = uri }

    fun createPhotoFile(prefix: String): File {
        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        return File.createTempFile("${prefix}_", ".jpg", photosDir)
    }

    fun savePhotoSet() {
        viewModelScope.launch {
            _isSaving.value = true
            val currentSet = alignmentRepository.getCurrentSetOnce()
            val photoSet = PhotoSet(
                takenAt = Instant.now(),
                alignerSetNumber = currentSet?.setNumber ?: 1,
                frontPhotoPath = _frontPhotoUri.value?.toString(),
                leftPhotoPath = _leftPhotoUri.value?.toString(),
                rightPhotoPath = _rightPhotoUri.value?.toString()
            )
            photoSetDao.insert(photoSet)
            _isSaving.value = false
            _saved.value = true
        }
    }

    fun resetForNewSet() {
        _frontPhotoUri.value = null
        _leftPhotoUri.value = null
        _rightPhotoUri.value = null
        _saved.value = false
    }
}
