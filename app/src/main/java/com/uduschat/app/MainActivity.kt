package com.uduschat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.uduschat.app.domain.model.AuthStep
import com.uduschat.app.domain.model.CallType
import com.uduschat.app.domain.model.ThemeMode
import com.uduschat.app.domain.model.UserStatusGroup
import com.uduschat.app.ui.UdusViewModel
import com.uduschat.app.ui.screens.auth.OtpVerifyScreen
import com.uduschat.app.ui.screens.auth.PhoneInputScreen
import com.uduschat.app.ui.screens.auth.ProfileSetupScreen
import com.uduschat.app.ui.screens.auth.TwoStepPinLockScreen
import com.uduschat.app.ui.screens.auth.WelcomeScreen
import com.uduschat.app.ui.screens.calls.ActiveCallOverlay
import com.uduschat.app.ui.screens.calls.CallsTabScreen
import com.uduschat.app.ui.screens.chats.ChatDetailScreen
import com.uduschat.app.ui.screens.chats.ChatsTabScreen
import com.uduschat.app.ui.screens.settings.ArchivedChatsScreen
import com.uduschat.app.ui.screens.settings.SettingsTabScreen
import com.uduschat.app.ui.screens.settings.StarredMessagesScreen
import com.uduschat.app.ui.screens.updates.StatusViewerOverlay
import com.uduschat.app.ui.screens.updates.UpdatesTabScreen
import com.uduschat.app.ui.theme.UdusChatTheme

private enum class HomeTab(val label: String) {
    CHATS("Chats"),
    UPDATES("Updates"),
    CALLS("Calls"),
    SETTINGS("Settings")
}

private sealed interface SubScreen {
    data object None : SubScreen
    data class ChatDetail(val chatId: String) : SubScreen
    data object Archived : SubScreen
    data object Starred : SubScreen
}

class MainActivity : ComponentActivity() {

