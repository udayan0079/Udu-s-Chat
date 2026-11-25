package com.uduschat.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uduschat.app.core.CountryCode
import com.uduschat.app.core.PhoneNumberUtils
import com.uduschat.app.domain.model.MessageStatus
import com.uduschat.app.ui.theme.LocalUdusColors
import kotlin.math.abs

private val avatarPalette = listOf(
    Color(0xFF008069),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFFD81B60),
    Color(0xFFF4511E),
    Color(0xFF00897B),
    Color(0xFF3949AB),
    Color(0xFF6D4C41)
)

@Composable
fun AvatarCircle(
    name: String,
    avatarUrl: String?,
    size: Dp = 48.dp,
    isGroup: Boolean = false,
    showOnlineDot: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = remember(name) {
        avatarPalette[abs(name.hashCode()) % avatarPalette.size]
    }
    val initials = remember(name) {
        name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "U" }
    }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isGroup) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = name,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            } else {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                )
            }
        }
        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366))
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun MessageTicksIcon(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    val udus = LocalUdusColors.current
    val (icon, tint) = when (status) {
        MessageStatus.SENDING -> Icons.Default.AccessTime to udus.tickMuted
        MessageStatus.SENT -> Icons.Default.Check to udus.tickMuted
        MessageStatus.DELIVERED -> Icons.Default.DoneAll to udus.tickMuted
        MessageStatus.READ -> Icons.Default.DoneAll to udus.tickReadBlue
    }
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = tint,
        modifier = modifier.size(16.dp)
    )
}

data class WallpaperPreset(
    val id: String,
    val label: String,
    val lightStart: Color,
    val lightEnd: Color,
    val darkStart: Color,
    val darkEnd: Color
)

val chatWallpaperPresets = listOf(
    WallpaperPreset("default", "Classic WhatsApp", Color(0xFFEFEAE2), Color(0xFFE7E0D4), Color(0xFF0B141A), Color(0xFF0E1820)),
    WallpaperPreset("emerald", "Emerald Mist", Color(0xFFDDF3EC), Color(0xFFC8EAE0), Color(0xFF071E1A), Color(0xFF0C2A24)),
    WallpaperPreset("ocean", "Ocean Breeze", Color(0xFFE0EEF8), Color(0xFFCFE4F4), Color(0xFF0A1726), Color(0xFF102236)),
    WallpaperPreset("sunset", "Warm Sand", Color(0xFFF6E9DF), Color(0xFFEFDBC9), Color(0xFF1D1412), Color(0xFF281B17)),
    WallpaperPreset("lavender", "Soft Twilight", Color(0xFFECE6F6), Color(0xFFDFD5F0), Color(0xFF151124), Color(0xFF1D1732))
)

@Composable
fun WallpaperBackground(
    wallpaperId: String?,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val preset = chatWallpaperPresets.firstOrNull { it.id == wallpaperId } ?: chatWallpaperPresets.first()
    val colors = if (darkTheme) listOf(preset.darkStart, preset.darkEnd) else listOf(preset.lightStart, preset.lightEnd)
    val patternTint = if (darkTheme) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.035f)

    Box(
        modifier = modifier.background(Brush.verticalGradient(colors))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 64.dp.toPx()
            var y = 0f
            var row = 0
            while (y < size.height + step) {
                var x = if (row % 2 == 0) 0f else step / 2f
                while (x < size.width + step) {
                    drawCircle(
                        color = patternTint,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                    x += step
                }
                y += step
                row++
            }
        }
    }
}

@Composable
fun CountryPickerDialog(
    selectedCountry: CountryCode,
    onSelect: (CountryCode) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) PhoneNumberUtils.supportedCountries
        else PhoneNumberUtils.supportedCountries.filter {
            it.name.lowercase().contains(q) ||
                it.dialCode.contains(q) ||
                it.iso.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a country") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search country or +code") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { "${it.iso}_${it.dialCode}" }) { country ->
                        val isSelected = country.iso == selectedCountry.iso && country.dialCode == selectedCountry.dialCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelect(country) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(country.flag, fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = country.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = country.dialCode,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CloudStatusPill(
    isFirebaseConfigured: Boolean,
    onClick: (() -> Unit)? = null
) {
    val bg = if (isFirebaseConfigured) Color(0x1E008069) else Color(0x1EF57C00)
    val fg = if (isFirebaseConfigured) Color(0xFF008069) else Color(0xFFE65100)
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                text = if (isFirebaseConfigured) "Global Cloud Connected" else "Demo Mode (Local)",
                color = fg,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
