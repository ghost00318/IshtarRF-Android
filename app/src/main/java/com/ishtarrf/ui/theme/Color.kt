package com.ishtarrf.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * IshtarRF — refined palette.
 * Deeper, slightly bluer navy than the original (#0D102B -> #0A0E27), a more
 * vivid amber accent, and a teal secondary for highlights/active states.
 */
private val IshtarBg = Color(0xFF0A0E27)
private val IshtarSurface = Color(0xFF121732)
private val IshtarSurfaceVariant = Color(0xFF1B2244)
private val IshtarAmber = Color(0xFFFF9F3E)
private val IshtarAmberContainer = Color(0xFF5A3A12)
private val IshtarTeal = Color(0xFF5BD6C4)
private val IshtarTextLight = Color(0xFFE7EAF6)
private val IshtarMuted = Color(0xFFB7BEDC)
private val IshtarOutline = Color(0xFF2E376B)
private val IshtarError = Color(0xFFFF6B6B)

val IshtarColorScheme: ColorScheme = darkColorScheme(
    primary = IshtarAmber,
    onPrimary = Color(0xFF1A1003),
    primaryContainer = IshtarAmberContainer,
    onPrimaryContainer = Color(0xFFFFE0BC),
    secondary = IshtarTeal,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF0E4A41),
    onSecondaryContainer = Color(0xFFB8FFF1),
    tertiary = Color(0xFF8AA0FF),
    onTertiary = Color(0xFF0A1240),
    background = IshtarBg,
    onBackground = IshtarTextLight,
    surface = IshtarSurface,
    onSurface = IshtarTextLight,
    surfaceVariant = IshtarSurfaceVariant,
    onSurfaceVariant = IshtarMuted,
    surfaceContainer = Color(0xFF161C3A),
    surfaceContainerHigh = Color(0xFF1C2348),
    outline = IshtarOutline,
    outlineVariant = Color(0xFF222A57),
    error = IshtarError,
    onError = Color(0xFF2B0000),
)

val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF3A86FF),
    onPrimary = Color(0xFF001233),
    primaryContainer = Color(0xFF21487F),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFF8E9BB3),
    onSecondary = Color(0xFF101418),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFC4C4C4),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF242424),
    outline = Color(0xFF3A3A3A),
    error = Color(0xFFFF6B6B),
)

val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0F174F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1F4),
    onPrimaryContainer = Color(0xFF0B123E),
    secondary = Color(0xFFB85C00),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0B0E14),
    surface = Color(0xFFF5F7FB),
    onSurface = Color(0xFF0B0E14),
    surfaceVariant = Color(0xFFE6EAF2),
    onSurfaceVariant = Color(0xFF454B5C),
    surfaceContainer = Color(0xFFEFF2F8),
    surfaceContainerHigh = Color(0xFFE8ECF4),
    outline = Color(0xFFD9DDE7),
    error = Color(0xFFC62828),
)