    private val viewModel: UdusViewModel by viewModels {
        val app = application as UdusChatApplication
        UdusViewModel.Factory(app.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UdusChatAppRoot(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setOnlinePresence(true)
    }

    override fun onPause() {
        super.onPause()
        viewModel.setOnlinePresence(false)
    }
}

@Composable
fun UdusChatAppRoot(viewModel: UdusViewModel) {
    val authState by viewModel.authState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val statuses by viewModel.statuses.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val starredMessages by viewModel.starredMessages.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
    val voicePlaybackState by viewModel.voicePlaybackState.collectAsState()

    val darkTheme = when (preferences.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    var pinUnlockedThisSession by remember { mutableStateOf(false) }

    UdusChatTheme(
        darkTheme = darkTheme,
        fontScale = preferences.fontScale.scale
    ) {
        val requiredPin = preferences.twoStepPin
        if (authState.step != AuthStep.AUTHENTICATED) {
            when (authState.step) {
                AuthStep.WELCOME -> WelcomeScreen(
                    isFirebaseConfigured = viewModel.cloudStatus.isFirebaseConfigured,
                    onContinue = viewModel::proceedFromWelcome
                )
                AuthStep.PHONE_INPUT -> PhoneInputScreen(
                    authState = authState,
                    isFirebaseConfigured = viewModel.cloudStatus.isFirebaseConfigured,
                    onSendOtp = viewModel::sendOtp,
                    onClearError = viewModel::clearAuthError
                )
                AuthStep.OTP_VERIFY -> OtpVerifyScreen(
                    authState = authState,
                    onVerifyOtp = viewModel::verifyOtp,
                    onResendOtp = viewModel::resendOtp,
                    onEditNumber = viewModel::backToPhoneInput
                )
                AuthStep.PROFILE_SETUP -> ProfileSetupScreen(
                    phoneNumber = authState.pendingPhoneE164,
                    isLoading = authState.isLoading,
                    onComplete = viewModel::completeProfileSetup
                )
                AuthStep.AUTHENTICATED -> Unit
            }
        } else if (!requiredPin.isNullOrBlank() && !pinUnlockedThisSession) {
            TwoStepPinLockScreen(
                expectedPin = requiredPin,
                onUnlocked = { pinUnlockedThisSession = true }
            )
        } else {
            var activeTab by rememberSaveable { mutableStateOf(HomeTab.CHATS) }
            var subScreen by remember { mutableStateOf<SubScreen>(SubScreen.None) }
            var viewingStatusGroup by remember { mutableStateOf<UserStatusGroup?>(null) }

            BackHandler(enabled = subScreen != SubScreen.None || viewingStatusGroup != null) {
                when {
                    viewingStatusGroup != null -> viewingStatusGroup = null
                    subScreen != SubScreen.None -> subScreen = SubScreen.None
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val currentSub = subScreen) {
                    is SubScreen.ChatDetail -> {
                        val chat by viewModel.observeChat(currentSub.chatId).collectAsState(
                            initial = chats.firstOrNull { it.id == currentSub.chatId }
                        )
                        val messages by viewModel.observeMessages(currentSub.chatId).collectAsState(initial = emptyList())
                        ChatDetailScreen(
                            chat = chat,
                            messages = messages,
                            allChats = chats,
                            contacts = contacts,
                            currentUser = currentUser,
                            preferences = preferences,
                            isRecordingVoice = isRecordingVoice,
                            recordingDurationMs = recordingDurationMs,
                            voicePlaybackState = voicePlaybackState,
                            onBack = { subScreen = SubScreen.None },
                            onMarkRead = { viewModel.markChatAsRead(currentSub.chatId) },
                            onSendMessage = { txt, att, reply -> viewModel.sendMessage(currentSub.chatId, txt, att, reply) },
                            onSaveDraft = { draft -> viewModel.saveDraft(currentSub.chatId, draft) },
                            onStartVoiceRecording = viewModel::startVoiceRecording,
                            onStopAndSendVoiceNote = { reply -> viewModel.stopAndSendVoiceNote(currentSub.chatId, reply) },
                            onCancelVoiceRecording = viewModel::cancelVoiceRecording,
                            onToggleVoicePlayback = viewModel::toggleVoicePlayback,
                            onCycleVoiceSpeed = viewModel::cycleVoiceSpeed,
                            onToggleReaction = { msgId, emoji -> viewModel.toggleReaction(currentSub.chatId, msgId, emoji) },
                            onToggleStarMessage = { msgId -> viewModel.toggleStarMessage(currentSub.chatId, msgId) },
                            onEditMessage = { msgId, newText -> viewModel.editMessage(currentSub.chatId, msgId, newText) },
                            onDeleteMessage = { msgId, forAll -> viewModel.deleteMessage(currentSub.chatId, msgId, forAll) },
                            onForwardMessages = viewModel::forwardMessages,
                            onMarkViewOnceOpened = { msgId -> viewModel.markViewOnceOpened(currentSub.chatId, msgId) },
                            onStartCall = { callType: CallType ->
                                val c = chat ?: return@ChatDetailScreen
                                val uid = c.otherUserId ?: c.id
                                val phone = c.otherUserPhone.orEmpty()
                                viewModel.startCall(uid, c.title, phone, c.avatarUrl, callType)
                            },
                            onToggleMute = { viewModel.toggleMuteChat(currentSub.chatId) },
                            onSetDisappearingTimer = { sec -> viewModel.setDisappearingTimer(currentSub.chatId, sec) },
                            onSetWallpaper = { wp -> viewModel.setChatWallpaper(currentSub.chatId, wp) },
                            onClearChat = { viewModel.clearChat(currentSub.chatId) },
                            onBlockUser = viewModel::blockUser,
                            onUpdateGroupInfo = { t, d -> viewModel.updateGroupInfo(currentSub.chatId, t, d) },
                            onAddGroupMembers = { uids -> viewModel.addGroupMembers(currentSub.chatId, uids) },
                            onRemoveGroupMember = { uid -> viewModel.removeGroupMember(currentSub.chatId, uid) },
                            onToggleGroupAdmin = { uid -> viewModel.toggleGroupAdmin(currentSub.chatId, uid) },
                            onLeaveGroup = { viewModel.leaveGroup(currentSub.chatId) }
                        )
                    }

                    SubScreen.Archived -> {
                        ArchivedChatsScreen(
                            chats = chats,
                            currentUserUid = currentUser?.uid.orEmpty(),
                            onBack = { subScreen = SubScreen.None },
                            onOpenChat = { id -> subScreen = SubScreen.ChatDetail(id) },
                            onUnarchive = viewModel::toggleArchiveChat
                        )
                    }

                    SubScreen.Starred -> {
                        StarredMessagesScreen(
                            starredMessages = starredMessages,
                            onBack = { subScreen = SubScreen.None },
                            onOpenChat = { id -> subScreen = SubScreen.ChatDetail(id) },
                            onUnstar = viewModel::toggleStarMessage
                        )
                    }

                    SubScreen.None -> {
                        val unreadChatsTotal = remember(chats) {
                            chats.count { !it.isArchived && it.unreadCount > 0 }
                        }
                        Scaffold(
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = activeTab == HomeTab.CHATS,
                                        onClick = { activeTab = HomeTab.CHATS },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (unreadChatsTotal > 0) {
                                                        Badge { Text(unreadChatsTotal.toString()) }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chats")
                                            }
                                        },
                                        label = { Text("Chats") }
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == HomeTab.UPDATES,
                                        onClick = { activeTab = HomeTab.UPDATES },
                                        icon = { Icon(Icons.Default.TrackChanges, contentDescription = "Updates") },
                                        label = { Text("Updates") }
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == HomeTab.CALLS,
                                        onClick = { activeTab = HomeTab.CALLS },
                                        icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                                        label = { Text("Calls") }
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == HomeTab.SETTINGS,
                                        onClick = { activeTab = HomeTab.SETTINGS },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                when (activeTab) {
                                    HomeTab.CHATS -> ChatsTabScreen(
                                        chats = chats,
                                        contacts = contacts,
                                        currentUserUid = currentUser?.uid.orEmpty(),
                                        isFirebaseConfigured = viewModel.cloudStatus.isFirebaseConfigured,
                                        onOpenChat = { id -> subScreen = SubScreen.ChatDetail(id) },
                                        onOpenArchived = { subScreen = SubScreen.Archived },
                                        onOpenStarred = { subScreen = SubScreen.Starred },
                                        onOpenSettings = { activeTab = HomeTab.SETTINGS },
                                        onTogglePin = viewModel::togglePinChat,
                                        onToggleMute = viewModel::toggleMuteChat,
                                        onToggleArchive = viewModel::toggleArchiveChat,
                                        onToggleFavorite = viewModel::toggleFavoriteChat,
                                        onMarkUnread = viewModel::markChatUnread,
                                        onDeleteChat = viewModel::deleteChat,
                                        onFindAndOpenByPhone = viewModel::findAndOpenChatByPhone,
                                        onOpenDirectChat = viewModel::openDirectChat,
                                        onCreateGroup = viewModel::createGroup,
                                        onSendBroadcast = viewModel::sendBroadcast,
                                        onSyncDeviceContacts = viewModel::syncDeviceContacts,
                                        onSimulateIncomingCall = viewModel::simulateIncomingCall
                                    )

                                    HomeTab.UPDATES -> UpdatesTabScreen(
                                        currentUser = currentUser,
                                        statuses = statuses,
                                        channels = channels,
                                        onPostTextStatus = viewModel::postTextStatus,
                                        onPostImageStatus = viewModel::postImageStatus,
                                        onViewStatusGroup = { viewingStatusGroup = it },
                                        onToggleChannelFollow = viewModel::toggleChannelFollow
                                    )

                                    HomeTab.CALLS -> CallsTabScreen(
                                        callLogs = callLogs,
                                        contacts = contacts,
                                        currentUserUid = currentUser?.uid.orEmpty(),
                                        isFirebaseConfigured = viewModel.cloudStatus.isFirebaseConfigured,
                                        onStartCall = viewModel::startCall,
                                        onSimulateIncomingCall = viewModel::simulateIncomingCall,
                                        onClearCallLog = viewModel::clearCallLog
                                    )

                                    HomeTab.SETTINGS -> SettingsTabScreen(
                                        currentUser = currentUser,
                                        preferences = preferences,
                                        cloudStatus = viewModel.cloudStatus,
                                        contacts = contacts,
                                        onUpdateProfile = viewModel::updateProfile,
                                        onUpdatePreferences = viewModel::updatePreferences,
                                        onUpdatePrivacy = viewModel::updatePrivacy,
                                        onUnblockUser = { uid -> viewModel.blockUser(uid, false) },
                                        onOpenStarredMessages = { subScreen = SubScreen.Starred },
                                        onSignOut = viewModel::signOut,
                                        onDeleteAccount = viewModel::deleteAccount
                                    )
                                }
                            }
                        }
                    }
                }

                val statusGrp = viewingStatusGroup
                if (statusGrp != null) {
                    StatusViewerOverlay(
                        group = statusGrp,
                        myUid = currentUser?.uid.orEmpty(),
                        contacts = contacts,
                        onMarkViewed = viewModel::markStatusViewed,
                        onDeleteStatus = viewModel::deleteStatus,
                        onReplyToStatus = viewModel::replyToStatus,
                        onOpenChat = { id -> subScreen = SubScreen.ChatDetail(id) },
                        onClose = { viewingStatusGroup = null }
                    )
                }

                val call = activeCall
                if (call != null) {
                    ActiveCallOverlay(
                        session = call,
                        onAccept = viewModel::acceptIncomingCall,
                        onDecline = viewModel::declineIncomingCall,
                        onEnd = viewModel::endActiveCall,
                        onToggleMute = viewModel::toggleCallMute,
                        onToggleSpeaker = viewModel::toggleCallSpeaker,
                        onToggleVideo = viewModel::toggleCallVideo,
                        onSwitchCamera = viewModel::switchCallCamera
                    )
                }
            }
        }
    }
}
