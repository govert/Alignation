package com.alignation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alignation.ui.theme.BudgetDanger

@Composable
fun BudgetRing(
    progress: Float,  // 0 = nothing used, 1.0 = 2h target used, 1.5 = 3h max used
    color: Color,     // Color for the normal budget zone
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 16.dp,
    dangerZoneColor: Color = BudgetDanger,
    content: @Composable () -> Unit = {}
) {
    // Ring shows REMAINING budget - depletes as time is used
    // Full ring (green + orange) = full 3h budget available
    // Green portion (2/3) = normal 2h budget remaining
    // Orange portion (1/3) = extra hour before problem day
    // Empty ring = budget exhausted (problem day!)

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1.5f),
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val arcSize = Size(
                width = this.size.width - strokeWidthPx,
                height = this.size.height - strokeWidthPx
            )
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

            // Background: empty/depleted state (gray ring)
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Calculate what's remaining
            // progress 0-1.0 depletes the green zone (240°)
            // progress 1.0-1.5 depletes the orange zone (120°)

            val greenUsed = animatedProgress.coerceAtMost(1f)      // 0 to 1
            val orangeUsed = ((animatedProgress - 1f) / 0.5f).coerceIn(0f, 1f)  // 0 to 1

            val greenRemaining = 1f - greenUsed   // 1 to 0
            val orangeRemaining = 1f - orangeUsed // 1 to 0

            // Draw orange zone first (it's "behind" the green visually)
            // Orange zone is the last 120° of the ring (from 150° to 270°)
            if (orangeRemaining > 0f) {
                val orangeSweep = orangeRemaining * 120f
                // Draw from where green ends, going clockwise
                drawArc(
                    color = dangerZoneColor,
                    startAngle = 150f,  // Starts where 240° green zone ends
                    sweepAngle = orangeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // Draw green zone on top
            // Green zone is first 240° of the ring (from -90° to 150°)
            if (greenRemaining > 0f) {
                val greenSweep = greenRemaining * 240f
                drawArc(
                    color = color,
                    startAngle = -90f,  // Start at top
                    sweepAngle = greenSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }

        content()
    }
}
