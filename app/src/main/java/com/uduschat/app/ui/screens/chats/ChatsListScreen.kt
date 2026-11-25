package com.uduschat.app.ui.screens.chats

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.uduschat.app.core.PhoneNumberUtils
import com.uduschat.app.core.TimeFormatters
import com.uduschat.app.domain.model.ChatFilter
import com.uduschat.app.domain.model.ChatThread
import com.uduschat.app.domain.model.ChatType
import com.uduschat.app.domain.model.UserProfile
import com.uduschat.app.ui.components.AvatarCircle
import com.uduschat.app.ui.components.CloudStatusPill
import com.uduschat.app.ui.components.CountryPickerDialog
import com.uduschat.app.ui.components.MessageTicksIcon
import com.uduschat.app.ui.theme.LocalUdusColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTabScreen(
    chats: List<ChatThread>,
    contacts: List<UserProfile>,
    currentUserUid: String,
    isFirebaseConfigured: Boolean,
    onOpenChat: (String) -> Unit,
    onOpenArchived: () -> Unit,
    onOpenStarred: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onToggleArchive: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMarkUnread: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onFindAndOpenByPhone: suspend (String, com.uduschat.app.core.CountryCode) -> Result<String>,
    onOpenDirectChat: suspend (UserProfile) -> Result<String>,
    onCreateGroup: suspend (String, String, List<String>) -> Result<String>,
    onSendBroadcast: suspend (List<String>, String) -> Result<Int>,
    onSyncDeviceContacts: (com.uduschat.app.core.CountryCode, (Int) -> Unit) -> Unit,
    onSimulateIncomingCall: () -> Unit
) {
    val udus = LocalUdusColors.current
    val scope = rememberCoroutineScope()

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(ChatFilter.ALL) }
    var menuExpanded by remember { mutableStateOf(false) }
    var actionTargetChat by remember { mutableStateOf<ChatThread?>(null) }

    var showNewChatSheet by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }

    val archivedCount = remember(chats) { chats.count { it.isArchived } }
    val visibleChats = remember(chats, searchQuery, activeFilter) {
        chats.filter { !it.isArchived }
            .filter { chat ->
                when (activeFilter) {
                    ChatFilter.ALL -> true
                    ChatFilter.UNREAD -> chat.unreadCount > 0
                    ChatFilter.FAVORITES -> chat.isFavorite
                    ChatFilter.GROUPS -> chat.type == ChatType.GROUP
                }
            }
            .filter { chat ->
                val q = searchQuery.trim().lowercase()
                q.isEmpty() ||
                    chat.title.lowercase().contains(q) ||
                    chat.lastMessagePreview.lowercase().contains(q) ||
                    (chat.otherUserPhone?.contains(q) == true)
            }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "udu's chat",
                                color = udus.brandEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            CloudStatusPill(isFirebaseConfigured = isFirebaseConfigured)
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchVisible = !searchVisible; if (!searchVisible) searchQuery = "" }) {
                            Icon(Icons.Default.Search, contentDescription = "Search chats")
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("New group") },
                                    leadingIcon = { Icon(Icons.Default.GroupAdd, null) },
                                    onClick = { menuExpanded = false; showCreateGroupDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("New broadcast") },
                                    leadingIcon = { Icon(Icons.Default.Campaign, null) },
                                    onClick = { menuExpanded = false; showBroadcastDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Starred messages") },
                                    leadingIcon = { Icon(Icons.Default.Star, null) },
                                    onClick = { menuExpanded = false; onOpenStarred() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Archived ($archivedCount)") },
                                    leadingIcon = { Icon(Icons.Default.Archive, null) },
                                    onClick = { menuExpanded = false; onOpenArchived() }
                                )
                                if (!isFirebaseConfigured) {
                                    DropdownMenuItem(
                                        text = { Text("Test incoming call") },
                                        onClick = { menuExpanded = false; onSimulateIncomingCall() }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = { menuExpanded = false; onOpenSettings() }
                                )
                            }
                        }
                    }
                )
                if (searchVisible) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name, phone or message...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatSheet = true },
                containerColor = udus.brandEmerald,
                contentColor = Color.White
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "New chat")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChatFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }

            if (archivedCount > 0 && activeFilter == ChatFilter.ALL && searchQuery.isBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenArchived)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = udus.brandEmerald
                        )
                        Spacer(Modifier.width(18.dp))
                        Text(
                            text = "Archived",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = archivedCount.toString(),
                            color = udus.brandEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }
            }

            val queryDigits = searchQuery.filter { it.isDigit() }.trimStart('0')
            val matchingUsers = if (searchQuery.isBlank()) {
                emptyList()
            } else {
                contacts.filter { u ->
                    u.displayName.contains(searchQuery, ignoreCase = true) ||
                        u.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                        (queryDigits.length >= 3 && u.phoneNumber.filter { it.isDigit() }.contains(queryDigits))
                }
            }

            if (visibleChats.isEmpty() && matchingUsers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No conversations found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap the green chat button to connect any valid international phone number.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(visibleChats, key = { it.id }) { chat ->
                    ChatThreadRow(
                        chat = chat,
                        currentUserUid = currentUserUid,
                        onClick = { onOpenChat(chat.id) },
                        onLongClick = { actionTargetChat = chat }
                    )
                }
                if (matchingUsers.isNotEmpty()) {
                    item {
                        Text(
                            text = "Registered on udu's chat",
                            style = MaterialTheme.typography.labelMedium,
                            color = udus.brandEmerald,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(matchingUsers, key = { "search_${it.uid}" }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        onOpenDirectChat(user).onSuccess { chatId -> onOpenChat(chatId) }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarCircle(
                                name = user.displayName,
                                avatarUrl = user.avatarUrl,
                                size = 48.dp,
                                showOnlineDot = user.isOnline
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = PhoneNumberUtils.formatDisplayPhone(user.phoneNumber),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(84.dp)) }
        }
    }

    // Long-press thread action sheet
    val target = actionTargetChat
    if (target != null) {
        ModalBottomSheet(onDismissRequest = { actionTargetChat = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = target.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                SheetActionRow(
                    icon = Icons.Default.PushPin,
                    label = if (target.isPinned) "Unpin chat" else "Pin chat",
                    onClick = { onTogglePin(target.id); actionTargetChat = null }
                )
                SheetActionRow(
                    icon = Icons.Default.NotificationsOff,
                    label = if (target.isMuted) "Unmute notifications" else "Mute notifications",
                    onClick = { onToggleMute(target.id); actionTargetChat = null }
                )
                SheetActionRow(
                    icon = Icons.Default.Favorite,
                    label = if (target.isFavorite) "Remove from Favorites" else "Add to Favorites",
                    onClick = { onToggleFavorite(target.id); actionTargetChat = null }
                )
                SheetActionRow(
                    icon = Icons.Default.Archive,
                    label = if (target.isArchived) "Unarchive chat" else "Archive chat",
                    onClick = { onToggleArchive(target.id); actionTargetChat = null }
                )
                SheetActionRow(
                    icon = Icons.Default.MarkChatUnread,
                    label = "Mark as unread",
                    onClick = { onMarkUnread(target.id); actionTargetChat = null }
                )
                SheetActionRow(
                    icon = Icons.Default.Delete,
                    label = "Delete chat",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { onDeleteChat(target.id); actionTargetChat = null }
                )
            }
        }
    }

    if (showNewChatSheet) {
        NewChatBottomSheet(
            contacts = contacts,
            onDismiss = { showNewChatSheet = false },
            onOpenByPhone = { raw, country ->
                val res = onFindAndOpenByPhone(raw, country)
                res.onSuccess { chatId ->
                    showNewChatSheet = false
                    onOpenChat(chatId)
                }
                res
            },
            onSelectContact = { user ->
                scope.launch {
                    onOpenDirectChat(user).onSuccess { chatId ->
                        showNewChatSheet = false
                        onOpenChat(chatId)
                    }
                }
            },
            onCreateGroupClick = {
                showNewChatSheet = false
                showCreateGroupDialog = true
            },
            onSyncContacts = onSyncDeviceContacts
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            contacts = contacts,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { title, desc, members ->
                scope.launch {
                    onCreateGroup(title, desc, members).onSuccess { chatId ->
                        showCreateGroupDialog = false
                        onOpenChat(chatId)
                    }
                }
            }
        )
    }

    if (showBroadcastDialog) {
        BroadcastDialog(
            contacts = contacts,
            onDismiss = { showBroadcastDialog = false },
            onSend = { recipients, text ->
                scope.launch {
                    onSendBroadcast(recipients, text)
                    showBroadcastDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatThreadRow(
    chat: ChatThread,
    currentUserUid: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val udus = LocalUdusColors.current
    val isTyping = chat.typingUserIds.isNotEmpty()
    val hasUnread = chat.unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(
            name = chat.title,
            avatarUrl = chat.avatarUrl,
            size = 52.dp,
            isGroup = chat.type == ChatType.GROUP,
            showOnlineDot = chat.type == ChatType.DIRECT && chat.isOnline
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = TimeFormatters.formatChatListTime(chat.lastMessageTime),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hasUnread) udus.unreadBadge else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isTyping -> {
                        Text(
                            text = "typing...",
                            color = udus.brandEmerald,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    chat.draftText.isNotBlank() -> {
                        Text(
                            text = "Draft: ${chat.draftText}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    else -> {
                        if (chat.lastMessageSenderId == currentUserUid && chat.lastMessagePreview.isNotBlank()) {
                            MessageTicksIcon(status = chat.lastMessageStatus)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = chat.lastMessagePreview.ifBlank { "Tap to start chatting" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (chat.isMuted) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = "Muted",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (chat.isPinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (hasUnread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(udus.unreadBadge)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatBottomSheet(
    contacts: List<UserProfile>,
    onDismiss: () -> Unit,
    onOpenByPhone: suspend (String, com.uduschat.app.core.CountryCode) -> Result<String>,
    onSelectContact: (UserProfile) -> Unit,
    onCreateGroupClick: () -> Unit,
    onSyncContacts: (com.uduschat.app.core.CountryCode, (Int) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var country by remember { mutableStateOf(PhoneNumberUtils.defaultCountry) }
    var rawPhone by remember { mutableStateOf("") }
    var showCountryDialog by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var syncStatus by remember { mutableStateOf<String?>(null) }

    val contactPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSyncContacts(country) { count ->
                syncStatus = "Synced $count contacts on udu's chat"
            }
        } else {
            syncStatus = "Contacts permission denied. Enter any phone number directly above."
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Connect by phone number", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Enter any valid phone number worldwide to start chatting over the internet.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showCountryDialog = true }) {
                    Text("${country.flag} ${country.dialCode}")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = rawPhone,
                    onValueChange = { rawPhone = it; phoneError = null },
                    placeholder = { Text(country.exampleLocal) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            onOpenByPhone(rawPhone, country).onFailure { err ->
                                phoneError = err.message
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Connect")
                }
            }

            if (!phoneError.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(phoneError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCreateGroupClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.GroupAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New group")
                }
                OutlinedButton(
                    onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            onSyncContacts(country) { count -> syncStatus = "Synced $count contacts" }
                        } else {
                            contactPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Contacts, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sync phone")
                }
            }

            if (!syncStatus.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(syncStatus.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(14.dp))
            Text("Contacts on udu's chat", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))

            val filteredSheetContacts = remember(contacts, rawPhone) {
                if (rawPhone.isBlank()) contacts
                else {
                    val d = rawPhone.filter { it.isDigit() }.trimStart('0')
                    contacts.filter { u ->
                        u.displayName.contains(rawPhone, ignoreCase = true) ||
                            u.phoneNumber.contains(rawPhone, ignoreCase = true) ||
                            (d.isNotEmpty() && u.phoneNumber.filter { it.isDigit() }.contains(d))
                    }
                }
            }

            LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                items(filteredSheetContacts, key = { it.uid }) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectContact(user) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarCircle(name = user.displayName, avatarUrl = user.avatarUrl, size = 44.dp, showOnlineDot = user.isOnline)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayName, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${PhoneNumberUtils.formatDisplayPhone(user.phoneNumber)} • ${user.about}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCountryDialog) {
        CountryPickerDialog(
            selectedCountry = country,
            onSelect = { country = it; showCountryDialog = false },
            onDismiss = { showCountryDialog = false }
        )
    }
}

@Composable
fun CreateGroupDialog(
    contacts: List<UserProfile>,
    onDismiss: () -> Unit,
    onCreate: (title: String, desc: String, memberUids: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedUids by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New group") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Group description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Select participants (${selectedUids.size}):", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(contacts, key = { it.uid }) { user ->
                        val checked = user.uid in selectedUids
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedUids = if (checked) selectedUids - user.uid else selectedUids + user.uid
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedUids = if (checked) selectedUids - user.uid else selectedUids + user.uid
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(user.displayName, fontWeight = FontWeight.Medium)
                                Text(
                                    PhoneNumberUtils.formatDisplayPhone(user.phoneNumber),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description, selectedUids.toList()) },
                enabled = title.trim().isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BroadcastDialog(
    contacts: List<UserProfile>,
    onDismiss: () -> Unit,
    onSend: (recipientUids: List<String>, message: String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var selectedUids by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New broadcast") },
        text = {
            Column {
                Text(
                    "Send one message individually to multiple contacts.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Broadcast message") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(contacts, key = { it.uid }) { user ->
                        val checked = user.uid in selectedUids
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedUids = if (checked) selectedUids - user.uid else selectedUids + user.uid
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedUids = if (checked) selectedUids - user.uid else selectedUids + user.uid
                                }
                            )
                            Text(user.displayName)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(selectedUids.toList(), message) },
                enabled = message.trim().isNotEmpty() && selectedUids.isNotEmpty()
            ) {
                Text("Send (${selectedUids.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
