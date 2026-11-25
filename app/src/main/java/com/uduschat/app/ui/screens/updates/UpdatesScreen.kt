package com.uduschat.app.ui.screens.updates

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uduschat.app.core.TimeFormatters
import com.uduschat.app.domain.model.ChannelItem
import com.uduschat.app.domain.model.StatusType
import com.uduschat.app.domain.model.StatusUpdate
import com.uduschat.app.domain.model.UserProfile
import com.uduschat.app.domain.model.UserStatusGroup
import com.uduschat.app.ui.components.AvatarCircle
import com.uduschat.app.ui.theme.LocalUdusColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val statusBgPalette = listOf(
    0xFF008069L,
    0xFF1F3C88L,
    0xFF6A1B9AL,
    0xFFC62828L,
    0xFF2E7D32L,
    0xFF37474FL
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesTabScreen(
    currentUser: UserProfile?,
    statuses: List<StatusUpdate>,
    channels: List<ChannelItem>,
    onPostTextStatus: (String, Long) -> Unit,
    onPostImageStatus: (String, String) -> Unit,
    onViewStatusGroup: (UserStatusGroup) -> Unit,
    onToggleChannelFollow: (String) -> Unit
) {
    val udus = LocalUdusColors.current
    val myUid = currentUser?.uid.orEmpty()

    var showTextComposer by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pendingImageUri = uri
    }

    val now = System.currentTimeMillis()
    val activeStatuses = remember(statuses) { statuses.filter { it.expiresAt > now } }
    val myStatuses = remember(activeStatuses, myUid) { activeStatuses.filter { it.userId == myUid } }

    val peerGroups = remember(activeStatuses, myUid) {
        activeStatuses
            .filter { it.userId != myUid }
            .groupBy { it.userId }
            .map { (uid, items) ->
                val sorted = items.sortedBy { it.createdAt }
                val first = sorted.first()
                UserStatusGroup(
                    userId = uid,
                    userName = first.userName,
                    userPhone = first.userPhone,
                    userAvatarUrl = first.userAvatarUrl,
                    items = sorted,
                    hasUnviewed = sorted.any { !it.viewers.containsKey(myUid) }
                )
            }
            .sortedByDescending { it.hasUnviewed }
    }
    val recentGroups = peerGroups.filter { it.hasUnviewed }
    val viewedGroups = peerGroups.filter { !it.hasUnviewed }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text("Updates", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showTextComposer = true },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Text status")
                }
                FloatingActionButton(
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    containerColor = udus.brandEmerald,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Photo status")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(6.dp))
                Text("Status", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))

                // My status row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (myStatuses.isNotEmpty() && currentUser != null) {
                                onViewStatusGroup(
                                    UserStatusGroup(
                                        userId = myUid,
                                        userName = "My status",
                                        userPhone = currentUser.phoneNumber,
                                        userAvatarUrl = currentUser.avatarUrl,
                                        items = myStatuses.sortedBy { it.createdAt },
                                        hasUnviewed = false
                                    )
                                )
                            } else {
                                showTextComposer = true
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AvatarCircle(
                            name = currentUser?.displayName ?: "Me",
                            avatarUrl = currentUser?.avatarUrl,
                            size = 54.dp,
                            modifier = if (myStatuses.isNotEmpty()) {
                                Modifier.border(2.5.dp, udus.brandEmerald, CircleShape).padding(3.dp)
                            } else Modifier
                        )
                        if (myStatuses.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(udus.brandEmerald)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("My status", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (myStatuses.isEmpty()) "Tap to add status update" else "${myStatuses.size} update(s) • ${TimeFormatters.formatRelativeShort(myStatuses.first().createdAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (recentGroups.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    Text("Recent updates", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                }
                items(recentGroups, key = { it.userId }) { group ->
                    StatusGroupRow(group = group, ringColor = udus.brandEmerald, onClick = { onViewStatusGroup(group) })
                }
            }

            if (viewedGroups.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    Text("Viewed updates", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                }
                items(viewedGroups, key = { it.userId }) { group ->
                    StatusGroupRow(group = group, ringColor = MaterialTheme.colorScheme.outline, onClick = { onViewStatusGroup(group) })
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(14.dp))
                Text("Channels", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            items(channels, key = { it.id }) { channel ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(name = channel.name, avatarUrl = null, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(channel.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${channel.followerCount / 1000}K followers",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (channel.isFollowing) {
                                OutlinedButton(onClick = { onToggleChannelFollow(channel.id) }) {
                                    Text("Following")
                                }
                            } else {
                                Button(onClick = { onToggleChannelFollow(channel.id) }) {
                                    Text("Follow")
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(channel.latestUpdate, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item { Spacer(Modifier.height(92.dp)) }
        }
    }

    if (showTextComposer) {
        TextStatusComposerDialog(
            onDismiss = { showTextComposer = false },
            onPost = { text, colorHex ->
                onPostTextStatus(text, colorHex)
                showTextComposer = false
            }
        )
    }

    val imgUri = pendingImageUri
    if (imgUri != null) {
        ImageStatusComposerDialog(
            imageUri = imgUri,
            onDismiss = { pendingImageUri = null },
            onPost = { caption ->
                onPostImageStatus(imgUri.toString(), caption)
                pendingImageUri = null
            }
        )
    }
}

@Composable
private fun StatusGroupRow(
    group: UserStatusGroup,
    ringColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(
            name = group.userName,
            avatarUrl = group.userAvatarUrl,
            size = 54.dp,
            modifier = Modifier
                .border(2.5.dp, ringColor, CircleShape)
                .padding(3.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(group.userName, fontWeight = FontWeight.SemiBold)
            Text(
                text = TimeFormatters.formatRelativeShort(group.items.last().createdAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TextStatusComposerDialog(
    onDismiss: () -> Unit,
    onPost: (String, Long) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var colorIndex by remember { mutableIntStateOf(0) }
    val bgColor = Color(statusBgPalette[colorIndex])

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Text status", modifier = Modifier.weight(1f))
                IconButton(onClick = { colorIndex = (colorIndex + 1) % statusBgPalette.size }) {
                    Icon(Icons.Default.Palette, contentDescription = "Change color")
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type a status", color = Color.White.copy(alpha = 0.7f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Color.White
                    ),
                    textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPost(text, statusBgPalette[colorIndex]) },
                enabled = text.trim().isNotEmpty()
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ImageStatusComposerDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo status") },
        text = {
            Column {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Add a caption...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onPost(caption) }) { Text("Post") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StatusViewerOverlay(
    group: UserStatusGroup,
    myUid: String,
    contacts: List<UserProfile>,
    onMarkViewed: (String) -> Unit,
    onDeleteStatus: (String) -> Unit,
    onReplyToStatus: suspend (StatusUpdate, String) -> Result<String>,
    onOpenChat: (String) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var index by remember { mutableIntStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    var replyText by remember { mutableStateOf("") }
    var showViewers by remember { mutableStateOf(false) }

    val currentItem = group.items.getOrNull(index)
    if (currentItem == null) {
        onClose()
        return
    }

    LaunchedEffect(currentItem.id, showViewers) {
        onMarkViewed(currentItem.id)
        if (showViewers) return@LaunchedEffect
        progress = 0f
        val steps = 60
        repeat(steps) { step ->
            delay(80L)
            progress = (step + 1) / steps.toFloat()
        }
        if (index + 1 < group.items.size) {
            index++
        } else {
            onClose()
        }
    }

    val isMine = currentItem.userId == myUid
    val bgColor = if (currentItem.type == StatusType.TEXT) Color(currentItem.backgroundHex) else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (currentItem.type == StatusType.TEXT) {
                Text(
                    text = currentItem.text,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(28.dp)
                )
            } else {
                AsyncImage(
                    model = currentItem.mediaUrl,
                    contentDescription = currentItem.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                if (currentItem.caption.isNotBlank()) {
                    Text(
                        text = currentItem.caption,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 86.dp, start = 20.dp, end = 20.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Left / right tap zones
        Row(modifier = Modifier.fillMaxSize().padding(top = 80.dp, bottom = 84.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable { if (index > 0) index-- }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable {
                        if (index + 1 < group.items.size) index++ else onClose()
                    }
            )
        }

        // Top progress bars + header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                group.items.forEachIndexed { idx, _ ->
                    val barValue = when {
                        idx < index -> 1f
                        idx == index -> progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { barValue },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarCircle(name = group.userName, avatarUrl = group.userAvatarUrl, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.userName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        TimeFormatters.formatRelativeShort(currentItem.createdAt),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (isMine) {
                    IconButton(onClick = {
                        onDeleteStatus(currentItem.id)
                        if (group.items.size <= 1) onClose()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete status", tint = Color.White)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        // Bottom bar: viewers (if mine) or reply box (if peer's)
        if (isMine) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showViewers = true }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${currentItem.viewers.size} views", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Reply to status...", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val txt = replyText.trim()
                        if (txt.isNotEmpty()) {
                            scope.launch {
                                onReplyToStatus(currentItem, txt).onSuccess { chatId ->
                                    replyText = ""
                                    onClose()
                                    onOpenChat(chatId)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF008069))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply", tint = Color.White)
                }
            }
        }
    }

    if (showViewers) {
        AlertDialog(
            onDismissRequest = { showViewers = false },
            title = { Text("Viewed by ${currentItem.viewers.size}") },
            text = {
                if (currentItem.viewers.isEmpty()) {
                    Text("No views yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentItem.viewers.entries.forEach { (uid, ts) ->
                            val name = contacts.firstOrNull { it.uid == uid }?.displayName ?: uid
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(TimeFormatters.formatRelativeShort(ts), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showViewers = false }) { Text("Close") }
            }
        )
    }
}
