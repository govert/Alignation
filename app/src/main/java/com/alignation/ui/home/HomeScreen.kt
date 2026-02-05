package com.alignation.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alignation.ui.components.BudgetRing
import com.alignation.ui.theme.BudgetApproaching
import com.alignation.ui.theme.BudgetComfortable
import com.alignation.ui.theme.BudgetDanger
import com.alignation.ui.theme.BudgetGettingThere
import com.alignation.ui.theme.BudgetProblem
import com.alignation.ui.theme.BudgetWarning
import com.alignation.ui.theme.RemoveButtonColor
import com.alignation.ui.theme.ReplaceButtonColor
import com.alignation.ui.theme.StreakFire
import java.time.Duration
import kotlin.math.max

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.needsSetup) {
        SetupPrompt()
        return
    }

    val budgetColor by animateColorAsState(
        targetValue = uiState.budgetStatus.toColor(),
        animationSpec = tween(500),
        label = "budgetColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Treatment progress header
        TreatmentProgressCard(
            weekNumber = uiState.weekNumber,
            totalWeeks = uiState.totalWeeks,
            daysRemaining = uiState.daysRemaining,
            progress = uiState.treatmentProgress
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Budget ring - main focus
        // Progress is based on the daily allowance (2h target = 100%)
        // Values above 100% go into the "danger zone" up to 150% (3h max)
        val effectiveAllowance = uiState.dailyAllowanceMinutes + uiState.graceMinutes
        val budgetProgress = (uiState.todayTimeOut.toMinutes().toFloat() / effectiveAllowance).coerceIn(0f, 1.5f)
        val isOverBudget = uiState.todayTimeOut.toMinutes() > effectiveAllowance
        val isInDanger = uiState.todayTimeOut.toMinutes() > effectiveAllowance &&
                         uiState.todayTimeOut.toMinutes() <= uiState.maxAllowanceMinutes

        BudgetRing(
            progress = budgetProgress,
            color = budgetColor,
            size = 220.dp,
            strokeWidth = 18.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatDurationMinutes(uiState.todayTimeOut),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = budgetColor
                )
                if (isOverBudget) {
                    Text(
                        text = "over ${effectiveAllowance}min target",
                        style = MaterialTheme.typography.bodySmall,
                        color = budgetColor
                    )
                    if (isInDanger) {
                        Text(
                            text = "${uiState.maxAllowanceMinutes - uiState.todayTimeOut.toMinutes().toInt()}min until problem",
                            style = MaterialTheme.typography.labelSmall,
                            color = BudgetDanger
                        )
                    }
                } else {
                    Text(
                        text = "of ${effectiveAllowance}min budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.graceMinutes > 0) {
                        Text(
                            text = "+${uiState.graceMinutes}min grace",
                            style = MaterialTheme.typography.labelSmall,
                            color = BudgetComfortable
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Current status
        CurrentStatusCard(
            isAlignerIn = uiState.isAlignerIn,
            sessionDuration = uiState.currentSessionDuration,
            budgetStatus = uiState.budgetStatus
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action button
        AnimatedContent(
            targetState = uiState.isAlignerIn,
            transitionSpec = {
                (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut())
            },
            label = "button"
        ) { isIn ->
            Button(
                onClick = {
                    if (isIn) {
                        viewModel.onRemoveClicked()
                    } else {
                        viewModel.onReplaceClicked()
                    }
                },
                modifier = Modifier
                    .size(140.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIn) RemoveButtonColor else ReplaceButtonColor
                )
            ) {
                Text(
                    text = if (isIn) "Remove" else "Replace",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Streak and problem days
        StatsRow(
            streak = uiState.currentStreak,
            problemDays = uiState.problemDays,
            totalDays = uiState.totalDaysTracked
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SetupPrompt() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Welcome to Alignation",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Go to Settings to set your treatment start date to get started.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TreatmentProgressCard(
    weekNumber: Int,
    totalWeeks: Int,
    daysRemaining: Int,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Week $weekNumber of $totalWeeks",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$daysRemaining days left",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun CurrentStatusCard(
    isAlignerIn: Boolean,
    sessionDuration: Duration,
    budgetStatus: BudgetStatus
) {
    val statusColor = if (isAlignerIn) BudgetComfortable else budgetStatus.toColor()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAlignerIn) "Aligners IN" else "Aligners OUT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = statusColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "for ${formatDuration(sessionDuration)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsRow(
    streak: Int,
    problemDays: Int,
    totalDays: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Streak
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (streak > 0) {
                    Text(
                        text = "\uD83D\uDD25",  // Fire emoji
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "$streak",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (streak > 0) StreakFire else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "day streak",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Problem days
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$problemDays",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (problemDays > 0) BudgetProblem else BudgetComfortable
            )
            Text(
                text = "problem days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun BudgetStatus.toColor(): Color = when (this) {
    BudgetStatus.COMFORTABLE -> BudgetComfortable
    BudgetStatus.GETTING_THERE -> BudgetGettingThere
    BudgetStatus.APPROACHING -> BudgetApproaching
    BudgetStatus.WARNING -> BudgetWarning
    BudgetStatus.DANGER -> BudgetDanger
    BudgetStatus.PROBLEM -> BudgetProblem
}

private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    val seconds = duration.toSecondsPart()

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatDurationMinutes(duration: Duration): String {
    val totalMinutes = duration.toMinutes()
    return "${totalMinutes}min"
}
