package com.alignation.ui.photos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.alignation.data.model.PhotoSet
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptureScreen(
    onBack: () -> Unit,
    viewModel: PhotoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Camera launch state
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoType by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPhotoUri != null) {
            when (pendingPhotoType) {
                "front" -> viewModel.setFrontPhoto(pendingPhotoUri!!)
                "left" -> viewModel.setLeftPhoto(pendingPhotoUri!!)
                "right" -> viewModel.setRightPhoto(pendingPhotoUri!!)
            }
        }
    }

    fun launchCamera(type: String) {
        val file = viewModel.createPhotoFile(type)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        pendingPhotoUri = uri
        pendingPhotoType = type
        cameraLauncher.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress Photos - Set #${uiState.currentSetNumber}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Capture section
            item {
                if (uiState.saved) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Photos saved!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.resetForNewSet() }) {
                                Text("Take More Photos")
                            }
                        }
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Take Photos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                PhotoButton(
                                    label = "Front",
                                    hasTaken = uiState.frontPhotoUri != null,
                                    onClick = { launchCamera("front") }
                                )
                                PhotoButton(
                                    label = "Left",
                                    hasTaken = uiState.leftPhotoUri != null,
                                    onClick = { launchCamera("left") }
                                )
                                PhotoButton(
                                    label = "Right",
                                    hasTaken = uiState.rightPhotoUri != null,
                                    onClick = { launchCamera("right") }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val hasAnyPhoto = uiState.frontPhotoUri != null ||
                                    uiState.leftPhotoUri != null ||
                                    uiState.rightPhotoUri != null

                            Button(
                                onClick = { viewModel.savePhotoSet() },
                                enabled = hasAnyPhoto && !uiState.isSaving,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (uiState.isSaving) "Saving..." else "Save Photo Set")
                            }
                        }
                    }
                }
            }

            // Past photo sets
            if (uiState.pastPhotoSets.isNotEmpty()) {
                item {
                    Text(
                        text = "Previous Photo Sets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.pastPhotoSets) { photoSet ->
                    PhotoSetCard(photoSet)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PhotoButton(
    label: String,
    hasTaken: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (hasTaken) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "$label taken",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Take $label photo"
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PhotoSetCard(photoSet: PhotoSet) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Set #${photoSet.alignerSetNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatter.format(photoSet.takenAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            val photoCount = listOfNotNull(
                photoSet.frontPhotoPath,
                photoSet.leftPhotoPath,
                photoSet.rightPhotoPath
            ).size
            Text(
                text = "$photoCount/3 photos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
