package com.alignation.ui.theme

import androidx.compose.ui.graphics.Color

// Primary colors - tooth/dental inspired blue-white
val Primary = Color(0xFF2196F3)
val PrimaryDark = Color(0xFF1976D2)
val PrimaryLight = Color(0xFFBBDEFB)

// Status colors (legacy)
val StatusGreen = Color(0xFF4CAF50)
val StatusGreenLight = Color(0xFFC8E6C9)
val StatusYellow = Color(0xFFFFC107)
val StatusYellowLight = Color(0xFFFFF9C4)
val StatusRed = Color(0xFFF44336)
val StatusRedLight = Color(0xFFFFCDD2)

// Budget urgency colors (time OUT spent)
val BudgetComfortable = Color(0xFF4CAF50)    // <1h used - green, plenty of budget
val BudgetGettingThere = Color(0xFF8BC34A)   // 1-1.5h used - light green
val BudgetApproaching = Color(0xFFFFC107)    // 1.5-2h used - yellow, approaching target
val BudgetWarning = Color(0xFFFF9800)        // 2-2.5h used - orange, over target
val BudgetDanger = Color(0xFFF44336)         // 2.5-3h used - red, streak at risk
val BudgetProblem = Color(0xFFB71C1C)        // >3h used - dark red, problem day

// Streak colors
val StreakFire = Color(0xFFFF5722)           // Streak indicator

// Button colors
val RemoveButtonColor = Color(0xFFFF7043)  // Warm orange-red for remove
val ReplaceButtonColor = Color(0xFF66BB6A) // Fresh green for replace

// Backgrounds
val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceDark = Color(0xFF121212)
val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF1E1E1E)

// Text
val OnPrimaryLight = Color.White
val OnPrimaryDark = Color.White
val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF757575)
