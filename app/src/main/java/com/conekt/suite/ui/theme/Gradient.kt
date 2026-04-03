package com.conekt.suite.ui.theme

import androidx.compose.ui.graphics.Brush

object ConektGradient {
    val brandHorizontal = Brush.horizontalGradient(
        colors = listOf(BrandStart, BrandEnd)
    )

    val brandVertical = Brush.verticalGradient(
        colors = listOf(BrandStart, BrandEnd)
    )
}