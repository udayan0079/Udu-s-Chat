package com.uduschat.app.data.repository

import android.app.Activity
import com.uduschat.app.core.DeviceContact
import com.uduschat.app.core.PhoneNumberUtils
import com.uduschat.app.data.local.UserPreferencesStore
import com.uduschat.app.domain.model.ActiveCallSession
import com.uduschat.app.domain.model.AuthState
import com.uduschat.app.domain.model.AuthStep
import com.uduschat.app.domain.model.CallDirection
import com.uduschat.app.domain.model.CallRecord
import com.uduschat.app.domain.model.CallStatus
import com.uduschat.app.domain.model.CallType
import com.uduschat.app.domain.model.ChannelItem
import com.uduschat.app.domain.model.ChatMessage
import com.uduschat.app.domain.model.ChatThread
import com.uduschat.app.domain.model.ChatType
import com.uduschat.app.domain.model.MessageDraftAttachment
import com.uduschat.app.domain.model.MessageStatus
import com.uduschat.app.domain.model.MessageType
import com.uduschat.app.domain.model.PrivacySettings
import com.uduschat.app.domain.model.StatusType
import com.uduschat.app.domain.model.StatusUpdate
import com.uduschat.app.domain.model.UserProfile
import com.uduschat.app.domain.repository.UdusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Full-featured local/interactive repository for testing all udu's chat features
 * immediately before adding Firebase google-services.json.
 */
