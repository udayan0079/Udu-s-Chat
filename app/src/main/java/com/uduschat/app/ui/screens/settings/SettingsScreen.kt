package com.uduschat.app.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uduschat.app.core.CloudConfigStatus
import com.uduschat.app.core.PhoneNumberUtils
import com.uduschat.app.core.TimeFormatters
import com.uduschat.app.domain.model.AppPreferences
import com.uduschat.app.domain.model.ChatMessage
import com.uduschat.app.domain.model.ChatThread
import com.uduschat.app.domain.model.FontScaleOption
import com.uduschat.app.domain.model.PrivacySettings
import com.uduschat.app.domain.model.ThemeMode
import com.uduschat.app.domain.model.UserProfile
import com.uduschat.app.domain.model.VisibilityOption
import com.uduschat.app.ui.components.AvatarCircle
import com.uduschat.app.ui.components.CloudStatusPill
import com.uduschat.app.ui.components.chatWallpaperPresets
import com.uduschat.app.ui.screens.chats.ChatThreadRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabScreen(
    currentUser: UserProfile?,
    preferences: AppPreferences,
    cloudStatus: CloudConfigStatus,
    contacts: List<UserProfile>,
    onUpdateProfile: (name: String, about: String, avatarUri: String?) -> Unit,
    onUpdatePreferences: ((AppPreferences) -> AppPreferences) -> Unit,
    onUpdatePrivacy: ((PrivacySettings) -> PrivacySettings) -> Unit,
    onUnblockUser: (String) -> Unit,
    onOpenStarredMessages: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showChatsPrefsDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showCloudGuideDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }
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
                        .clickable { showProfileDialog = true }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarCircle(
                        name = currentUser?.displayName ?: "Udu User",
                        avatarUrl = currentUser?.avatarUrl,
                        size = 64.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: "Udu User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.about ?: "Hey there! I am using udu's chat.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!currentUser?.phoneNumber.isNullOrBlank()) {
                            Text(
                                text = PhoneNumberUtils.formatDisplayPhone(currentUser?.phoneNumber.orEmpty()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { showQrDialog = true }) {
                        Icon(Icons.Default.QrCode2, contentDescription = "Phone QR", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Cloud status card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { showCloudGuideDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Global Network & Firebase Status", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            CloudStatusPill(isFirebaseConfigured = cloudStatus.isFirebaseConfigured)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (cloudStatus.isFirebaseConfigured) {
                                "Project: ${cloudStatus.projectId} • Real global OTP & Firestore active."
                            } else {
                                "Tap to see how to attach google-services.json for global SMS OTP & live internet sync."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                SettingsRow(Icons.Default.Key, "Account", "Two-step verification, delete account") { showAccountDialog = true }
                SettingsRow(Icons.Default.Lock, "Privacy", "Last seen, read receipts, blocked contacts") { showPrivacyDialog = true }
                SettingsRow(Icons.Default.Chat, "Chats", "Theme, wallpapers, font size, enter is send") { showChatsPrefsDialog = true }
                SettingsRow(Icons.Default.Notifications, "Notifications", "Message alerts & vibration") { showNotificationsDialog = true }
                SettingsRow(Icons.Default.Storage, "Storage and data", "Network auto-download preferences") { showStorageDialog = true }
                SettingsRow(Icons.Default.Star, "Starred messages", "View saved important messages", onOpenStarredMessages)
                SettingsRow(Icons.AutoMirrored.Filled.Logout, "Sign out", "Disconnect current session", onSignOut)
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showProfileDialog && currentUser != null) {
        var name by remember { mutableStateOf(currentUser.displayName) }
        var about by remember { mutableStateOf(currentUser.about) }
        var pickedAvatar by remember { mutableStateOf<Uri?>(null) }
        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) pickedAvatar = uri
        }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AvatarCircle(
                        name = name,
                        avatarUrl = pickedAvatar?.toString() ?: currentUser.avatarUrl,
                        size = 76.dp,
                        modifier = Modifier.clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                    TextButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Change photo") }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = about, onValueChange = { about = it }, label = { Text("About") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateProfile(name, about, pickedAvatar?.toString())
                    showProfileDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("Cancel") } }
        )
    }

    if (showQrDialog && currentUser != null) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("Your Global udu's chat ID") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(currentUser.displayName, fontWeight = FontWeight.Bold)
                    Text(PhoneNumberUtils.formatDisplayPhone(currentUser.phoneNumber), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Anyone on udu's chat anywhere in the world can connect using this phone number.",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showQrDialog = false }) { Text("Close") } }
        )
    }

    if (showAccountDialog) {
        var newPin by remember { mutableStateOf(preferences.twoStepPin.orEmpty()) }
        var confirmDelete by remember { mutableStateOf(false) }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Account & Security") },
            text = {
                Column {
                    Text("Two-step verification (6-digit app PIN)", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("6-digit PIN (blank to disable)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newPin.isEmpty() || newPin.length == 6) {
                                onUpdatePreferences { it.copy(twoStepPin = newPin.ifBlank { null }) }
                                Toast.makeText(context, if (newPin.isEmpty()) "PIN disabled" else "PIN saved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "PIN must be 6 digits", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (newPin.isEmpty()) "Disable PIN" else "Save PIN")
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    if (!confirmDelete) {
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteForever, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Delete account")
                        }
                    } else {
                        Button(
                            onClick = {
                                showAccountDialog = false
                                onDeleteAccount()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm Permanent Delete")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAccountDialog = false }) { Text("Close") } }
        )
    }

    if (showPrivacyDialog && currentUser != null) {
        val privacy = currentUser.privacy
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Read receipts", fontWeight = FontWeight.Medium)
                            Text("Show blue ticks when you read messages", style = MaterialTheme.typography.labelMedium)
                        }
                        Switch(
                            checked = privacy.readReceiptsEnabled,
                            onCheckedChange = { enabled ->
                                onUpdatePrivacy { it.copy(readReceiptsEnabled = enabled) }
                            }
                        )
                    }
                    Text("Last seen & online", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VisibilityOption.entries.forEach { opt ->
                            OutlinedButton(
                                onClick = { onUpdatePrivacy { it.copy(lastSeenVisibility = opt) } }
                            ) {
                                Text(
                                    opt.label,
                                    color = if (privacy.lastSeenVisibility == opt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (privacy.blockedUids.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Blocked contacts (${privacy.blockedUids.size})", fontWeight = FontWeight.Medium)
                        privacy.blockedUids.forEach { uid ->
                            val name = contacts.firstOrNull { it.uid == uid }?.displayName ?: uid
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(name, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onUnblockUser(uid) }) { Text("Unblock") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Done") } }
        )
    }

    if (showChatsPrefsDialog) {
        AlertDialog(
            onDismissRequest = { showChatsPrefsDialog = false },
            title = { Text("Chats Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme", fontWeight = FontWeight.SemiBold)
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onUpdatePreferences { it.copy(themeMode = mode) }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.themeMode == mode,
                                onClick = { onUpdatePreferences { it.copy(themeMode = mode) } }
                            )
                            Text(mode.label)
                        }
                    }
                    HorizontalDivider()
                    Text("Font size", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FontScaleOption.entries.forEach { opt ->
                            OutlinedButton(onClick = { onUpdatePreferences { it.copy(fontScale = opt) } }) {
                                Text(
                                    opt.label,
                                    color = if (preferences.fontScale == opt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    Text("Default wallpaper", fontWeight = FontWeight.SemiBold)
                    chatWallpaperPresets.forEach { preset ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onUpdatePreferences { it.copy(wallpaperId = preset.id) }
                            }.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.wallpaperId == preset.id,
                                onClick = { onUpdatePreferences { it.copy(wallpaperId = preset.id) } }
                            )
                            Text(preset.label)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enter key sends message", modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.enterIsSend,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(enterIsSend = v) } }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showChatsPrefsDialog = false }) { Text("Close") } }
        )
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Notifications") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("High priority notifications", modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.highPriorityNotifications,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(highPriorityNotifications = v) } }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibrate on message", modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.vibrateOnMessage,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(vibrateOnMessage = v) } }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotificationsDialog = false }) { Text("Done") } }
        )
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Storage & Data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-download media on Wi-Fi", modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.mediaAutoDownloadWifi,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(mediaAutoDownloadWifi = v) } }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-download media on Cellular", modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.mediaAutoDownloadCellular,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(mediaAutoDownloadCellular = v) } }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStorageDialog = false }) { Text("Done") } }
        )
    }

    if (showCloudGuideDialog) {
        AlertDialog(
            onDismissRequest = { showCloudGuideDialog = false },
            title = { Text("Global Internet Cloud Setup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = if (cloudStatus.isFirebaseConfigured) Color(0xFF25D366) else Color(0xFFF57C00)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(cloudStatus.modeLabel, fontWeight = FontWeight.Bold)
                    }
                    Text("To enable real global SMS OTP login and cross-device internet messaging from anywhere in the world:")
                    Text("1. Create a Firebase project & register Android app package: com.uduschat.app")
                    Text("2. Enable Authentication -> Sign-in method -> Phone.")
                    Text("3. Create Cloud Firestore & Storage, then deploy firebase/firestore.rules and firebase/storage.rules.")
                    Text("4. Download google-services.json and place it at app/google-services.json, then rebuild.")
                }
            },
            confirmButton = { TextButton(onClick = { showCloudGuideDialog = false }) { Text("Got it") } }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(
    chats: List<ChatThread>,
    currentUserUid: String,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onUnarchive: (String) -> Unit
) {
    val archived = remember(chats) { chats.filter { it.isArchived } }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Archived (${archived.size})") }
            )
        }
    ) { padding ->
        if (archived.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No archived chats")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(archived, key = { it.id }) { chat ->
                    ChatThreadRow(
                        chat = chat,
                        currentUserUid = currentUserUid,
                        onClick = { onOpenChat(chat.id) },
                        onLongClick = { onUnarchive(chat.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredMessagesScreen(
    starredMessages: List<ChatMessage>,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onUnstar: (chatId: String, messageId: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Starred messages") }
            )
        }
    ) { padding ->
        if (starredMessages.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
                Text("No starred messages yet")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
                items(starredMessages, key = { it.id }) { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { onOpenChat(msg.chatId) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(msg.senderName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text(msg.displayPreview(), style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    TimeFormatters.formatRelativeShort(msg.createdAt),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onUnstar(msg.chatId, msg.id) }) {
                                Icon(Icons.Default.Star, contentDescription = "Unstar", tint = Color(0xFFFFB300))
                            }
                        }
                    }
                }
            }
        }
    }
}
