package com.uduschat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class UdusColors(
    val brandEmerald: Color,
    val accentGreen: Color,
    val outgoingBubble: Color,
    val incomingBubble: Color,
    val chatWallpaper: Color,
    val headerContainer: Color,
    val headerContent: Color,
    val tickReadBlue: Color,
    val tickMuted: Color,
    val unreadBadge: Color,
    val quoteBar: Color,
    val quoteBackground: Color,
    val subtleSurface: Color
)

val LocalUdusColors = staticCompositionLocalOf {
    UdusColors(
        brandEmerald = Color(0xFF008069),
        accentGreen = Color(0xFF25D366),
        outgoingBubble = Color(0xFFD9FDD3),
        incomingBubble = Color(0xFFFFFFFF),
        chatWallpaper = Color(0xFFEFEAE2),
        headerContainer = Color(0xFFFFFFFF),
        headerContent = Color(0xFF111B21),
        tickReadBlue = Color(0xFF53BDEB),
        tickMuted = Color(0xFF667781),
        unreadBadge = Color(0xFF25D366),
        quoteBar = Color(0xFF008069),
        quoteBackground = Color(0x14000000),
        subtleSurface = Color(0xFFF0F2F5)
    )
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008069),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9FDD3),
    onPrimaryContainer = Color(0xFF062E22),
    secondary = Color(0xFF25D366),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111B21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111B21),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF54656F),
    outline = Color(0xFFD1D7DB),
    error = Color(0xFFEA0038)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00A884),
    onPrimary = Color(0xFF0B141A),
    primaryContainer = Color(0xFF005C4B),
    onPrimaryContainer = Color(0xFFE9EDEF),
    secondary = Color(0xFF25D366),
    onSecondary = Color(0xFF0B141A),
    background = Color(0xFF0B141A),
    onBackground = Color(0xFFE9EDEF),
    surface = Color(0xFF0B141A),
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = Color(0xFF202C33),
    onSurfaceVariant = Color(0xFF8696A0),
    outline = Color(0xFF2A3942),
    error = Color(0xFFF15C6D)
)

@Composable
fun UdusChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val udusColors = if (darkTheme) {
        UdusColors(
            brandEmerald = Color(0xFF00A884),
            accentGreen = Color(0xFF25D366),
            outgoingBubble = Color(0xFF005C4B),
            incomingBubble = Color(0xFF202C33),
            chatWallpaper = Color(0xFF0B141A),
            headerContainer = Color(0xFF111B21),
            headerContent = Color(0xFFE9EDEF),
            tickReadBlue = Color(0xFF53BDEB),
            tickMuted = Color(0xFF8696A0),
            unreadBadge = Color(0xFF00A884),
            quoteBar = Color(0xFF25D366),
            quoteBackground = Color(0x24000000),
            subtleSurface = Color(0xFF1F2C34)
        )
    } else {
        UdusColors(
            brandEmerald = Color(0xFF008069),
            accentGreen = Color(0xFF25D366),
            outgoingBubble = Color(0xFFD9FDD3),
            incomingBubble = Color(0xFFFFFFFF),
            chatWallpaper = Color(0xFFEFEAE2),
            headerContainer = Color(0xFFFFFFFF),
            headerContent = Color(0xFF111B21),
            tickReadBlue = Color(0xFF53BDEB),
            tickMuted = Color(0xFF667781),
            unreadBadge = Color(0xFF25D366),
            quoteBar = Color(0xFF008069),
            quoteBackground = Color(0x10000000),
            subtleSurface = Color(0xFFF0F2F5)
        )
    }

    val typography = Typography(
        titleLarge = TextStyle(
            fontSize = (22 * fontScale).sp,
            fontWeight = FontWeight.Bold
        ),
        titleMedium = TextStyle(
            fontSize = (17 * fontScale).sp,
            fontWeight = FontWeight.SemiBold
        ),
        bodyLarge = TextStyle(
            fontSize = (16 * fontScale).sp,
            lineHeight = (22 * fontScale).sp
        ),
        bodyMedium = TextStyle(
            fontSize = (14.5 * fontScale).sp,
            lineHeight = (20 * fontScale).sp
        ),
        labelMedium = TextStyle(
            fontSize = (12 * fontScale).sp,
            fontWeight = FontWeight.Medium
        )
    )

    CompositionLocalProvider(LocalUdusColors provides udusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