class DemoUdusRepository(
    private val preferencesStore: UserPreferencesStore? = null
) : UdusRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _authState = MutableStateFlow(AuthState(isDemoMode = true))
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _contacts = MutableStateFlow<List<UserProfile>>(emptyList())
    override val contacts: StateFlow<List<UserProfile>> = _contacts.asStateFlow()

    private val _chats = MutableStateFlow<List<ChatThread>>(emptyList())
    override val chats: StateFlow<List<ChatThread>> = _chats.asStateFlow()

    private val messagesByChat = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    private val _statuses = MutableStateFlow<List<StatusUpdate>>(emptyList())
    override val statuses: StateFlow<List<StatusUpdate>> = _statuses.asStateFlow()

    private val _channels = MutableStateFlow<List<ChannelItem>>(emptyList())
    override val channels: StateFlow<List<ChannelItem>> = _channels.asStateFlow()

    private val _callLogs = MutableStateFlow<List<CallRecord>>(emptyList())
    override val callLogs: StateFlow<List<CallRecord>> = _callLogs.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
    override val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

    private val _starredMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val starredMessages: StateFlow<List<ChatMessage>> = _starredMessages.asStateFlow()

    private var generatedDemoOtp: String = "123456"

    init {
        if (preferencesStore != null) {
            scope.launch {
                val saved = preferencesStore.demoSessionFlow.firstOrNull()
                if (saved != null) {
                    val (phone, name, about) = saved
                    initializeSignedInDemo(phone = phone, name = name, about = about, avatarUri = null)
                }
            }
        }
    }

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> =
        messagesByChat.map { map ->
            val myUid = _currentUser.value?.uid.orEmpty()
            val now = System.currentTimeMillis()
            map[chatId].orEmpty()
                .filter { msg -> myUid !in msg.deletedFor && (msg.expiresAt == null || msg.expiresAt > now) }
                .sortedBy { it.createdAt }
        }

    override fun observeChat(chatId: String): Flow<ChatThread?> =
        _chats.map { list -> list.firstOrNull { it.id == chatId } }

    override fun proceedFromWelcome() {
        _authState.update { it.copy(step = AuthStep.PHONE_INPUT, errorMessage = null) }
    }

    override fun clearAuthError() {
        _authState.update { it.copy(errorMessage = null) }
    }

    override suspend fun sendOtp(phoneE164: String, activity: Activity?) {
        if (!PhoneNumberUtils.isValidE164(phoneE164)) {
            _authState.update { it.copy(errorMessage = "Enter a valid international phone number.") }
            return
        }
        generatedDemoOtp = "123456"
        _authState.update {
            it.copy(
                step = AuthStep.OTP_VERIFY,
                pendingPhoneE164 = phoneE164,
                verificationId = "demo_verification",
                demoOtpHint = generatedDemoOtp,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    override suspend fun resendOtp(activity: Activity?) {
        val phone = _authState.value.pendingPhoneE164
        if (phone.isBlank()) return
        generatedDemoOtp = ((100000..999999).random()).toString()
        _authState.update {
            it.copy(demoOtpHint = generatedDemoOtp, errorMessage = null)
        }
    }

    override suspend fun verifyOtp(code: String) {
        val trimmed = code.trim()
        if (trimmed.length != 6 || trimmed != generatedDemoOtp) {
            _authState.update {
                it.copy(errorMessage = "Invalid OTP code. Demo code is $generatedDemoOtp.")
            }
            return
        }
        _authState.update {
            it.copy(step = AuthStep.PROFILE_SETUP, isLoading = false, errorMessage = null)
        }
    }

    override fun backToPhoneInput() {
        _authState.update {
            it.copy(step = AuthStep.PHONE_INPUT, demoOtpHint = null, errorMessage = null)
        }
    }

    override suspend fun completeProfileSetup(
        displayName: String,
        about: String,
        avatarUri: String?
    ) {
        val cleanName = displayName.trim().ifBlank { "Udu User" }
        val cleanAbout = about.trim().ifBlank { "Hey there! I am using udu's chat." }
        val phone = _authState.value.pendingPhoneE164.ifBlank { "+8801712345678" }
        preferencesStore?.saveDemoSession(phone, cleanName, cleanAbout, avatarUri)
        initializeSignedInDemo(phone, cleanName, cleanAbout, avatarUri)
    }

    private fun initializeSignedInDemo(
        phone: String,
        name: String,
        about: String,
        avatarUri: String?
    ) {
        val myUid = "demo_me"
        val me = UserProfile(
            uid = myUid,
            phoneNumber = phone,
            displayName = name,
            about = about,
            avatarUrl = avatarUri,
            isOnline = true,
            lastSeenAt = System.currentTimeMillis()
        )
        _currentUser.value = me
        _authState.value = AuthState(
            step = AuthStep.AUTHENTICATED,
            pendingPhoneE164 = phone,
            isDemoMode = true
        )
        if (_chats.value.isEmpty()) {
            seedDemoWorld(me)
        }
    }

    private fun seedDemoWorld(me: UserProfile) {
        val now = System.currentTimeMillis()
        val rafi = UserProfile(
            uid = "u_rafi",
            phoneNumber = "+8801711223344",
            displayName = "Rafi Ahmed",
            about = "Building mobile apps in Dhaka",
            isOnline = true,
            lastSeenAt = now
        )
        val nusrat = UserProfile(
            uid = "u_nusrat",
            phoneNumber = "+447911123456",
            displayName = "Nusrat Jahan (London)",
            about = "Available worldwide on udu's chat",
            isOnline = false,
            lastSeenAt = now - 14 * 60_000L
        )
        val tanvir = UserProfile(
            uid = "u_tanvir",
            phoneNumber = "+15552345678",
            displayName = "Tanvir Hasan (NYC)",
            about = "Coffee & Kotlin",
            isOnline = true,
            lastSeenAt = now
        )
        val ayesha = UserProfile(
            uid = "u_ayesha",
            phoneNumber = "+971501234567",
            displayName = "Ayesha Rahman (Dubai)",
            about = "Traveling across time zones",
            isOnline = false,
            lastSeenAt = now - 2 * 3600_000L
        )
        val support = UserProfile(
            uid = "u_support",
            phoneNumber = "+8801800000001",
            displayName = "udu's chat Global Guide",
            about = "Connect Firebase google-services.json for global live sync",
            isOnline = true,
            lastSeenAt = now
        )

        _contacts.value = listOf(rafi, nusrat, tanvir, ayesha, support)

        val chatRafi = ChatThread(
            id = "chat_rafi",
            type = ChatType.DIRECT,
            title = rafi.displayName,
            avatarUrl = null,
            participantIds = listOf(me.uid, rafi.uid),
            otherUserId = rafi.uid,
            otherUserPhone = rafi.phoneNumber,
            isOnline = rafi.isOnline,
            lastSeenAt = rafi.lastSeenAt,
            lastMessagePreview = "Voice notes, groups, and calls are ready!",
            lastMessageSenderId = rafi.uid,
            lastMessageStatus = MessageStatus.READ,
            lastMessageTime = now - 3 * 60_000L,
            unreadCount = 2,
            isPinned = true,
            isFavorite = true
        )

        val chatGroup = ChatThread(
            id = "chat_squad",
            type = ChatType.GROUP,
            title = "Global Friends Squad",
            description = "Chatting across Dhaka, London, NYC & Dubai",
            participantIds = listOf(me.uid, rafi.uid, nusrat.uid, tanvir.uid, ayesha.uid),
            adminIds = listOf(me.uid, rafi.uid),
            lastMessagePreview = "Nusrat: Reaching out from London!",
            lastMessageSenderId = nusrat.uid,
            lastMessageStatus = MessageStatus.DELIVERED,
            lastMessageTime = now - 22 * 60_000L,
            unreadCount = 1,
            isFavorite = true
        )

        val chatNusrat = ChatThread(
            id = "chat_nusrat",
            type = ChatType.DIRECT,
            title = nusrat.displayName,
            participantIds = listOf(me.uid, nusrat.uid),
            otherUserId = nusrat.uid,
            otherUserPhone = nusrat.phoneNumber,
            isOnline = false,
            lastSeenAt = nusrat.lastSeenAt,
            lastMessagePreview = "Call me on udu's chat tonight!",
            lastMessageSenderId = me.uid,
            lastMessageStatus = MessageStatus.READ,
            lastMessageTime = now - 3 * 3600_000L
        )

        val chatTanvir = ChatThread(
            id = "chat_tanvir",
            type = ChatType.DIRECT,
            title = tanvir.displayName,
            participantIds = listOf(me.uid, tanvir.uid),
            otherUserId = tanvir.uid,
            otherUserPhone = tanvir.phoneNumber,
            isOnline = true,
            lastSeenAt = tanvir.lastSeenAt,
            lastMessagePreview = "Check out the new Status stories tab",
            lastMessageSenderId = tanvir.uid,
            lastMessageStatus = MessageStatus.READ,
            lastMessageTime = now - 26 * 3600_000L
        )

        val chatSupport = ChatThread(
            id = "chat_support",
            type = ChatType.DIRECT,
            title = support.displayName,
            participantIds = listOf(me.uid, support.uid),
            otherUserId = support.uid,
            otherUserPhone = support.phoneNumber,
            isOnline = true,
            lastSeenAt = support.lastSeenAt,
            lastMessagePreview = "You can dial any valid international number in New Chat",
            lastMessageSenderId = support.uid,
            lastMessageStatus = MessageStatus.READ,
            lastMessageTime = now - 48 * 3600_000L
        )

        _chats.value = sortChats(listOf(chatRafi, chatGroup, chatNusrat, chatTanvir, chatSupport))

        val initialMessages = mapOf(
            "chat_rafi" to listOf(
                ChatMessage(
                    id = "m_r1",
                    chatId = "chat_rafi",
                    senderId = rafi.uid,
                    senderName = rafi.displayName,
                    senderPhone = rafi.phoneNumber,
                    text = "Welcome to udu's chat! Tap New Chat (+) to connect any valid phone number.",
                    status = MessageStatus.READ,
                    createdAt = now - 15 * 60_000L
                ),
                ChatMessage(
                    id = "m_r2",
                    chatId = "chat_rafi",
                    senderId = me.uid,
                    senderName = me.displayName,
                    senderPhone = me.phoneNumber,
                    text = "Looks clean! Does it have reactions, replies, voice notes & calls?",
                    status = MessageStatus.READ,
                    createdAt = now - 8 * 60_000L,
                    reactions = mapOf(rafi.uid to "\uD83D\uDD25")
                ),
                ChatMessage(
                    id = "m_r3",
                    chatId = "chat_rafi",
                    senderId = rafi.uid,
                    senderName = rafi.displayName,
                    senderPhone = rafi.phoneNumber,
                    text = "Voice notes, groups, and calls are ready!",
                    replyToMessageId = "m_r2",
                    replyToSenderName = me.displayName,
                    replyToPreview = "Looks clean! Does it have reactions, replies, voice notes & calls?",
                    status = MessageStatus.DELIVERED,
                    createdAt = now - 3 * 60_000L
                )
            ),
            "chat_squad" to listOf(
                ChatMessage(
                    id = "m_g1",
                    chatId = "chat_squad",
                    senderId = tanvir.uid,
                    senderName = tanvir.displayName,
                    senderPhone = tanvir.phoneNumber,
                    text = "Good morning from New York!",
                    status = MessageStatus.READ,
                    createdAt = now - 45 * 60_000L
                ),
                ChatMessage(
                    id = "m_g2",
                    chatId = "chat_squad",
                    senderId = nusrat.uid,
                    senderName = nusrat.displayName,
                    senderPhone = nusrat.phoneNumber,
                    text = "Reaching out from London!",
                    status = MessageStatus.DELIVERED,
                    createdAt = now - 22 * 60_000L
                )
            ),
            "chat_nusrat" to listOf(
                ChatMessage(
                    id = "m_n1",
                    chatId = "chat_nusrat",
                    senderId = nusrat.uid,
                    senderName = nusrat.displayName,
                    senderPhone = nusrat.phoneNumber,
                    type = MessageType.LOCATION,
                    locationLat = 51.5072,
                    locationLng = -0.1276,
                    locationLabel = "London, United Kingdom",
                    status = MessageStatus.READ,
                    createdAt = now - 4 * 3600_000L
                ),
                ChatMessage(
                    id = "m_n2",
                    chatId = "chat_nusrat",
                    senderId = me.uid,
                    senderName = me.displayName,
                    senderPhone = me.phoneNumber,
                    text = "Call me on udu's chat tonight!",
                    status = MessageStatus.READ,
                    createdAt = now - 3 * 3600_000L
                )
            ),
            "chat_tanvir" to listOf(
                ChatMessage(
                    id = "m_t1",
                    chatId = "chat_tanvir",
                    senderId = tanvir.uid,
                    senderName = tanvir.displayName,
                    senderPhone = tanvir.phoneNumber,
                    text = "Check out the new Status stories tab",
                    status = MessageStatus.READ,
                    createdAt = now - 26 * 3600_000L
                )
            ),
            "chat_support" to listOf(
                ChatMessage(
                    id = "m_s1",
                    chatId = "chat_support",
                    senderId = support.uid,
                    senderName = support.displayName,
                    senderPhone = support.phoneNumber,
                    text = "You can dial any valid international number in New Chat. Drop your Firebase google-services.json into app/ to switch from Local Demo Mode to real global SMS OTP & live cloud sync!",
                    status = MessageStatus.READ,
                    createdAt = now - 48 * 3600_000L,
                    starredBy = listOf(me.uid)
                )
            )
        )
        messagesByChat.value = initialMessages
        recomputeStarred()

        _statuses.value = listOf(
            StatusUpdate(
                id = "st_rafi",
                userId = rafi.uid,
                userName = rafi.displayName,
                userPhone = rafi.phoneNumber,
                type = StatusType.TEXT,
                text = "Testing udu's chat global voice notes & status stories!",
                backgroundHex = 0xFF008069,
                createdAt = now - 35 * 60_000L,
                expiresAt = now + 23 * 3600_000L
            ),
            StatusUpdate(
                id = "st_nusrat",
                userId = nusrat.uid,
                userName = nusrat.displayName,
                userPhone = nusrat.phoneNumber,
                type = StatusType.TEXT,
                text = "Rainy evening in London \u2614 Connected globally!",
                backgroundHex = 0xFF1F3C88,
                createdAt = now - 2 * 3600_000L,
                expiresAt = now + 22 * 3600_000L
            ),
            StatusUpdate(
                id = "st_tanvir",
                userId = tanvir.uid,
                userName = tanvir.displayName,
                userPhone = tanvir.phoneNumber,
                type = StatusType.TEXT,
                text = "Late night coding session \uD83D\uDCBB",
                backgroundHex = 0xFF6A1B9A,
                createdAt = now - 5 * 3600_000L,
                expiresAt = now + 19 * 3600_000L,
                viewers = mapOf(me.uid to now - 4 * 3600_000L)
            )
        )

        _channels.value = listOf(
            ChannelItem(
                id = "ch_udus",
                name = "udu's chat Announcements",
                description = "Official tips, privacy updates & new feature drops",
                followerCount = 128400,
                isFollowing = true,
                latestUpdate = "Global phone number connection & voice notes are live!",
                latestTime = now - 55 * 60_000L
            ),
            ChannelItem(
                id = "ch_tech",
                name = "Mobile & Android Dev",
                description = "Kotlin, Jetpack Compose, and Firebase architecture",
                followerCount = 45200,
                isFollowing = false,
                latestUpdate = "Material 3 adaptive layouts & real-time Firestore sync",
                latestTime = now - 4 * 3600_000L
            ),
            ChannelItem(
                id = "ch_cricket",
                name = "World Cricket Live",
                description = "Ball-by-ball scores and match highlights",
                followerCount = 310900,
                isFollowing = false,
                latestUpdate = "Bangladesh vs India series schedule announced",
                latestTime = now - 7 * 3600_000L
            )
        )

        _callLogs.value = listOf(
            CallRecord(
                id = "call_1",
                callerId = rafi.uid,
                callerName = rafi.displayName,
                callerPhone = rafi.phoneNumber,
                calleeId = me.uid,
                calleeName = me.displayName,
                calleePhone = me.phoneNumber,
                callType = CallType.VIDEO,
                direction = CallDirection.INCOMING,
                status = CallStatus.ENDED,
                startedAt = now - 45 * 60_000L,
                endedAt = now - 41 * 60_000L,
                durationSeconds = 242L
            ),
            CallRecord(
                id = "call_2",
                callerId = nusrat.uid,
                callerName = nusrat.displayName,
                callerPhone = nusrat.phoneNumber,
                calleeId = me.uid,
                calleeName = me.displayName,
                calleePhone = me.phoneNumber,
                callType = CallType.VOICE,
                direction = CallDirection.INCOMING,
                status = CallStatus.MISSED,
                startedAt = now - 3 * 3600_000L
            ),
            CallRecord(
                id = "call_3",
                callerId = me.uid,
                callerName = me.displayName,
                callerPhone = me.phoneNumber,
                calleeId = tanvir.uid,
                calleeName = tanvir.displayName,
                calleePhone = tanvir.phoneNumber,
                callType = CallType.VOICE,
                direction = CallDirection.OUTGOING,
                status = CallStatus.ENDED,
                startedAt = now - 18 * 3600_000L,
                endedAt = now - 18 * 3600_000L + 95_000L,
                durationSeconds = 95L
            )
        )
    }

    override suspend fun updateProfile(displayName: String, about: String, avatarUri: String?) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            displayName = displayName.trim().ifBlank { current.displayName },
            about = about.trim().ifBlank { current.about },
            avatarUrl = avatarUri ?: current.avatarUrl
        )
        _currentUser.value = updated
        preferencesStore?.saveDemoSession(updated.phoneNumber, updated.displayName, updated.about, updated.avatarUrl)
    }

    override suspend fun updatePrivacy(transform: (PrivacySettings) -> PrivacySettings) {
        _currentUser.update { user ->
            user?.copy(privacy = transform(user.privacy))
        }
    }

    override suspend fun setOnlinePresence(isOnline: Boolean) {
        _currentUser.update { user ->
            user?.copy(isOnline = isOnline, lastSeenAt = System.currentTimeMillis())
        }
    }

    override suspend fun signOut() {
        preferencesStore?.clearDemoSession()
        _currentUser.value = null
        _authState.value = AuthState(step = AuthStep.WELCOME, isDemoMode = true)
    }

    override suspend fun deleteAccount() {
        _chats.value = emptyList()
        messagesByChat.value = emptyMap()
        _statuses.value = emptyList()
        _callLogs.value = emptyList()
        signOut()
    }

    override suspend fun findUserByPhone(phoneE164: String): Result<UserProfile?> {
        if (!PhoneNumberUtils.isValidE164(phoneE164)) {
            return Result.failure(IllegalArgumentException("Invalid international phone number"))
        }
        val existing = _contacts.value.firstOrNull { it.phoneNumber == phoneE164 }
        if (existing != null) return Result.success(existing)

        // In Demo mode, allow connecting to any valid international phone number directly.
        val country = PhoneNumberUtils.findCountryByE164(phoneE164)
        val regionLabel = country?.name ?: "International"
        val created = UserProfile(
            uid = "u_${phoneE164.removePrefix("+")}",
            phoneNumber = phoneE164,
            displayName = PhoneNumberUtils.formatDisplayPhone(phoneE164),
            about = "Connected via $regionLabel",
            isOnline = true,
            lastSeenAt = System.currentTimeMillis()
        )
        _contacts.update { list -> list + created }
        return Result.success(created)
    }

    override suspend fun syncDeviceContacts(deviceContacts: List<DeviceContact>): Result<List<UserProfile>> {
        val currentMap = _contacts.value.associateBy { it.phoneNumber }.toMutableMap()
        val now = System.currentTimeMillis()
        deviceContacts.forEach { dc ->
            if (dc.phoneNumberE164 != _currentUser.value?.phoneNumber && !currentMap.containsKey(dc.phoneNumberE164)) {
                currentMap[dc.phoneNumberE164] = UserProfile(
                    uid = "u_${dc.phoneNumberE164.removePrefix("+")}",
                    phoneNumber = dc.phoneNumberE164,
                    displayName = dc.displayName,
                    about = "Available on udu's chat",
                    isOnline = false,
                    lastSeenAt = now - 600_000L
                )
            }
        }
        val merged = currentMap.values.sortedBy { it.displayName }
        _contacts.value = merged
        return Result.success(merged)
    }

    override suspend fun startOrOpenDirectChat(user: UserProfile): Result<String> {
        val me = _currentUser.value ?: return Result.failure(IllegalStateException("Not signed in"))
        val existing = _chats.value.firstOrNull {
            it.type == ChatType.DIRECT && (it.otherUserId == user.uid || it.otherUserPhone == user.phoneNumber)
        }
        if (existing != null) return Result.success(existing.id)

        if (_contacts.value.none { it.uid == user.uid }) {
            _contacts.update { it + user }
        }
        val chatId = "chat_${UUID.randomUUID().toString().take(8)}"
        val thread = ChatThread(
            id = chatId,
            type = ChatType.DIRECT,
            title = user.displayName.ifBlank { PhoneNumberUtils.formatDisplayPhone(user.phoneNumber) },
            avatarUrl = user.avatarUrl,
            participantIds = listOf(me.uid, user.uid),
            otherUserId = user.uid,
            otherUserPhone = user.phoneNumber,
            isOnline = user.isOnline,
            lastSeenAt = user.lastSeenAt,
            lastMessagePreview = "Start messaging ${user.displayName}",
            lastMessageTime = System.currentTimeMillis()
        )
        _chats.update { sortChats(listOf(thread) + it) }
        return Result.success(chatId)
    }

    override suspend fun createGroupChat(
        title: String,
        description: String,
        memberUids: List<String>,
        avatarUri: String?
    ): Result<String> {
        val me = _currentUser.value ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Group name required"))

        val chatId = "grp_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val members = (listOf(me.uid) + memberUids).distinct()
        val thread = ChatThread(
            id = chatId,
            type = ChatType.GROUP,
            title = cleanTitle,
            description = description.trim(),
            avatarUrl = avatarUri,
            participantIds = members,
            adminIds = listOf(me.uid),
            lastMessagePreview = "${me.displayName} created group \"$cleanTitle\"",
            lastMessageSenderId = me.uid,
            lastMessageType = MessageType.SYSTEM,
            lastMessageTime = now
        )
        val sysMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = me.uid,
            senderName = me.displayName,
            type = MessageType.SYSTEM,
            text = "${me.displayName} created group \"$cleanTitle\"",
            createdAt = now
        )
        messagesByChat.update { it + (chatId to listOf(sysMsg)) }
        _chats.update { sortChats(listOf(thread) + it) }
        return Result.success(chatId)
    }

    override suspend fun sendBroadcastMessage(recipientUids: List<String>, text: String): Result<Int> {
        val clean = text.trim()
        if (clean.isBlank() || recipientUids.isEmpty()) return Result.success(0)
        var sentCount = 0
        recipientUids.forEach { uid ->
            val user = _contacts.value.firstOrNull { it.uid == uid } ?: return@forEach
            val chatId = startOrOpenDirectChat(user).getOrNull() ?: return@forEach
            sendMessage(chatId = chatId, text = clean)
            sentCount++
        }
        return Result.success(sentCount)
    }

    override suspend fun sendMessage(
        chatId: String,
        text: String,
        attachment: MessageDraftAttachment?,
        replyTo: ChatMessage?
    ) {
        val me = _currentUser.value ?: return
        val thread = _chats.value.firstOrNull { it.id == chatId } ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank() && attachment == null) return

        val now = System.currentTimeMillis()
        val expiresAt = thread.disappearingSeconds.takeIf { it > 0L }?.let { now + it * 1000L }
        val message = ChatMessage(
            id = "m_${UUID.randomUUID().toString().take(10)}",
            chatId = chatId,
            senderId = me.uid,
            senderName = me.displayName,
            senderPhone = me.phoneNumber,
            type = attachment?.type ?: MessageType.TEXT,
            text = trimmed,
            mediaUrl = attachment?.uriOrPath,
            mediaLocalPath = attachment?.uriOrPath,
            mediaMimeType = attachment?.mimeType,
            mediaDurationMs = attachment?.durationMs ?: 0L,
            fileName = attachment?.fileName,
            fileSizeBytes = attachment?.fileSizeBytes ?: 0L,
            locationLat = attachment?.locationLat,
            locationLng = attachment?.locationLng,
            locationLabel = attachment?.locationLabel,
            sharedContactName = attachment?.sharedContactName,
            sharedContactPhone = attachment?.sharedContactPhone,
            replyToMessageId = replyTo?.id,
            replyToSenderName = replyTo?.senderName,
            replyToPreview = replyTo?.displayPreview(),
            viewOnce = attachment?.viewOnce ?: false,
            status = MessageStatus.SENT,
            createdAt = now,
            expiresAt = expiresAt
        )

        appendMessage(chatId, message, resetDraft = true, incrementUnread = false)

        // Simulate realistic delivery -> read -> contextual reply when chatting with demo peers.
        val isBlocked = thread.otherUserId != null && thread.otherUserId in me.privacy.blockedUids
        if (!isBlocked) {
            scope.launch {
                delay(450L)
                updateMessageStatus(chatId, message.id, MessageStatus.DELIVERED)
                delay(650L)
                val finalStatus = if (me.privacy.readReceiptsEnabled) MessageStatus.READ else MessageStatus.DELIVERED
                updateMessageStatus(chatId, message.id, finalStatus)

                val responder = if (thread.type == ChatType.DIRECT) {
                    _contacts.value.firstOrNull { it.uid == thread.otherUserId }
                } else {
                    _contacts.value.firstOrNull { it.uid in thread.participantIds && it.uid != me.uid }
                }
                if (responder != null) {
                    delay(400L)
                    setPeerTyping(chatId, responder.uid, true)
                    delay(1100L)
                    setPeerTyping(chatId, responder.uid, false)
                    val replyMsg = ChatMessage(
                        id = "m_${UUID.randomUUID().toString().take(10)}",
                        chatId = chatId,
                        senderId = responder.uid,
                        senderName = responder.displayName,
                        senderPhone = responder.phoneNumber,
                        type = MessageType.TEXT,
                        text = buildDemoAutoReply(message),
                        status = MessageStatus.DELIVERED,
                        createdAt = System.currentTimeMillis(),
                        expiresAt = thread.disappearingSeconds.takeIf { it > 0L }?.let { System.currentTimeMillis() + it * 1000L }
                    )
                    appendMessage(chatId, replyMsg, resetDraft = false, incrementUnread = false)
                }
            }
        }
    }

    private fun buildDemoAutoReply(incoming: ChatMessage): String {
        val lower = incoming.text.lowercase()
        return when (incoming.type) {
            MessageType.AUDIO -> "Got your voice note! Sounds crystal clear."
            MessageType.IMAGE -> "Nice photo! Looks great on udu's chat."
            MessageType.DOCUMENT -> "Received the document (${incoming.fileName ?: "file"})."
            MessageType.LOCATION -> "Thanks for sharing the location pin!"
            MessageType.CONTACT -> "Saved ${incoming.sharedContactName ?: "the contact"}."
            else -> when {
                "hi" in lower || "hello" in lower || "salam" in lower -> "Hey! Good to connect with you on udu's chat."
                "call" in lower -> "Sure! Tap the audio or video call button anytime."
                else -> "Received: \"${incoming.text.take(60)}\" \uD83D\uDC4D"
            }
        }
    }

    private fun appendMessage(
        chatId: String,
        message: ChatMessage,
        resetDraft: Boolean,
        incrementUnread: Boolean
    ) {
        messagesByChat.update { map ->
            val current = map[chatId].orEmpty()
            map + (chatId to (current + message))
        }
        _chats.update { list ->
            sortChats(
                list.map { chat ->
                    if (chat.id != chatId) chat
                    else chat.copy(
                        lastMessagePreview = if (chat.type == ChatType.GROUP && message.senderId != _currentUser.value?.uid && message.type != MessageType.SYSTEM) {
                            "${message.senderName.substringBefore(" ")}: ${message.displayPreview()}"
                        } else {
                            message.displayPreview()
                        },
                        lastMessageSenderId = message.senderId,
                        lastMessageType = message.type,
                        lastMessageStatus = message.status,
                        lastMessageTime = message.createdAt,
                        draftText = if (resetDraft) "" else chat.draftText,
                        unreadCount = if (incrementUnread) chat.unreadCount + 1 else chat.unreadCount
                    )
                }
            )
        }
    }

    private fun updateMessageStatus(chatId: String, messageId: String, status: MessageStatus) {
        messagesByChat.update { map ->
            val list = map[chatId] ?: return@update map
            map + (chatId to list.map { if (it.id == messageId) it.copy(status = status) else it })
        }
        _chats.update { chats ->
            chats.map { c ->
                if (c.id == chatId && c.lastMessageSenderId == _currentUser.value?.uid) {
                    c.copy(lastMessageStatus = status)
                } else c
            }
        }
    }

    private fun setPeerTyping(chatId: String, peerUid: String, typing: Boolean) {
        _chats.update { list ->
            list.map { chat ->
                if (chat.id != chatId) chat
                else {
                    val updated = if (typing) {
                        (chat.typingUserIds + peerUid).distinct()
                    } else {
                        chat.typingUserIds - peerUid
                    }
                    chat.copy(typingUserIds = updated)
                }
            }
        }
    }

    override suspend fun markChatAsRead(chatId: String) {
        val meUid = _currentUser.value?.uid ?: return
        _chats.update { list ->
            list.map { if (it.id == chatId) it.copy(unreadCount = 0) else it }
        }
        messagesByChat.update { map ->
            val list = map[chatId] ?: return@update map
            map + (chatId to list.map { msg ->
                if (msg.senderId != meUid && msg.status != MessageStatus.READ) {
                    msg.copy(status = MessageStatus.READ)
                } else msg
            })
        }
    }

    override suspend fun setTyping(chatId: String, isTyping: Boolean) {
        // Local self-typing no-op in demo repository.
    }

    override suspend fun saveDraft(chatId: String, draft: String) {
        _chats.update { list ->
            list.map { if (it.id == chatId) it.copy(draftText = draft) else it }
        }
    }

    override suspend fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val meUid = _currentUser.value?.uid ?: return
        messagesByChat.update { map ->
            val list = map[chatId] ?: return@update map
            map + (chatId to list.map { msg ->
                if (msg.id != messageId) msg
                else {
                    val updated = msg.reactions.toMutableMap()
                    if (updated[meUid] == emoji) updated.remove(meUid) else updated[meUid] = emoji
                    msg.copy(reactions = updated)
                }
            })
        }
    }

    override suspend fun toggleStarMessage(chatId: String, messageId: String) {
        val meUid = _currentUser.value?.uid ?: return
        messagesByChat.update { map ->
            val list = map[chatId] ?: return@update map
            map + (chatId to list.map { msg ->
                if (msg.id != messageId) msg
                else {
                    val starred = if (meUid in msg.starredBy) msg.starredBy - meUid else msg.starredBy + meUid
                    msg.copy(starredBy = starred)
                }
            })
        }
        recomputeStarred()
    }

    override suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        val clean = newText.trim()
        if (clean.isBlank()) return
        messagesByChat.update { map ->
            val list = map[chatId] ?: return@update map
            map + (chatId to list.map { msg ->
                if (msg.id == messageId) msg.copy(text = clean, isEdited = true) else msg
            })
        }
        refreshLastMessagePreview(chatId)
    }

    override suspend fun deleteMessage(chatId: String, messageId: String, forEveryone: Boolean) {
        val meUid = _currentUser.value?.uid ?: return
        messagesByChat.update { map ->
            val list = map[chatId] ?: return@update map
            map + (chatId to list.map { msg ->
                if (msg.id != messageId) msg
                else if (forEveryone) {
                    msg.copy(isDeletedForEveryone = true, text = "", mediaUrl = null, reactions = emptyMap())
                } else {
                    msg.copy(deletedFor = (msg.deletedFor + meUid).distinct())
                }
            })
        }
        recomputeStarred()
        refreshLastMessagePreview(chatId)
    }

    override suspend fun forwardMessages(targetChatIds: List<String>, messages: List<ChatMessage>) {
        val me = _currentUser.value ?: return
        targetChatIds.forEach { targetId ->
            messages.forEach { src ->
                val forwarded = src.copy(
                    id = "m_${UUID.randomUUID().toString().take(10)}",
                    chatId = targetId,
                    senderId = me.uid,
                    senderName = me.displayName,
                    senderPhone = me.phoneNumber,
                    replyToMessageId = null,
                    replyToSenderName = null,
                    replyToPreview = null,
                    reactions = emptyMap(),
                    starredBy = emptyList(),
                    deletedFor = emptyList(),
                    isForwarded = true,
                    isEdited = false,
                    status = MessageStatus.READ,
                    createdAt = System.currentTimeMillis()
                )
                appendMessage(targetId, forwarded, resetDraft = false, incrementUnread = false)
            }
        }
    }

    override suspend fun markViewOnceOpened(chatId: String, messageId: String) {
        val meUid = _currentUser.value?.uid ?: return
        messagesByChat.update { map ->
            val list = map[chatId] ?: return@update map
            map + (chatId to list.map { msg ->
                if (msg.id == messageId) msg.copy(viewedBy = (msg.viewedBy + meUid).distinct()) else msg
            })
        }
    }

    private fun refreshLastMessagePreview(chatId: String) {
        val meUid = _currentUser.value?.uid.orEmpty()
        val lastVisible = messagesByChat.value[chatId].orEmpty()
            .filter { meUid !in it.deletedFor }
            .maxByOrNull { it.createdAt }
        _chats.update { list ->
            list.map { chat ->
                if (chat.id != chatId) chat
                else if (lastVisible != null) {
                    chat.copy(
                        lastMessagePreview = lastVisible.displayPreview(),
                        lastMessageTime = lastVisible.createdAt,
                        lastMessageSenderId = lastVisible.senderId
                    )
                } else {
                    chat.copy(lastMessagePreview = "")
                }
            }
        }
    }

    private fun recomputeStarred() {
        val meUid = _currentUser.value?.uid.orEmpty()
        _starredMessages.value = messagesByChat.value.values.flatten()
            .filter { meUid in it.starredBy && meUid !in it.deletedFor && !it.isDeletedForEveryone }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun togglePinChat(chatId: String) {
        _chats.update { list -> sortChats(list.map { if (it.id == chatId) it.copy(isPinned = !it.isPinned) else it }) }
    }

    override suspend fun toggleMuteChat(chatId: String) {
        _chats.update { list -> list.map { if (it.id == chatId) it.copy(isMuted = !it.isMuted) else it } }
    }

    override suspend fun toggleArchiveChat(chatId: String) {
        _chats.update { list -> list.map { if (it.id == chatId) it.copy(isArchived = !it.isArchived) else it } }
    }

    override suspend fun toggleFavoriteChat(chatId: String) {
        _chats.update { list -> list.map { if (it.id == chatId) it.copy(isFavorite = !it.isFavorite) else it } }
    }

    override suspend fun markChatUnread(chatId: String) {
        _chats.update { list -> list.map { if (it.id == chatId) it.copy(unreadCount = maxOf(1, it.unreadCount)) else it } }
    }

    override suspend fun clearChatMessages(chatId: String) {
        messagesByChat.update { it + (chatId to emptyList()) }
        _chats.update { list -> list.map { if (it.id == chatId) it.copy(lastMessagePreview = "", unreadCount = 0) else it } }
        recomputeStarred()
    }

    override suspend fun deleteChat(chatId: String) {
        messagesByChat.update { it - chatId }
        _chats.update { list -> list.filterNot { it.id == chatId } }
        recomputeStarred()
    }

    override suspend fun setDisappearingTimer(chatId: String, seconds: Long) {
        _chats.update { list -> list.map { if (it.id == chatId) it.copy(disappearingSeconds = seconds) else it } }
    }

    override suspend fun setChatWallpaper(chatId: String, wallpaperId: String?) {
        _chats.update { list -> list.map { if (it.id == chatId) it.copy(wallpaperId = wallpaperId) else it } }
    }

    override suspend fun blockUser(uid: String, block: Boolean) {
        updatePrivacy { privacy ->
            val updated = if (block) (privacy.blockedUids + uid).distinct() else privacy.blockedUids - uid
            privacy.copy(blockedUids = updated)
        }
    }

    override suspend fun updateGroupInfo(chatId: String, title: String, description: String) {
        _chats.update { list ->
            list.map {
                if (it.id == chatId) it.copy(title = title.trim().ifBlank { it.title }, description = description.trim())
                else it
            }
        }
    }

    override suspend fun addGroupMembers(chatId: String, newMemberUids: List<String>) {
        _chats.update { list ->
            list.map {
                if (it.id == chatId) it.copy(participantIds = (it.participantIds + newMemberUids).distinct())
                else it
            }
        }
    }

    override suspend fun removeGroupMember(chatId: String, memberUid: String) {
        _chats.update { list ->
            list.map {
                if (it.id == chatId) it.copy(
                    participantIds = it.participantIds - memberUid,
                    adminIds = it.adminIds - memberUid
                ) else it
            }
        }
    }

    override suspend fun toggleGroupAdmin(chatId: String, memberUid: String) {
        _chats.update { list ->
            list.map {
                if (it.id != chatId) it
                else {
                    val admins = if (memberUid in it.adminIds) it.adminIds - memberUid else it.adminIds + memberUid
                    it.copy(adminIds = admins)
                }
            }
        }
    }

    override suspend fun leaveGroup(chatId: String) {
        deleteChat(chatId)
    }

    override suspend fun postTextStatus(text: String, backgroundHex: Long) {
        val me = _currentUser.value ?: return
        val clean = text.trim()
        if (clean.isBlank()) return
        val now = System.currentTimeMillis()
        val item = StatusUpdate(
            id = "st_${UUID.randomUUID().toString().take(8)}",
            userId = me.uid,
            userName = me.displayName,
            userPhone = me.phoneNumber,
            userAvatarUrl = me.avatarUrl,
            type = StatusType.TEXT,
            text = clean,
            backgroundHex = backgroundHex,
            createdAt = now,
            expiresAt = now + 24L * 3600_000L
        )
        _statuses.update { listOf(item) + it }
    }

    override suspend fun postImageStatus(mediaUri: String, caption: String) {
        val me = _currentUser.value ?: return
        val now = System.currentTimeMillis()
        val item = StatusUpdate(
            id = "st_${UUID.randomUUID().toString().take(8)}",
            userId = me.uid,
            userName = me.displayName,
            userPhone = me.phoneNumber,
            userAvatarUrl = me.avatarUrl,
            type = StatusType.IMAGE,
            mediaUrl = mediaUri,
            caption = caption.trim(),
            createdAt = now,
            expiresAt = now + 24L * 3600_000L
        )
        _statuses.update { listOf(item) + it }
    }

    override suspend fun markStatusViewed(statusId: String) {
        val meUid = _currentUser.value?.uid ?: return
        val now = System.currentTimeMillis()
        _statuses.update { list ->
            list.map { st ->
                if (st.id == statusId && st.userId != meUid && !st.viewers.containsKey(meUid)) {
                    st.copy(viewers = st.viewers + (meUid to now))
                } else st
            }
        }
    }

    override suspend fun deleteStatus(statusId: String) {
        _statuses.update { list -> list.filterNot { it.id == statusId } }
    }

    override suspend fun replyToStatus(status: StatusUpdate, replyText: String): Result<String> {
        val peer = _contacts.value.firstOrNull { it.uid == status.userId } ?: UserProfile(
            uid = status.userId,
            phoneNumber = status.userPhone,
            displayName = status.userName,
            avatarUrl = status.userAvatarUrl
        )
        val chatId = startOrOpenDirectChat(peer).getOrElse { return Result.failure(it) }
        val statusSummary = if (status.type == StatusType.TEXT) status.text else (status.caption.ifBlank { "\uD83D\uDCF7 Status photo" })
        val quoted = ChatMessage(
            id = "status_${status.id}",
            chatId = chatId,
            senderId = status.userId,
            senderName = "${status.userName} \u2022 Status",
            text = statusSummary
        )
        sendMessage(chatId = chatId, text = replyText, replyTo = quoted)
        return Result.success(chatId)
    }

    override suspend fun toggleChannelFollow(channelId: String) {
        _channels.update { list ->
            list.map { ch ->
                if (ch.id != channelId) ch
                else {
                    val following = !ch.isFollowing
                    ch.copy(
                        isFollowing = following,
                        followerCount = if (following) ch.followerCount + 1 else (ch.followerCount - 1).coerceAtLeast(0)
                    )
                }
            }
        }
    }

    override suspend fun startCall(
        peerUid: String,
        peerName: String,
        peerPhone: String,
        peerAvatarUrl: String?,
        callType: CallType
    ) {
        val callId = "call_${UUID.randomUUID().toString().take(8)}"
        _activeCall.value = ActiveCallSession(
            callId = callId,
            peerUid = peerUid,
            peerName = peerName,
            peerPhone = peerPhone,
            peerAvatarUrl = peerAvatarUrl,
            callType = callType,
            isIncoming = false,
            status = CallStatus.RINGING,
            isSpeakerOn = callType == CallType.VIDEO,
            isVideoEnabled = callType == CallType.VIDEO
        )
        // Simulate remote user answering after 2 seconds in Demo Mode.
        scope.launch {
            delay(2000L)
            val current = _activeCall.value
            if (current != null && current.callId == callId && current.status == CallStatus.RINGING) {
                _activeCall.value = current.copy(
                    status = CallStatus.CONNECTED,
                    connectedAtMs = System.currentTimeMillis()
                )
            }
        }
    }

    override suspend fun acceptIncomingCall() {
        _activeCall.update { current ->
            current?.copy(
                status = CallStatus.CONNECTED,
                connectedAtMs = System.currentTimeMillis()
            )
        }
    }

    override suspend fun declineIncomingCall() {
        val current = _activeCall.value ?: return
        val me = _currentUser.value ?: return
        _activeCall.value = null
        val record = CallRecord(
            id = current.callId,
            callerId = current.peerUid,
            callerName = current.peerName,
            callerPhone = current.peerPhone,
            callerAvatarUrl = current.peerAvatarUrl,
            calleeId = me.uid,
            calleeName = me.displayName,
            calleePhone = me.phoneNumber,
            callType = current.callType,
            direction = CallDirection.INCOMING,
            status = CallStatus.DECLINED,
            startedAt = System.currentTimeMillis()
        )
        _callLogs.update { listOf(record) + it }
    }

    override suspend fun endActiveCall() {
        val current = _activeCall.value ?: return
        val me = _currentUser.value ?: return
        _activeCall.value = null
        val now = System.currentTimeMillis()
        val durationSec = current.connectedAtMs?.let { ((now - it) / 1000L).coerceAtLeast(1L) } ?: 0L
        val status = when {
            current.connectedAtMs != null -> CallStatus.ENDED
            current.isIncoming -> CallStatus.MISSED
            else -> CallStatus.ENDED
        }
        val record = CallRecord(
            id = current.callId,
            callerId = if (current.isIncoming) current.peerUid else me.uid,
            callerName = if (current.isIncoming) current.peerName else me.displayName,
            callerPhone = if (current.isIncoming) current.peerPhone else me.phoneNumber,
            calleeId = if (current.isIncoming) me.uid else current.peerUid,
            calleeName = if (current.isIncoming) me.displayName else current.peerName,
            calleePhone = if (current.isIncoming) me.phoneNumber else current.peerPhone,
            callType = current.callType,
            direction = if (current.isIncoming) CallDirection.INCOMING else CallDirection.OUTGOING,
            status = status,
            startedAt = current.connectedAtMs ?: now,
            endedAt = now,
            durationSeconds = durationSec
        )
        _callLogs.update { listOf(record) + it }
    }

    override fun toggleCallMute() {
        _activeCall.update { it?.copy(isMuted = !it.isMuted) }
    }

    override fun toggleCallSpeaker() {
        _activeCall.update { it?.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    override fun toggleCallVideo() {
        _activeCall.update { it?.copy(isVideoEnabled = !it.isVideoEnabled) }
    }

    override fun switchCallCamera() {
        _activeCall.update { it?.copy(isFrontCamera = !it.isFrontCamera) }
    }

    override suspend fun simulateIncomingCall() {
        val peer = _contacts.value.firstOrNull() ?: return
        _activeCall.value = ActiveCallSession(
            callId = "call_${UUID.randomUUID().toString().take(8)}",
            peerUid = peer.uid,
            peerName = peer.displayName,
            peerPhone = peer.phoneNumber,
            peerAvatarUrl = peer.avatarUrl,
            callType = CallType.VIDEO,
            isIncoming = true,
            status = CallStatus.RINGING,
            isSpeakerOn = true,
            isVideoEnabled = true
        )
    }

    override suspend fun clearCallLog() {
        _callLogs.value = emptyList()
    }

    private fun sortChats(list: List<ChatThread>): List<ChatThread> =
        list.sortedWith(
            compareByDescending<ChatThread> { it.isPinned }
                .thenByDescending { it.lastMessageTime }
        )
}
