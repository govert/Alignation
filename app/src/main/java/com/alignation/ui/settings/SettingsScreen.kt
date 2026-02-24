package com.alignation.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.alignation.data.model.AlignerSet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAuditLog: () -> Unit = {},
    onNavigateToFeedback: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var notificationsEnabled by remember { mutableStateOf(true) }

    // Check notification permission
    val hasNotificationPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsEnabled = granted
    }

    LaunchedEffect(Unit) {
        notificationsEnabled = hasNotificationPermission
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Send Feedback at the top
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                NavigationRow(
                    title = "Send Feedback",
                    description = "Report issues or request features",
                    onClick = onNavigateToFeedback,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Treatment section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Treatment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Start date picker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showDatePicker() }
                    ) {
                        OutlinedTextField(
                            value = uiState.settings?.treatmentStartDate?.format(
                                DateTimeFormatter.ofPattern("MMM d, yyyy")
                            ) ?: "Not set",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Treatment Start Date") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    uiState.settings?.let { settings ->
                        Spacer(modifier = Modifier.height(8.dp))

                        val endDate = settings.treatmentStartDate.plusWeeks(settings.treatmentWeeks.toLong())
                        val today = LocalDate.now()
                        val daysElapsed = ChronoUnit.DAYS.between(settings.treatmentStartDate, today).toInt().coerceAtLeast(0)
                        val totalDays = settings.treatmentWeeks * 7
                        val daysRemaining = (totalDays - daysElapsed).coerceAtLeast(0)
                        val weeksElapsed = (daysElapsed / 7) + 1

                        Text(
                            text = "Week $weeksElapsed of ${settings.treatmentWeeks}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "$daysRemaining days remaining (ends ${endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Daily Budget section with sliders
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val settings = uiState.settings

                    // Daily allowance slider
                    Text(
                        text = "Target: ${settings?.dailyAllowanceMinutes ?: 120} minutes (${(settings?.dailyAllowanceMinutes ?: 120) / 60}h ${(settings?.dailyAllowanceMinutes ?: 120) % 60}m)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = (settings?.dailyAllowanceMinutes ?: 120).toFloat(),
                        onValueChange = { viewModel.updateDailyAllowance(it.roundToInt()) },
                        valueRange = 60f..180f,
                        steps = 11, // 10-minute increments
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Max allowance slider
                    Text(
                        text = "Problem day threshold: ${settings?.maxAllowanceMinutes ?: 180} minutes (${(settings?.maxAllowanceMinutes ?: 180) / 60}h ${(settings?.maxAllowanceMinutes ?: 180) % 60}m)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = (settings?.maxAllowanceMinutes ?: 180).toFloat(),
                        onValueChange = { viewModel.updateMaxAllowance(it.roundToInt()) },
                        valueRange = 120f..300f,
                        steps = 17, // 10-minute increments
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grace time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Grace time carry-over",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Max ${settings?.maxGraceMinutes ?: 30} minutes from previous day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings?.enableGraceTime ?: true,
                            onCheckedChange = { viewModel.updateEnableGraceTime(it) }
                        )
                    }

                    if (settings?.enableGraceTime == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Max grace: ${settings.maxGraceMinutes} minutes",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = settings.maxGraceMinutes.toFloat(),
                            onValueChange = { viewModel.updateMaxGrace(it.roundToInt()) },
                            valueRange = 10f..60f,
                            steps = 4, // 10-minute increments
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Alarm settings section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Alarms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Master notification toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reminder notifications",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Enable alarm system when aligners are out",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationsEnabled = enabled
                                }
                            }
                        )
                    }

                    if (notificationsEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        val settings = uiState.settings

                        // 1h alarm toggle
                        AlarmToggleRow(
                            title = "1 hour warning",
                            description = "Alert after 1 hour out",
                            enabled = settings?.enableAlarm1h ?: true,
                            onToggle = { viewModel.updateAlarmToggle(alarm1h = it) }
                        )

                        // 15min before soft limit
                        AlarmToggleRow(
                            title = "15 min before target",
                            description = "Warning before daily target (${(settings?.dailyAllowanceMinutes ?: 120) - 15} min)",
                            enabled = settings?.enableAlarm15mBeforeSoft ?: true,
                            onToggle = { viewModel.updateAlarmToggle(alarm15mSoft = it) }
                        )

                        // 15min before hard limit
                        AlarmToggleRow(
                            title = "15 min before problem",
                            description = "Warning before problem day threshold (${(settings?.maxAllowanceMinutes ?: 180) - 15} min)",
                            enabled = settings?.enableAlarm15mBeforeHard ?: true,
                            onToggle = { viewModel.updateAlarmToggle(alarm15mHard = it) }
                        )

                        // 5min before hard limit
                        AlarmToggleRow(
                            title = "5 min before problem",
                            description = "Urgent alarm before problem day (${(settings?.maxAllowanceMinutes ?: 180) - 5} min)",
                            enabled = settings?.enableAlarm5mBeforeHard ?: true,
                            onToggle = { viewModel.updateAlarmToggle(alarm5mHard = it) }
                        )
                    }
                }
            }
        }

        // Aligner Set History section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Aligner Set History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val setHistory = uiState.setHistory
                    if (setHistory.isEmpty()) {
                        Text(
                            text = "No set changes recorded yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Set history items
        if (uiState.setHistory.isNotEmpty()) {
            items(uiState.setHistory) { set ->
                SetHistoryItem(set)
            }
        }

        // Data tools
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationRow(
                        title = "View Audit Log",
                        description = "See all data changes",
                        onClick = onNavigateToAuditLog
                    )
                }
            }
        }

        // About section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Alignation v1.0",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Track your aligner wear compliance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Developed by DNA Kode",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Date picker dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (uiState.settings?.treatmentStartDate ?: LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { viewModel.hideDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            viewModel.setTreatmentStartDate(date)
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDatePicker() }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AlarmToggleRow(
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun NavigationRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SetHistoryItem(set: AlignerSet) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        .withZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
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
            Text(
                text = "Set #${set.setNumber}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Started ${formatter.format(set.startedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                set.notes?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
