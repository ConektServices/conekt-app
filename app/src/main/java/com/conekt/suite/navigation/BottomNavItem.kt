package com.conekt.suite.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val icon: ImageVector
) {
    companion object {
        val items = listOf(
            BottomNavItem(Routes.PULSE, Icons.Rounded.GraphicEq),
            BottomNavItem(Routes.VAULT, Icons.Rounded.Folder),
            BottomNavItem(Routes.CANVAS, Icons.Rounded.EditNote),
            BottomNavItem(Routes.MUSIC, Icons.Rounded.Headphones),
            BottomNavItem(Routes.PROFILE, Icons.Rounded.AccountCircle)
        )
    }
}