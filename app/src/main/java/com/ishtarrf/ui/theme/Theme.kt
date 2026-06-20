package com.ishtarrf.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ishtarrf.domain.AppTheme

@Composable
fun IshtarRFTheme(
    appTheme: AppTheme,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (appTheme) {
        AppTheme.ISHTAR -> IshtarColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.LIGHT -> LightColorScheme
    }
    val lightStatusBar = appTheme == AppTheme.LIGHT

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = lightStatusBar
            controller.isAppearanceLightNavigationBars = lightStatusBar
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IshtarTypography,
        content = content,
    )
}
