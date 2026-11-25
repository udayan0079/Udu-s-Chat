package com.uduschat.app.ui.screens.chats

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.uduschat.app.core.PhoneNumberUtils
import com.uduschat.app.core.TimeFormatters
import com.uduschat.app.core.VoicePlaybackState
import com.uduschat.app.domain.model.AppPreferences
import com.uduschat.app.domain.model.CallType
import com.uduschat.app.domain.model.ChatMessage
import com.uduschat.app.domain.model.ChatThread
import com.uduschat.app.domain.model.ChatType
import com.uduschat.app.domain.model.MessageDraftAttachment
import com.uduschat.app.domain.model.MessageType
import com.uduschat.app.domain.model.ThemeMode
import com.uduschat.app.domain.model.UserProfile
import com.uduschat.app.ui.components.AvatarCircle
import com.uduschat.app.ui.components.MessageTicksIcon
import com.uduschat.app.ui.components.WallpaperBackground
import com.uduschat.app.ui.components.chatWallpaperPresets
import com.uduschat.app.ui.theme.LocalUdusColors
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private val quickReactions = listOf("\uD83D\uDC4D", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDE4F")
private val quickEmojis = listOf(
    "\uD83D\uDE00", "\uD83D\uDE02", "\uD83E\uDD70", "\uD83D\uDE0D", "\uD83D\uDE0E", "\uD83E\uDD14",
    "\uD83D\uDC4D", "\uD83D\uDC4F", "\uD83D\uDD25", "\u2764\uFE0F", "\uD83D\uDE4F", "\uD83C\uDF89",
    "\uD83D\uDE22", "\uD83D\uDE2E", "\uD83E\uDD23", "\uD83D\uDE0A", "\u2705", "\uD83D\uDCAF"
)
private val groupSenderColors = listOf(
    Color(0xFF00897B),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFFE53935),
    Color(0xFFF4511E)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: ChatThread?,
    messages: List<ChatMessage>,
    allChats: List<ChatThread>,
    contacts: List<UserProfile>,
    currentUser: UserProfile?,
    preferences: AppPreferences,
    isRecordingVoice: Boolean,
    recordingDurationMs: Long,
    voicePlaybackState: VoicePlaybackState,
    onBack: () -> Unit,
    onMarkRead: () -> Unit,
    onSendMessage: (text: String, attachment: MessageDraftAttachment?, replyTo: ChatMessage?) -> Unit,
    onSaveDraft: (String) -> Unit,
    onStartVoiceRecording: () -> Boolean,
    onStopAndSendVoiceNote: (replyTo: ChatMessage?) -> Unit,
    onCancelVoiceRecording: () -> Unit,
    onToggleVoicePlayback: (ChatMessage) -> Unit,
    onCycleVoiceSpeed: () -> Unit,
    onToggleReaction: (messageId: String, emoji: String) -> Unit,
    onToggleStarMessage: (messageId: String) -> Unit,
    onEditMessage: (messageId: String, newText: String) -> Unit,
    onDeleteMessage: (messageId: String, forEveryone: Boolean) -> Unit,
    onForwardMessages: (targetChatIds: List<String>, messages: List<ChatMessage>) -> Unit,
    onMarkViewOnceOpened: (messageId: String) -> Unit,
    onStartCall: (CallType) -> Unit,
    onToggleMute: () -> Unit,
    onSetDisappearingTimer: (Long) -> Unit,
    onSetWallpaper: (String?) -> Unit,
    onClearChat: () -> Unit,
    onBlockUser: (String, Boolean) -> Unit,
    onUpdateGroupInfo: (String, String) -> Unit,
    onAddGroupMembers: (List<String>) -> Unit,
    onRemoveGroupMember: (String) -> Unit,
    onToggleGroupAdmin: (String) -> Unit,
    onLeaveGroup: () -> Unit
) {
    val context = LocalContext.current
    val udus = LocalUdusColors.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val darkTheme = when (preferences.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val myUid = currentUser?.uid.orEmpty()
    val isPeerBlocked = chat?.otherUserId != null && chat.otherUserId in currentUser?.privacy?.blockedUids.orEmpty()

    var inputText by remember(chat?.id) { mutableStateOf(chat?.draftText.orEmpty()) }
    var replyTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var editingTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardSourceMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var viewOnceImageMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    var showEmojiTray by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var viewOnceNextAttachment by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showContactSharePicker by remember { mutableStateOf(false) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoPreview by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(chat?.id, messages.size) {
        onMarkRead()
        if (messages.isNotEmpty() && !showSearchBar) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pendingPhotoPreview = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) {
            pendingPhotoPreview = uri
        }
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val out = File(dir, "img_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val meta = queryUriMetadata(context, uri)
            onSendMessage(
                "",
                MessageDraftAttachment(
                    type = MessageType.DOCUMENT,
                    uriOrPath = uri.toString(),
                    fileName = meta.first,
                    fileSizeBytes = meta.second,
                    mimeType = context.contentResolver.getType(uri)
                ),
                replyTarget
            )
            replyTarget = null
        }
    }

    val audioFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val meta = queryUriMetadata(context, uri)
            onSendMessage(
                "",
                MessageDraftAttachment(
                    type = MessageType.AUDIO,
                    uriOrPath = uri.toString(),
                    fileName = meta.first,
                    fileSizeBytes = meta.second,
                    mimeType = context.contentResolver.getType(uri) ?: "audio/*"
                ),
                replyTarget
            )
            replyTarget = null
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartVoiceRecording()
        } else {
            Toast.makeText(context, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraFlow() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val out = File(dir, "img_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun submitText() {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return
        val editMsg = editingTarget
        if (editMsg != null) {
            onEditMessage(editMsg.id, trimmed)
            editingTarget = null
            inputText = ""
            onSaveDraft("")
        } else {
            onSendMessage(trimmed, null, replyTarget)
            replyTarget = null
            inputText = ""
            onSaveDraft("")
        }
    }

    val filteredMessages = remember(messages, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) messages
        else messages.filter { it.text.lowercase().contains(q) || (it.fileName?.lowercase()?.contains(q) == true) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            onSaveDraft(inputText)
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showInfoSheet = true }
                        ) {
                            AvatarCircle(
                                name = chat?.title ?: "Chat",
                                avatarUrl = chat?.avatarUrl,
                                size = 38.dp,
                                isGroup = chat?.type == ChatType.GROUP,
                                showOnlineDot = chat?.type == ChatType.DIRECT && chat.isOnline
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chat?.title ?: "udu's chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitle = when {
                                    chat == null -> ""
                                    chat.typingUserIds.isNotEmpty() -> "typing..."
                                    chat.type == ChatType.GROUP -> "${chat.participantIds.size} participants"
                                    else -> TimeFormatters.formatLastSeen(chat.isOnline, chat.lastSeenAt)
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (chat?.typingUserIds?.isNotEmpty() == true) udus.brandEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { onStartCall(CallType.VIDEO) }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video call")
                        }
                        IconButton(onClick = { onStartCall(CallType.VOICE) }) {
                            Icon(Icons.Default.Call, contentDescription = "Voice call")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Chat menu")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (chat?.type == ChatType.GROUP) "Group info" else "View contact") },
                                    leadingIcon = { Icon(Icons.Default.Info, null) },
                                    onClick = { showMenu = false; showInfoSheet = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Search") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    onClick = { showMenu = false; showSearchBar = !showSearchBar; if (!showSearchBar) searchQuery = "" }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (chat?.isMuted == true) "Unmute notifications" else "Mute notifications") },
                                    leadingIcon = { Icon(Icons.Default.NotificationsOff, null) },
                                    onClick = { showMenu = false; onToggleMute() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Disappearing messages") },
                                    leadingIcon = { Icon(Icons.Default.Timer, null) },
                                    onClick = { showMenu = false; showDisappearingDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Wallpaper") },
                                    leadingIcon = { Icon(Icons.Default.Wallpaper, null) },
                                    onClick = { showMenu = false; showWallpaperDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear chat") },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = { showMenu = false; onClearChat() }
                                )
                                if (chat?.type == ChatType.DIRECT && chat.otherUserId != null) {
                                    DropdownMenuItem(
                                        text = { Text(if (isPeerBlocked) "Unblock" else "Block") },
                                        leadingIcon = { Icon(Icons.Default.Block, null) },
                                        onClick = {
                                            showMenu = false
                                            onBlockUser(chat.otherUserId, !isPeerBlocked)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search in chat (${filteredMessages.size} matches)") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            IconButton(onClick = { showSearchBar = false; searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            WallpaperBackground(
                wallpaperId = chat?.wallpaperId ?: preferences.wallpaperId,
                darkTheme = darkTheme,
                modifier = Modifier.fillMaxSize()
            )

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(13.dp), tint = udus.brandEmerald)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Messages & calls on udu's chat are synced privately over TLS.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if ((chat?.disappearingSeconds ?: 0L) > 0L) {
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    color = udus.brandEmerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Disappearing messages: ${formatDisappearingLabel(chat?.disappearingSeconds ?: 0L)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = udus.brandEmerald,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(filteredMessages, key = { _, m -> m.id }) { idx, msg ->
                        val prev = filteredMessages.getOrNull(idx - 1)
                        val showDateDivider = prev == null || !TimeFormatters.isSameDay(prev.createdAt, msg.createdAt)
                        if (showDateDivider) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = TimeFormatters.formatDateDivider(msg.createdAt),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (msg.type == MessageType.SYSTEM) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        } else {
                            MessageBubbleRow(
                                message = msg,
                                isOutgoing = msg.senderId == myUid,
                                isGroupChat = chat?.type == ChatType.GROUP,
                                myUid = myUid,
                                voicePlaybackState = voicePlaybackState,
                                onSwipeReply = { replyTarget = msg },
                                onLongPress = { selectedMessage = msg },
                                onQuoteClick = { quotedId ->
                                    val targetIdx = filteredMessages.indexOfFirst { it.id == quotedId }
                                    if (targetIdx >= 0) {
                                        scope.launch { listState.animateScrollToItem(targetIdx) }
                                    }
                                },
                                onImageClick = {
                                    if (msg.viewOnce) {
                                        if (myUid !in msg.viewedBy) {
                                            viewOnceImageMessage = msg
                                            onMarkViewOnceOpened(msg.id)
                                        }
                                    } else {
                                        fullScreenImageUrl = msg.mediaUrl ?: msg.mediaLocalPath
                                    }
                                },
                                onToggleVoice = { onToggleVoicePlayback(msg) },
                                onCycleVoiceSpeed = onCycleVoiceSpeed,
                                onToggleReaction = { emoji -> onToggleReaction(msg.id, emoji) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }

                if (isPeerBlocked) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chat?.otherUserId?.let { onBlockUser(it, false) } }
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "You blocked this contact. Tap to unblock.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    // Reply / Edit preview banner
                    val bannerTitle = when {
                        editingTarget != null -> "Editing message"
                        replyTarget != null -> replyTarget?.senderName ?: ""
                        else -> null
                    }
                    val bannerBody = when {
                        editingTarget != null -> editingTarget?.text.orEmpty()
                        replyTarget != null -> replyTarget?.displayPreview().orEmpty()
                        else -> null
                    }
                    if (bannerTitle != null && bannerBody != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(34.dp)
                                        .background(udus.brandEmerald, RoundedCornerShape(2.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bannerTitle, color = udus.brandEmerald, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Text(bannerBody, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { replyTarget = null; editingTarget = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                                }
                            }
                        }
                    }

                    // Composer bar
                    if (isRecordingVoice) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Recording ${TimeFormatters.formatDuration(recordingDurationMs)}",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onCancelVoiceRecording) {
                                Text("Cancel", color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    onStopAndSendVoiceNote(replyTarget)
                                    replyTarget = null
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(udus.brandEmerald)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send voice note", tint = Color.White)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    IconButton(onClick = { showEmojiTray = !showEmojiTray }) {
                                        Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = {
                                            inputText = it
                                            onSaveDraft(it)
                                        },
                                        placeholder = { Text("Message") },
                                        maxLines = 4,
                                        keyboardOptions = KeyboardOptions(
                                            imeAction = if (preferences.enterIsSend) ImeAction.Send else ImeAction.Default
                                        ),
                                        keyboardActions = KeyboardActions(onSend = { submitText() }),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { showAttachmentSheet = true }) {
                                        Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (inputText.isBlank()) {
                                        IconButton(onClick = ::launchCameraFlow) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (inputText.trim().isNotEmpty()) {
                                        submitText()
                                    } else {
                                        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        if (hasMic) onStartVoiceRecording()
                                        else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(udus.brandEmerald)
                            ) {
                                Icon(
                                    imageVector = if (inputText.trim().isNotEmpty()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                    contentDescription = if (inputText.trim().isNotEmpty()) "Send" else "Voice note",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    if (showEmojiTray) {
                        Surface(color = MaterialTheme.colorScheme.surface) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                quickEmojis.take(9).forEach { emoji ->
                                    Text(
                                        text = emoji,
                                        fontSize = 24.sp,
                                        modifier = Modifier.clickable { inputText += emoji }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Long-press message context sheet
    val ctxMsg = selectedMessage
    if (ctxMsg != null) {
        ModalBottomSheet(onDismissRequest = { selectedMessage = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    quickReactions.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier.clickable {
                                onToggleReaction(ctxMsg.id, emoji)
                                selectedMessage = null
                            }
                        )
                    }
                }
                HorizontalDivider()
                ContextActionItem(Icons.AutoMirrored.Filled.Reply, "Reply") {
                    replyTarget = ctxMsg
                    selectedMessage = null
                }
                if (ctxMsg.text.isNotBlank() && !ctxMsg.isDeletedForEveryone) {
                    ContextActionItem(Icons.Default.ContentCopy, "Copy") {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("udu_msg", ctxMsg.text))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        selectedMessage = null
                    }
                }
                ContextActionItem(Icons.AutoMirrored.Filled.Forward, "Forward") {
                    forwardSourceMessage = ctxMsg
                    selectedMessage = null
                }
                val isStarred = myUid in ctxMsg.starredBy
                ContextActionItem(Icons.Default.Star, if (isStarred) "Unstar" else "Star") {
                    onToggleStarMessage(ctxMsg.id)
                    selectedMessage = null
                }
                if (ctxMsg.senderId == myUid && ctxMsg.type == MessageType.TEXT && !ctxMsg.isDeletedForEveryone) {
                    ContextActionItem(Icons.Default.Edit, "Edit") {
                        editingTarget = ctxMsg
                        inputText = ctxMsg.text
                        selectedMessage = null
                    }
                }
                ContextActionItem(Icons.Default.Delete, "Delete for me", tint = MaterialTheme.colorScheme.error) {
                    onDeleteMessage(ctxMsg.id, false)
                    selectedMessage = null
                }
                if (ctxMsg.senderId == myUid && !ctxMsg.isDeletedForEveryone) {
                    ContextActionItem(Icons.Default.Delete, "Delete for everyone", tint = MaterialTheme.colorScheme.error) {
                        onDeleteMessage(ctxMsg.id, true)
                        selectedMessage = null
                    }
                }
            }
        }
    }

    // Attachment Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("View once (photos)", modifier = Modifier.weight(1f))
                    Switch(checked = viewOnceNextAttachment, onCheckedChange = { viewOnceNextAttachment = it })
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachmentGridItem(Icons.Default.Description, "Document", Color(0xFF7F66FF)) {
                        showAttachmentSheet = false
                        documentLauncher.launch(arrayOf("*/*"))
                    }
                    AttachmentGridItem(Icons.Default.CameraAlt, "Camera", Color(0xFFFF2E74)) {
                        showAttachmentSheet = false
                        launchCameraFlow()
                    }
                    AttachmentGridItem(Icons.Default.Image, "Gallery", Color(0xFFC861F9)) {
                        showAttachmentSheet = false
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachmentGridItem(Icons.Default.AudioFile, "Audio", Color(0xFFFA6824)) {
                        showAttachmentSheet = false
                        audioFileLauncher.launch("audio/*")
                    }
                    AttachmentGridItem(Icons.Default.LocationOn, "Location", Color(0xFF00A884)) {
                        showAttachmentSheet = false
                        showLocationPicker = true
                    }
                    AttachmentGridItem(Icons.Default.Person, "Contact", Color(0xFF009DE2)) {
                        showAttachmentSheet = false
                        showContactSharePicker = true
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // Photo caption confirmation dialog
    val previewUri = pendingPhotoPreview
    if (previewUri != null) {
        var caption by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingPhotoPreview = null },
            title = { Text(if (viewOnceNextAttachment) "Send View Once photo" else "Send photo") },
            text = {
                Column {
                    AsyncImage(
                        model = previewUri,
                        contentDescription = null,
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
                Button(onClick = {
                    onSendMessage(
                        caption,
                        MessageDraftAttachment(
                            type = MessageType.IMAGE,
                            uriOrPath = previewUri.toString(),
                            fileName = "photo.jpg",
                            mimeType = "image/jpeg",
                            viewOnce = viewOnceNextAttachment
                        ),
                        replyTarget
                    )
                    replyTarget = null
                    viewOnceNextAttachment = false
                    pendingPhotoPreview = null
                }) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPhotoPreview = null }) { Text("Cancel") }
            }
        )
    }

    if (showLocationPicker) {
        var label by remember { mutableStateOf("Dhaka, Bangladesh") }
        AlertDialog(
            onDismissRequest = { showLocationPicker = false },
            title = { Text("Share location") },
            text = {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Location label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSendMessage(
                        "",
                        MessageDraftAttachment(
                            type = MessageType.LOCATION,
                            locationLat = 23.8103,
                            locationLng = 90.4125,
                            locationLabel = label.ifBlank { "Shared location" }
                        ),
                        replyTarget
                    )
                    replyTarget = null
                    showLocationPicker = false
                }) {
                    Text("Send Pin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationPicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showContactSharePicker) {
        AlertDialog(
            onDismissRequest = { showContactSharePicker = false },
            title = { Text("Share contact") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(contacts.size) { idx ->
                        val c = contacts[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSendMessage(
                                        "",
                                        MessageDraftAttachment(
                                            type = MessageType.CONTACT,
                                            sharedContactName = c.displayName,
                                            sharedContactPhone = c.phoneNumber
                                        ),
                                        replyTarget
                                    )
                                    replyTarget = null
                                    showContactSharePicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarCircle(name = c.displayName, avatarUrl = c.avatarUrl, size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(c.displayName, fontWeight = FontWeight.SemiBold)
                                Text(PhoneNumberUtils.formatDisplayPhone(c.phoneNumber), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactSharePicker = false }) { Text("Close") }
            }
        )
    }

    // Forward Dialog
    val fwdMsg = forwardSourceMessage
    if (fwdMsg != null) {
        var targetIds by remember { mutableStateOf(setOf<String>()) }
        AlertDialog(
            onDismissRequest = { forwardSourceMessage = null },
            title = { Text("Forward to...") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(allChats.size) { idx ->
                        val c = allChats[idx]
                        val checked = c.id in targetIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetIds = if (checked) targetIds - c.id else targetIds + c.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { targetIds = if (checked) targetIds - c.id else targetIds + c.id }
                            )
                            Text(c.title)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onForwardMessages(targetIds.toList(), listOf(fwdMsg))
                        forwardSourceMessage = null
                        Toast.makeText(context, "Forwarded", Toast.LENGTH_SHORT).show()
                    },
                    enabled = targetIds.isNotEmpty()
                ) {
                    Text("Forward")
                }
            },
            dismissButton = {
                TextButton(onClick = { forwardSourceMessage = null }) { Text("Cancel") }
            }
        )
    }

    if (showDisappearingDialog) {
        val options = listOf(0L to "Off", 86400L to "24 hours", 604800L to "7 days", 7776000L to "90 days")
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing messages") },
            text = {
                Column {
                    options.forEach { (sec, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetDisappearingTimer(sec)
                                    showDisappearingDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            if ((chat?.disappearingSeconds ?: 0L) == sec) {
                                Icon(Icons.Default.Check, null, tint = udus.brandEmerald)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDisappearingDialog = false }) { Text("Close") } }
        )
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("Chat wallpaper") },
            text = {
                Column {
                    chatWallpaperPresets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetWallpaper(preset.id)
                                    showWallpaperDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (darkTheme) preset.darkStart else preset.lightStart)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(preset.label, modifier = Modifier.weight(1f))
                            if ((chat?.wallpaperId ?: preferences.wallpaperId) == preset.id) {
                                Icon(Icons.Default.Check, null, tint = udus.brandEmerald)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWallpaperDialog = false }) { Text("Close") } }
        )
    }

    if (showInfoSheet && chat != null) {
        ChatInfoBottomSheet(
            chat = chat,
            contacts = contacts,
            myUid = myUid,
            onDismiss = { showInfoSheet = false },
            onUpdateGroupInfo = onUpdateGroupInfo,
            onAddGroupMembers = onAddGroupMembers,
            onRemoveMember = onRemoveGroupMember,
            onToggleAdmin = onToggleGroupAdmin,
            onLeaveGroup = {
                showInfoSheet = false
                onLeaveGroup()
                onBack()
            }
        )
    }

    // View Once Image Dialog
    val voMsg = viewOnceImageMessage
    if (voMsg != null) {
        Dialog(
            onDismissRequest = { viewOnceImageMessage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .statusBarsPadding()
            ) {
                AsyncImage(
                    model = voMsg.mediaUrl ?: voMsg.mediaLocalPath,
                    contentDescription = "View once photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { viewOnceImageMessage = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }

    // Full-screen Media Viewer
    val fsUrl = fullScreenImageUrl
    if (fsUrl != null) {
        Dialog(
            onDismissRequest = { fullScreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .statusBarsPadding()
            ) {
                AsyncImage(
                    model = fsUrl,
                    contentDescription = "Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fullScreenImageUrl = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleRow(
    message: ChatMessage,
    isOutgoing: Boolean,
    isGroupChat: Boolean,
    myUid: String,
    voicePlaybackState: VoicePlaybackState,
    onSwipeReply: () -> Unit,
    onLongPress: () -> Unit,
    onQuoteClick: (String) -> Unit,
    onImageClick: () -> Unit,
    onToggleVoice: () -> Unit,
    onCycleVoiceSpeed: () -> Unit,
    onToggleReaction: (String) -> Unit
) {
    val udus = LocalUdusColors.current
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val bubbleColor = if (isOutgoing) udus.outgoingBubble else udus.incomingBubble
    val bubbleShape = RoundedCornerShape(
        topStart = if (isOutgoing) 14.dp else 4.dp,
        topEnd = if (isOutgoing) 4.dp else 14.dp,
        bottomStart = 14.dp,
        bottomEnd = 14.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .offset { IntOffset(dragOffset.roundToInt(), 0) }
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 70f) onSwipeReply()
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffset = (dragOffset + dragAmount).coerceIn(0f, 110f)
                    }
                )
            },
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            shadowElevation = 0.5.dp,
            modifier = Modifier
                .widthIn(min = 96.dp, max = 310.dp)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                if (isGroupChat && !isOutgoing) {
                    val senderColor = groupSenderColors[abs(message.senderId.hashCode()) % groupSenderColors.size]
                    Text(
                        text = message.senderName,
                        color = senderColor,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (message.isForwarded) {
                    Text(
                        text = "Forwarded",
                        style = MaterialTheme.typography.labelMedium,
                        fontStyle = FontStyle.Italic,
                        color = udus.tickMuted
                    )
                }

                // Quoted reply preview
                if (!message.replyToPreview.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(udus.quoteBackground)
                            .clickable { message.replyToMessageId?.let(onQuoteClick) }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(30.dp)
                                .background(udus.quoteBar, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = message.replyToSenderName.orEmpty(),
                                color = udus.quoteBar,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = message.replyToPreview.orEmpty(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                if (message.isDeletedForEveryone) {
                    Text(
                        text = "This message was deleted",
                        fontStyle = FontStyle.Italic,
                        color = udus.tickMuted
                    )
                } else {
                    when (message.type) {
                        MessageType.IMAGE -> {
                            if (message.viewOnce) {
                                val alreadyOpened = myUid in message.viewedBy
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(udus.quoteBackground)
                                        .clickable(enabled = !alreadyOpened, onClick = onImageClick)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Visibility, null, tint = udus.brandEmerald)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (alreadyOpened) "Opened" else "View once photo",
                                        fontStyle = if (alreadyOpened) FontStyle.Italic else FontStyle.Normal,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = message.mediaUrl ?: message.mediaLocalPath,
                                    contentDescription = "Photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(onClick = onImageClick),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (message.text.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        MessageType.AUDIO -> {
                            val isActive = voicePlaybackState.activeMessageId == message.id
                            val isPlaying = isActive && voicePlaybackState.isPlaying
                            val dur = if (isActive && voicePlaybackState.durationMs > 0) voicePlaybackState.durationMs else message.mediaDurationMs.coerceAtLeast(1000L)
                            val pos = if (isActive) voicePlaybackState.positionMs else 0L
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.widthIn(min = 210.dp)
                            ) {
                                IconButton(onClick = onToggleVoice, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play voice note",
                                        tint = udus.brandEmerald
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = { (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) },
                                        color = udus.brandEmerald,
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = TimeFormatters.formatDuration(if (isActive) pos else dur),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = udus.tickMuted,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isActive) {
                                            Text(
                                                text = "${voicePlaybackState.speed}x",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = udus.brandEmerald,
                                                modifier = Modifier.clickable(onClick = onCycleVoiceSpeed)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        MessageType.DOCUMENT -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(udus.quoteBackground)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, null, tint = udus.brandEmerald)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(message.fileName ?: "Document", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (message.fileSizeBytes > 0) {
                                        Text(TimeFormatters.formatFileSize(message.fileSizeBytes), style = MaterialTheme.typography.labelMedium, color = udus.tickMuted)
                                    }
                                }
                            }
                        }

                        MessageType.LOCATION -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(udus.quoteBackground)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = Color(0xFFE53935), modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(message.locationLabel ?: "Shared Location", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${message.locationLat ?: 0.0}, ${message.locationLng ?: 0.0}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = udus.tickMuted
                                    )
                                }
                            }
                        }

                        MessageType.CONTACT -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(udus.quoteBackground)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarCircle(name = message.sharedContactName ?: "C", avatarUrl = null, size = 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(message.sharedContactName ?: "Contact", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        PhoneNumberUtils.formatDisplayPhone(message.sharedContactPhone.orEmpty()),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = udus.tickMuted
                                    )
                                }
                            }
                        }

                        else -> {
                            Text(message.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (myUid in message.starredBy) {
                        Icon(Icons.Default.Star, null, tint = udus.tickMuted, modifier = Modifier.size(12.dp))
                    }
                    if (message.isEdited) {
                        Text("edited", style = MaterialTheme.typography.labelMedium, fontSize = 10.sp, color = udus.tickMuted)
                    }
                    Text(
                        text = TimeFormatters.formatMessageTime(message.createdAt),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        color = udus.tickMuted
                    )
                    if (isOutgoing) {
                        MessageTicksIcon(status = message.status)
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            val grouped = message.reactions.values.groupingBy { it }.eachCount()
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    grouped.forEach { (emoji, count) ->
                        Text(
                            text = if (count > 1) "$emoji $count" else emoji,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onToggleReaction(emoji) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextActionItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AttachmentGridItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInfoBottomSheet(
    chat: ChatThread,
    contacts: List<UserProfile>,
    myUid: String,
    onDismiss: () -> Unit,
    onUpdateGroupInfo: (String, String) -> Unit,
    onAddGroupMembers: (List<String>) -> Unit,
    onRemoveMember: (String) -> Unit,
    onToggleAdmin: (String) -> Unit,
    onLeaveGroup: () -> Unit
) {
    var editingGroup by remember { mutableStateOf(false) }
    var groupTitle by remember(chat.title) { mutableStateOf(chat.title) }
    var groupDesc by remember(chat.description) { mutableStateOf(chat.description) }
    var showAddMember by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarCircle(
                name = chat.title,
                avatarUrl = chat.avatarUrl,
                size = 76.dp,
                isGroup = chat.type == ChatType.GROUP
            )
            Spacer(Modifier.height(10.dp))
            Text(chat.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (chat.type == ChatType.DIRECT && !chat.otherUserPhone.isNullOrBlank()) {
                Text(
                    PhoneNumberUtils.formatDisplayPhone(chat.otherUserPhone),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (chat.type == ChatType.GROUP) {
                Text("Group • ${chat.participantIds.size} participants", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (chat.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(chat.description, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (chat.type == ChatType.GROUP) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { editingGroup = !editingGroup }) {
                        Text("Edit group")
                    }
                    OutlinedButton(onClick = { showAddMember = true }) {
                        Text("Add member")
                    }
                }
                if (editingGroup) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = groupTitle, onValueChange = { groupTitle = it }, label = { Text("Group name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = groupDesc, onValueChange = { groupDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onUpdateGroupInfo(groupTitle, groupDesc); editingGroup = false }) { Text("Save") }
                }

                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp).fillMaxWidth()) {
                    items(chat.participantIds.size) { idx ->
                        val uid = chat.participantIds[idx]
                        val user = contacts.firstOrNull { it.uid == uid }
                        val name = if (uid == myUid) "You" else user?.displayName ?: uid
                        val isAdmin = uid in chat.adminIds
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarCircle(name = name, avatarUrl = user?.avatarUrl, size = 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Medium)
                                if (isAdmin) Text("Group admin", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            if (uid != myUid && myUid in chat.adminIds) {
                                TextButton(onClick = { onToggleAdmin(uid) }) {
                                    Text(if (isAdmin) "Dismiss" else "Make admin")
                                }
                                TextButton(onClick = { onRemoveMember(uid) }) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onLeaveGroup) {
                    Text("Exit group", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAddMember) {
        val candidates = contacts.filter { it.uid !in chat.participantIds }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Add participants") },
            text = {
                if (candidates.isEmpty()) {
                    Text("All your contacts are already in this group.")
                } else {
                    Column {
                        candidates.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddGroupMembers(listOf(user.uid))
                                        showAddMember = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(user.displayName)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddMember = false }) { Text("Close") } }
        )
    }
}

private fun formatDisappearingLabel(seconds: Long): String = when (seconds) {
    86400L -> "24 hours"
    604800L -> "7 days"
    7776000L -> "90 days"
    else -> "Off"
}

private fun queryUriMetadata(context: Context, uri: Uri): Pair<String, Long> {
    var name = "Attachment"
    var size = 0L
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
    }
    return name to size
}
