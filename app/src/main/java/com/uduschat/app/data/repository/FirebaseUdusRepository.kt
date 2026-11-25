package com.uduschat.app.data.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.uduschat.app.core.DeviceContact
import com.uduschat.app.core.PhoneNumberUtils
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
import com.uduschat.app.domain.model.VisibilityOption
import com.uduschat.app.domain.repository.UdusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Global internet backend powered by Firebase Phone Auth, Cloud Firestore,
 * Firebase Storage, and Firebase Cloud Messaging.
 */
class FirebaseUdusRepository(
    private val context: Context
) : UdusRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private val _authState = MutableStateFlow(AuthState(isDemoMode = false))
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _contacts = MutableStateFlow<List<UserProfile>>(emptyList())
    override val contacts: StateFlow<List<UserProfile>> = _contacts.asStateFlow()

    private val _chats = MutableStateFlow<List<ChatThread>>(emptyList())
    override val chats: StateFlow<List<ChatThread>> = _chats.asStateFlow()

    private val _statuses = MutableStateFlow<List<StatusUpdate>>(emptyList())
    override val statuses: StateFlow<List<StatusUpdate>> = _statuses.asStateFlow()

    private val _channels = MutableStateFlow<List<ChannelItem>>(
        listOf(
            ChannelItem(
                id = "ch_udus",
                name = "udu's chat Global",
                description = "Official global updates & release announcements",
                followerCount = 128400,
                isFollowing = true,
                latestUpdate = "Global cloud messaging is connected via Firebase.",
                latestTime = System.currentTimeMillis()
            )
        )
    )
    override val channels: StateFlow<List<ChannelItem>> = _channels.asStateFlow()

    private val _callLogs = MutableStateFlow<List<CallRecord>>(emptyList())
    override val callLogs: StateFlow<List<CallRecord>> = _callLogs.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
    override val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

    private val _starredMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val starredMessages: StateFlow<List<ChatMessage>> = _starredMessages.asStateFlow()

    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private val registrations = mutableListOf<ListenerRegistration>()

    init {
        scope.launch {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                loadAuthenticatedUser(firebaseUser.uid, firebaseUser.phoneNumber.orEmpty())
            }
        }
    }

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val meUid = auth.currentUser?.uid.orEmpty()
        val reg = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val now = System.currentTimeMillis()
                val msgs = snapshot.documents.mapNotNull { doc -> doc.toChatMessage(chatId) }
                    .filter { msg -> meUid !in msg.deletedFor && (msg.expiresAt == null || msg.expiresAt > now) }
                trySend(msgs)
            }
        awaitClose { reg.remove() }
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
        if (activity == null) {
            _authState.update { it.copy(errorMessage = "Activity unavailable for SMS verification.") }
            return
        }
        _authState.update { it.copy(isLoading = true, pendingPhoneE164 = phoneE164, errorMessage = null) }
        verifyWithFirebase(phoneE164, activity, token = null)
    }

    override suspend fun resendOtp(activity: Activity?) {
        val phone = _authState.value.pendingPhoneE164
        if (phone.isBlank() || activity == null) return
        _authState.update { it.copy(isLoading = true, errorMessage = null) }
        verifyWithFirebase(phone, activity, token = resendToken)
    }

    private fun verifyWithFirebase(
        phoneE164: String,
        activity: Activity,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                scope.launch {
                    signInWithCredential(credential)
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _authState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Phone verification failed."
                    )
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                resendToken = token
                _authState.update {
                    it.copy(
                        step = AuthStep.OTP_VERIFY,
                        verificationId = verificationId,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (token != null) {
            builder.setForceResendingToken(token)
        }
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    override suspend fun verifyOtp(code: String) {
        val vid = _authState.value.verificationId
        if (vid.isNullOrBlank()) {
            _authState.update { it.copy(errorMessage = "Verification session expired. Resend code.") }
            return
        }
        _authState.update { it.copy(isLoading = true, errorMessage = null) }
        val credential = PhoneAuthProvider.getCredential(vid, code.trim())
        signInWithCredential(credential)
    }

    private suspend fun signInWithCredential(credential: PhoneAuthCredential) {
        runCatching {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: error("Authentication failed")
            loadAuthenticatedUser(user.uid, user.phoneNumber ?: _authState.value.pendingPhoneE164)
        }.onFailure { err ->
            _authState.update {
                it.copy(isLoading = false, errorMessage = err.localizedMessage ?: "Invalid OTP code")
            }
        }
    }

    private suspend fun loadAuthenticatedUser(uid: String, phone: String) {
        val snap = runCatching { firestore.collection("users").document(uid).get().await() }.getOrNull()
        val fcmToken = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        if (snap != null && snap.exists() && !snap.getString("displayName").isNullOrBlank()) {
            val profile = snap.toUserProfile().copy(isOnline = true, fcmToken = fcmToken ?: snap.getString("fcmToken"))
            _currentUser.value = profile
            _authState.value = AuthState(step = AuthStep.AUTHENTICATED, pendingPhoneE164 = profile.phoneNumber, isDemoMode = false)
            firestore.collection("users").document(uid)
                .set(mapOf("isOnline" to true, "lastSeenAt" to System.currentTimeMillis(), "fcmToken" to fcmToken), SetOptions.merge())
            attachRealtimeListeners(uid)
        } else {
            _authState.update {
                it.copy(
                    step = AuthStep.PROFILE_SETUP,
                    pendingPhoneE164 = phone,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    override fun backToPhoneInput() {
        _authState.update { it.copy(step = AuthStep.PHONE_INPUT, errorMessage = null) }
    }

    override suspend fun completeProfileSetup(
        displayName: String,
        about: String,
        avatarUri: String?
    ) {
        val user = auth.currentUser ?: return
        val cleanName = displayName.trim().ifBlank { "Udu User" }
        val cleanAbout = about.trim().ifBlank { "Hey there! I am using udu's chat." }
        val uploadedAvatar = avatarUri?.let { uploadFileIfNeeded("avatars/${user.uid}/profile.jpg", it) }
        val fcmToken = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        val phone = user.phoneNumber ?: _authState.value.pendingPhoneE164
        val now = System.currentTimeMillis()
        val profile = UserProfile(
            uid = user.uid,
            phoneNumber = phone,
            displayName = cleanName,
            about = cleanAbout,
            avatarUrl = uploadedAvatar,
            isOnline = true,
            lastSeenAt = now,
            fcmToken = fcmToken
        )
        firestore.collection("users").document(user.uid).set(profile.toFirestoreMap()).await()
        _currentUser.value = profile
        _authState.value = AuthState(step = AuthStep.AUTHENTICATED, pendingPhoneE164 = phone, isDemoMode = false)
        attachRealtimeListeners(user.uid)
    }

    override suspend fun updateProfile(displayName: String, about: String, avatarUri: String?) {
        val current = _currentUser.value ?: return
        val uploadedAvatar = avatarUri?.let { uploadFileIfNeeded("avatars/${current.uid}/profile.jpg", it) }
        val updated = current.copy(
            displayName = displayName.trim().ifBlank { current.displayName },
            about = about.trim().ifBlank { current.about },
            avatarUrl = uploadedAvatar ?: current.avatarUrl
        )
        _currentUser.value = updated
        firestore.collection("users").document(current.uid).set(updated.toFirestoreMap(), SetOptions.merge())
    }

    override suspend fun updatePrivacy(transform: (PrivacySettings) -> PrivacySettings) {
        val current = _currentUser.value ?: return
        val updated = current.copy(privacy = transform(current.privacy))
        _currentUser.value = updated
        firestore.collection("users").document(current.uid)
            .update("privacy", updated.privacy.toFirestoreMap())
    }

    override suspend fun setOnlinePresence(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            firestore.collection("users").document(uid)
                .update(mapOf("isOnline" to isOnline, "lastSeenAt" to System.currentTimeMillis()))
        }
    }

    override suspend fun signOut() {
        setOnlinePresence(false)
        clearListeners()
        auth.signOut()
        _currentUser.value = null
        _chats.value = emptyList()
        _statuses.value = emptyList()
        _callLogs.value = emptyList()
        _authState.value = AuthState(step = AuthStep.WELCOME, isDemoMode = false)
    }

    override suspend fun deleteAccount() {
        val uid = auth.currentUser?.uid ?: return
        runCatching { firestore.collection("users").document(uid).delete().await() }
        runCatching { auth.currentUser?.delete()?.await() }
        signOut()
    }

    private fun attachRealtimeListeners(myUid: String) {
        clearListeners()

        // 1. User profile
        registrations += firestore.collection("users").document(myUid)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    _currentUser.value = snap.toUserProfile()
                }
            }

        // 1b. Registered udu's chat users directory for phone/name search
        registrations += firestore.collection("users")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val users = snap.documents
                    .mapNotNull { doc -> runCatching { doc.toUserProfile() }.getOrNull() }
                    .filter { it.uid != myUid && it.phoneNumber.isNotBlank() }
                    .sortedBy { it.displayName.lowercase() }
                _contacts.value = users
            }

        // 2. Chats
        registrations += firestore.collection("chats")
            .whereArrayContains("participantIds", myUid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch {
                    val threads = snap.documents.mapNotNull { doc -> doc.toChatThread(myUid) }
                        .sortedWith(
                            compareByDescending<ChatThread> { it.isPinned }
                                .thenByDescending { it.lastMessageTime }
                        )
                    _chats.value = threads
                }
            }

        // 3. Statuses
        registrations += firestore.collection("statuses")
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                _statuses.value = snap.documents.mapNotNull { it.toStatusUpdate() }
                    .sortedByDescending { it.createdAt }
            }

        // 4. Calls & live incoming call signaling
        registrations += firestore.collection("calls")
            .whereArrayContains("participants", myUid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val records = snap.documents.mapNotNull { it.toCallRecord(myUid) }
                    .sortedByDescending { it.startedAt }
                _callLogs.value = records

                val ringing = snap.documents.firstOrNull { doc ->
                    doc.getString("calleeId") == myUid &&
                        doc.getString("status") == CallStatus.RINGING.name
                }
                if (ringing != null && _activeCall.value == null) {
                    val type = runCatching { CallType.valueOf(ringing.getString("callType") ?: "VOICE") }
                        .getOrDefault(CallType.VOICE)
                    _activeCall.value = ActiveCallSession(
                        callId = ringing.id,
                        peerUid = ringing.getString("callerId").orEmpty(),
                        peerName = ringing.getString("callerName").orEmpty(),
                        peerPhone = ringing.getString("callerPhone").orEmpty(),
                        peerAvatarUrl = ringing.getString("callerAvatarUrl"),
                        callType = type,
                        isIncoming = true,
                        status = CallStatus.RINGING,
                        isSpeakerOn = type == CallType.VIDEO,
                        isVideoEnabled = type == CallType.VIDEO
                    )
                } else {
                    val active = _activeCall.value
                    if (active != null) {
                        val matching = snap.documents.firstOrNull { it.id == active.callId }
                        val remoteStatus = matching?.getString("status")
                            ?.let { runCatching { CallStatus.valueOf(it) }.getOrNull() }
                        if (remoteStatus == CallStatus.CONNECTED && active.status != CallStatus.CONNECTED) {
                            _activeCall.value = active.copy(
                                status = CallStatus.CONNECTED,
                                connectedAtMs = System.currentTimeMillis()
                            )
                        } else if (remoteStatus == CallStatus.ENDED || remoteStatus == CallStatus.DECLINED || remoteStatus == CallStatus.MISSED) {
                            _activeCall.value = null
                        }
                    }
                }
            }
    }

    private fun clearListeners() {
        registrations.forEach { it.remove() }
        registrations.clear()
    }

    override suspend fun findUserByPhone(phoneE164: String): Result<UserProfile?> = runCatching {
        if (!PhoneNumberUtils.isValidE164(phoneE164)) {
            throw IllegalArgumentException("Invalid international phone number")
        }
        val query = firestore.collection("users")
            .whereEqualTo("phoneNumber", phoneE164)
            .limit(1)
            .get()
            .await()
        val found = query.documents.firstOrNull()?.toUserProfile()
        if (found != null && _contacts.value.none { it.uid == found.uid }) {
            _contacts.update { it + found }
        }
        found
    }

    override suspend fun syncDeviceContacts(deviceContacts: List<DeviceContact>): Result<List<UserProfile>> = runCatching {
        val myPhone = _currentUser.value?.phoneNumber
        val numbers = deviceContacts.map { it.phoneNumberE164 }.filter { it != myPhone }.distinct()
        val foundUsers = mutableListOf<UserProfile>()
        numbers.chunked(10).forEach { chunk ->
            val snap = firestore.collection("users")
                .whereIn("phoneNumber", chunk)
                .get()
                .await()
            snap.documents.mapTo(foundUsers) { it.toUserProfile() }
        }
        _contacts.value = foundUsers.distinctBy { it.uid }.sortedBy { it.displayName }
        _contacts.value
    }

    override suspend fun startOrOpenDirectChat(user: UserProfile): Result<String> = runCatching {
        val me = _currentUser.value ?: error("Not signed in")
        val chatId = listOf(me.uid, user.uid).sorted().joinToString("_")
        val ref = firestore.collection("chats").document(chatId)
        val snap = ref.get().await()
        if (!snap.exists()) {
            val payload = mapOf(
                "type" to ChatType.DIRECT.name,
                "participantIds" to listOf(me.uid, user.uid),
                "adminIds" to emptyList<String>(),
                "participantNames" to mapOf(me.uid to me.displayName, user.uid to user.displayName),
                "participantPhones" to mapOf(me.uid to me.phoneNumber, user.uid to user.phoneNumber),
                "participantAvatars" to mapOf(me.uid to me.avatarUrl.orEmpty(), user.uid to user.avatarUrl.orEmpty()),
                "lastMessagePreview" to "",
                "lastMessageSenderId" to "",
                "lastMessageType" to MessageType.TEXT.name,
                "lastMessageStatus" to MessageStatus.SENT.name,
                "lastMessageTime" to System.currentTimeMillis(),
                "disappearingSeconds" to 0L
            )
            ref.set(payload).await()
        }
        chatId
    }

    override suspend fun createGroupChat(
        title: String,
        description: String,
        memberUids: List<String>,
        avatarUri: String?
    ): Result<String> = runCatching {
        val me = _currentUser.value ?: error("Not signed in")
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Group name required" }

        val chatId = "grp_${UUID.randomUUID()}"
        val uploadedAvatar = avatarUri?.let { uploadFileIfNeeded("chats/$chatId/group_avatar.jpg", it) }
        val members = (listOf(me.uid) + memberUids).distinct()
        val now = System.currentTimeMillis()
        val payload = mapOf(
            "type" to ChatType.GROUP.name,
            "title" to cleanTitle,
            "description" to description.trim(),
            "avatarUrl" to uploadedAvatar,
            "participantIds" to members,
            "adminIds" to listOf(me.uid),
            "lastMessagePreview" to "${me.displayName} created group \"$cleanTitle\"",
            "lastMessageSenderId" to me.uid,
            "lastMessageType" to MessageType.SYSTEM.name,
            "lastMessageStatus" to MessageStatus.SENT.name,
            "lastMessageTime" to now,
            "disappearingSeconds" to 0L
        )
        firestore.collection("chats").document(chatId).set(payload).await()
        chatId
    }

    override suspend fun sendBroadcastMessage(recipientUids: List<String>, text: String): Result<Int> = runCatching {
        val clean = text.trim()
        if (clean.isBlank() || recipientUids.isEmpty()) return@runCatching 0
        var count = 0
        recipientUids.forEach { uid ->
            val user = _contacts.value.firstOrNull { it.uid == uid } ?: return@forEach
            val chatId = startOrOpenDirectChat(user).getOrNull() ?: return@forEach
            sendMessage(chatId = chatId, text = clean)
            count++
        }
        count
    }

    override suspend fun sendMessage(
        chatId: String,
        text: String,
        attachment: MessageDraftAttachment?,
        replyTo: ChatMessage?
    ) {
        val me = _currentUser.value ?: return
        val cleanText = text.trim()
        if (cleanText.isBlank() && attachment == null) return

        val msgId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val thread = _chats.value.firstOrNull { it.id == chatId }
        val expiresAt = thread?.disappearingSeconds?.takeIf { it > 0L }?.let { now + it * 1000L }

        val uploadedUrl = attachment?.uriOrPath?.let { path ->
            val name = attachment.fileName ?: "media_${System.currentTimeMillis()}"
            uploadFileIfNeeded("chats/$chatId/${msgId}_$name", path)
        }

        val message = ChatMessage(
            id = msgId,
            chatId = chatId,
            senderId = me.uid,
            senderName = me.displayName,
            senderPhone = me.phoneNumber,
            type = attachment?.type ?: MessageType.TEXT,
            text = cleanText,
            mediaUrl = uploadedUrl,
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

        val chatRef = firestore.collection("chats").document(chatId)
        chatRef.collection("messages").document(msgId).set(message.toFirestoreMap()).await()
        chatRef.update(
            mapOf(
                "lastMessagePreview" to message.displayPreview(),
                "lastMessageSenderId" to me.uid,
                "lastMessageType" to message.type.name,
                "lastMessageStatus" to MessageStatus.SENT.name,
                "lastMessageTime" to now
            )
        )
    }

    override suspend fun markChatAsRead(chatId: String) {
        val meUid = auth.currentUser?.uid ?: return
        val readReceipts = _currentUser.value?.privacy?.readReceiptsEnabled ?: true
        val snap = firestore.collection("chats").document(chatId).collection("messages").get().await()
        val batch = firestore.batch()
        snap.documents.forEach { doc ->
            val senderId = doc.getString("senderId")
            val status = doc.getString("status")
            if (senderId != meUid && status != MessageStatus.READ.name && readReceipts) {
                batch.update(doc.reference, "status", MessageStatus.READ.name)
            }
        }
        batch.update(firestore.collection("chats").document(chatId), "unread_$meUid", 0)
        batch.commit().await()
    }

    override suspend fun setTyping(chatId: String, isTyping: Boolean) {
        val meUid = auth.currentUser?.uid ?: return
        val op = if (isTyping) FieldValue.arrayUnion(meUid) else FieldValue.arrayRemove(meUid)
        runCatching { firestore.collection("chats").document(chatId).update("typingUserIds", op) }
    }

    override suspend fun saveDraft(chatId: String, draft: String) {
        val meUid = auth.currentUser?.uid ?: return
        runCatching { firestore.collection("chats").document(chatId).update("draft_$meUid", draft) }
    }

    override suspend fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val meUid = auth.currentUser?.uid ?: return
        val ref = firestore.collection("chats").document(chatId).collection("messages").document(messageId)
        val doc = ref.get().await()
        val current = (doc.get("reactions") as? Map<*, *>)?.get(meUid) as? String
        if (current == emoji) {
            ref.update("reactions.$meUid", FieldValue.delete())
        } else {
            ref.update("reactions.$meUid", emoji)
        }
    }

    override suspend fun toggleStarMessage(chatId: String, messageId: String) {
        val meUid = auth.currentUser?.uid ?: return
        val ref = firestore.collection("chats").document(chatId).collection("messages").document(messageId)
        val doc = ref.get().await()
        val starred = (doc.get("starredBy") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val op = if (meUid in starred) FieldValue.arrayRemove(meUid) else FieldValue.arrayUnion(meUid)
        ref.update("starredBy", op)
    }

    override suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        val clean = newText.trim()
        if (clean.isBlank()) return
        firestore.collection("chats").document(chatId).collection("messages").document(messageId)
            .update(mapOf("text" to clean, "isEdited" to true))
    }

    override suspend fun deleteMessage(chatId: String, messageId: String, forEveryone: Boolean) {
        val meUid = auth.currentUser?.uid ?: return
        val ref = firestore.collection("chats").document(chatId).collection("messages").document(messageId)
        if (forEveryone) {
            ref.update(mapOf("isDeletedForEveryone" to true, "text" to "", "mediaUrl" to null))
        } else {
            ref.update("deletedFor", FieldValue.arrayUnion(meUid))
        }
    }

    override suspend fun forwardMessages(targetChatIds: List<String>, messages: List<ChatMessage>) {
        targetChatIds.forEach { targetId ->
            messages.forEach { msg ->
                sendMessage(
                    chatId = targetId,
                    text = msg.text,
                    attachment = if (msg.type != MessageType.TEXT) {
                        MessageDraftAttachment(
                            type = msg.type,
                            uriOrPath = msg.mediaUrl,
                            fileName = msg.fileName,
                            fileSizeBytes = msg.fileSizeBytes,
                            mimeType = msg.mediaMimeType,
                            durationMs = msg.mediaDurationMs,
                            locationLat = msg.locationLat,
                            locationLng = msg.locationLng,
                            locationLabel = msg.locationLabel,
                            sharedContactName = msg.sharedContactName,
                            sharedContactPhone = msg.sharedContactPhone
                        )
                    } else null
                )
            }
        }
    }

    override suspend fun markViewOnceOpened(chatId: String, messageId: String) {
        val meUid = auth.currentUser?.uid ?: return
        firestore.collection("chats").document(chatId).collection("messages").document(messageId)
            .update("viewedBy", FieldValue.arrayUnion(meUid))
    }

    override suspend fun togglePinChat(chatId: String) = toggleBooleanUserChatFlag(chatId, "pinned")
    override suspend fun toggleMuteChat(chatId: String) = toggleBooleanUserChatFlag(chatId, "muted")
    override suspend fun toggleArchiveChat(chatId: String) = toggleBooleanUserChatFlag(chatId, "archived")
    override suspend fun toggleFavoriteChat(chatId: String) = toggleBooleanUserChatFlag(chatId, "favorite")

    private suspend fun toggleBooleanUserChatFlag(chatId: String, key: String) {
        val meUid = auth.currentUser?.uid ?: return
        val ref = firestore.collection("chats").document(chatId)
        val snap = ref.get().await()
        val field = "${key}_$meUid"
        val current = snap.getBoolean(field) ?: false
        ref.update(field, !current)
    }

    override suspend fun markChatUnread(chatId: String) {
        val meUid = auth.currentUser?.uid ?: return
        firestore.collection("chats").document(chatId).update("unread_$meUid", 1)
    }

    override suspend fun clearChatMessages(chatId: String) {
        val meUid = auth.currentUser?.uid ?: return
        val snap = firestore.collection("chats").document(chatId).collection("messages").get().await()
        val batch = firestore.batch()
        snap.documents.forEach { doc ->
            batch.update(doc.reference, "deletedFor", FieldValue.arrayUnion(meUid))
        }
        batch.commit().await()
    }

    override suspend fun deleteChat(chatId: String) {
        val meUid = auth.currentUser?.uid ?: return
        firestore.collection("chats").document(chatId).update("participantIds", FieldValue.arrayRemove(meUid))
    }

    override suspend fun setDisappearingTimer(chatId: String, seconds: Long) {
        firestore.collection("chats").document(chatId).update("disappearingSeconds", seconds)
    }

    override suspend fun setChatWallpaper(chatId: String, wallpaperId: String?) {
        val meUid = auth.currentUser?.uid ?: return
        firestore.collection("chats").document(chatId).update("wallpaper_$meUid", wallpaperId)
    }

    override suspend fun blockUser(uid: String, block: Boolean) {
        updatePrivacy { privacy ->
            val updated = if (block) (privacy.blockedUids + uid).distinct() else privacy.blockedUids - uid
            privacy.copy(blockedUids = updated)
        }
    }

    override suspend fun updateGroupInfo(chatId: String, title: String, description: String) {
        firestore.collection("chats").document(chatId)
            .update(mapOf("title" to title.trim(), "description" to description.trim()))
    }

    override suspend fun addGroupMembers(chatId: String, newMemberUids: List<String>) {
        if (newMemberUids.isEmpty()) return
        firestore.collection("chats").document(chatId)
            .update("participantIds", FieldValue.arrayUnion(*newMemberUids.toTypedArray()))
    }

    override suspend fun removeGroupMember(chatId: String, memberUid: String) {
        firestore.collection("chats").document(chatId).update(
            mapOf(
                "participantIds" to FieldValue.arrayRemove(memberUid),
                "adminIds" to FieldValue.arrayRemove(memberUid)
            )
        )
    }

    override suspend fun toggleGroupAdmin(chatId: String, memberUid: String) {
        val ref = firestore.collection("chats").document(chatId)
        val snap = ref.get().await()
        val admins = (snap.get("adminIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val op = if (memberUid in admins) FieldValue.arrayRemove(memberUid) else FieldValue.arrayUnion(memberUid)
        ref.update("adminIds", op)
    }

    override suspend fun leaveGroup(chatId: String) {
        val meUid = auth.currentUser?.uid ?: return
        removeGroupMember(chatId, meUid)
    }

    override suspend fun postTextStatus(text: String, backgroundHex: Long) {
        val me = _currentUser.value ?: return
        val clean = text.trim()
        if (clean.isBlank()) return
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val map = mapOf(
            "userId" to me.uid,
            "userName" to me.displayName,
            "userPhone" to me.phoneNumber,
            "userAvatarUrl" to me.avatarUrl,
            "type" to StatusType.TEXT.name,
            "text" to clean,
            "backgroundHex" to backgroundHex,
            "caption" to "",
            "createdAt" to now,
            "expiresAt" to now + 24L * 3600_000L,
            "viewers" to emptyMap<String, Long>()
        )
        firestore.collection("statuses").document(id).set(map)
    }

    override suspend fun postImageStatus(mediaUri: String, caption: String) {
        val me = _currentUser.value ?: return
        val id = UUID.randomUUID().toString()
        val url = uploadFileIfNeeded("statuses/${me.uid}/$id.jpg", mediaUri) ?: return
        val now = System.currentTimeMillis()
        val map = mapOf(
            "userId" to me.uid,
            "userName" to me.displayName,
            "userPhone" to me.phoneNumber,
            "userAvatarUrl" to me.avatarUrl,
            "type" to StatusType.IMAGE.name,
            "text" to "",
            "mediaUrl" to url,
            "caption" to caption.trim(),
            "createdAt" to now,
            "expiresAt" to now + 24L * 3600_000L,
            "viewers" to emptyMap<String, Long>()
        )
        firestore.collection("statuses").document(id).set(map)
    }

    override suspend fun markStatusViewed(statusId: String) {
        val meUid = auth.currentUser?.uid ?: return
        firestore.collection("statuses").document(statusId)
            .update("viewers.$meUid", System.currentTimeMillis())
    }

    override suspend fun deleteStatus(statusId: String) {
        firestore.collection("statuses").document(statusId).delete()
    }

    override suspend fun replyToStatus(status: StatusUpdate, replyText: String): Result<String> = runCatching {
        val peer = UserProfile(
            uid = status.userId,
            phoneNumber = status.userPhone,
            displayName = status.userName,
            avatarUrl = status.userAvatarUrl
        )
        val chatId = startOrOpenDirectChat(peer).getOrThrow()
        val quoted = ChatMessage(
            id = status.id,
            chatId = chatId,
            senderId = status.userId,
            senderName = "${status.userName} \u2022 Status",
            text = if (status.type == StatusType.TEXT) status.text else status.caption.ifBlank { "\uD83D\uDCF7 Status photo" }
        )
        sendMessage(chatId, replyText, replyTo = quoted)
        chatId
    }

    override suspend fun toggleChannelFollow(channelId: String) {
        _channels.update { list ->
            list.map { ch ->
                if (ch.id != channelId) ch else ch.copy(isFollowing = !ch.isFollowing)
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
        val me = _currentUser.value ?: return
        val callId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
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
        val payload = mapOf(
            "callerId" to me.uid,
            "callerName" to me.displayName,
            "callerPhone" to me.phoneNumber,
            "callerAvatarUrl" to me.avatarUrl,
            "calleeId" to peerUid,
            "calleeName" to peerName,
            "calleePhone" to peerPhone,
            "calleeAvatarUrl" to peerAvatarUrl,
            "participants" to listOf(me.uid, peerUid),
            "callType" to callType.name,
            "status" to CallStatus.RINGING.name,
            "startedAt" to now,
            "durationSeconds" to 0L
        )
        firestore.collection("calls").document(callId).set(payload)
    }

    override suspend fun acceptIncomingCall() {
        val active = _activeCall.value ?: return
        _activeCall.value = active.copy(status = CallStatus.CONNECTED, connectedAtMs = System.currentTimeMillis())
        firestore.collection("calls").document(active.callId)
            .update("status", CallStatus.CONNECTED.name)
    }

    override suspend fun declineIncomingCall() {
        val active = _activeCall.value ?: return
        _activeCall.value = null
        firestore.collection("calls").document(active.callId)
            .update(mapOf("status" to CallStatus.DECLINED.name, "endedAt" to System.currentTimeMillis()))
    }

    override suspend fun endActiveCall() {
        val active = _activeCall.value ?: return
        _activeCall.value = null
        val now = System.currentTimeMillis()
        val dur = active.connectedAtMs?.let { ((now - it) / 1000L).coerceAtLeast(1L) } ?: 0L
        val finalStatus = if (active.connectedAtMs != null) CallStatus.ENDED else CallStatus.MISSED
        firestore.collection("calls").document(active.callId)
            .update(mapOf("status" to finalStatus.name, "endedAt" to now, "durationSeconds" to dur))
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
        // Real Firebase mode uses Firestore call signaling.
    }

    override suspend fun clearCallLog() {
        // Local filter clear.
        _callLogs.value = emptyList()
    }

    private suspend fun uploadFileIfNeeded(remotePath: String, uriString: String): String? {
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) return uriString
        return runCatching {
            val uri = if (uriString.startsWith("/")) Uri.fromFile(File(uriString)) else Uri.parse(uriString)
            val ref = storage.reference.child(remotePath)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }.getOrNull()
    }

    private fun UserProfile.toFirestoreMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "phoneNumber" to phoneNumber,
        "displayName" to displayName,
        "about" to about,
        "avatarUrl" to avatarUrl,
        "isOnline" to isOnline,
        "lastSeenAt" to lastSeenAt,
        "fcmToken" to fcmToken,
        "privacy" to privacy.toFirestoreMap()
    )

    private fun PrivacySettings.toFirestoreMap(): Map<String, Any> = mapOf(
        "lastSeenVisibility" to lastSeenVisibility.name,
        "profilePhotoVisibility" to profilePhotoVisibility.name,
        "aboutVisibility" to aboutVisibility.name,
        "readReceiptsEnabled" to readReceiptsEnabled,
        "defaultDisappearingSeconds" to defaultDisappearingSeconds,
        "blockedUids" to blockedUids
    )

    private fun ChatMessage.toFirestoreMap(): Map<String, Any?> = mapOf(
        "senderId" to senderId,
        "senderName" to senderName,
        "senderPhone" to senderPhone,
        "type" to type.name,
        "text" to text,
        "mediaUrl" to mediaUrl,
        "mediaMimeType" to mediaMimeType,
        "mediaDurationMs" to mediaDurationMs,
        "fileName" to fileName,
        "fileSizeBytes" to fileSizeBytes,
        "locationLat" to locationLat,
        "locationLng" to locationLng,
        "locationLabel" to locationLabel,
        "sharedContactName" to sharedContactName,
        "sharedContactPhone" to sharedContactPhone,
        "replyToMessageId" to replyToMessageId,
        "replyToSenderName" to replyToSenderName,
        "replyToPreview" to replyToPreview,
        "reactions" to reactions,
        "starredBy" to starredBy,
        "deletedFor" to deletedFor,
        "isDeletedForEveryone" to isDeletedForEveryone,
        "isEdited" to isEdited,
        "isForwarded" to isForwarded,
        "viewOnce" to viewOnce,
        "viewedBy" to viewedBy,
        "status" to status.name,
        "createdAt" to createdAt,
        "expiresAt" to expiresAt
    )

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toUserProfile(): UserProfile {
        val privMap = get("privacy") as? Map<String, Any?>
        val privacy = PrivacySettings(
            lastSeenVisibility = (privMap?.get("lastSeenVisibility") as? String)
                ?.let { runCatching { VisibilityOption.valueOf(it) }.getOrNull() }
                ?: VisibilityOption.EVERYONE,
            profilePhotoVisibility = (privMap?.get("profilePhotoVisibility") as? String)
                ?.let { runCatching { VisibilityOption.valueOf(it) }.getOrNull() }
                ?: VisibilityOption.EVERYONE,
            aboutVisibility = (privMap?.get("aboutVisibility") as? String)
                ?.let { runCatching { VisibilityOption.valueOf(it) }.getOrNull() }
                ?: VisibilityOption.EVERYONE,
            readReceiptsEnabled = privMap?.get("readReceiptsEnabled") as? Boolean ?: true,
            defaultDisappearingSeconds = (privMap?.get("defaultDisappearingSeconds") as? Number)?.toLong() ?: 0L,
            blockedUids = (privMap?.get("blockedUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
        )
        return UserProfile(
            uid = getString("uid") ?: id,
            phoneNumber = getString("phoneNumber").orEmpty(),
            displayName = getString("displayName").orEmpty(),
            about = getString("about") ?: "Hey there! I am using udu's chat.",
            avatarUrl = getString("avatarUrl"),
            isOnline = getBoolean("isOnline") ?: false,
            lastSeenAt = getLong("lastSeenAt") ?: 0L,
            fcmToken = getString("fcmToken"),
            privacy = privacy
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun DocumentSnapshot.toChatThread(myUid: String): ChatThread? {
        val participantIds = (get("participantIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val type = runCatching { ChatType.valueOf(getString("type") ?: "DIRECT") }.getOrDefault(ChatType.DIRECT)
        val otherUid = if (type == ChatType.DIRECT) participantIds.firstOrNull { it != myUid } else null
        val names = get("participantNames") as? Map<String, String>
        val phones = get("participantPhones") as? Map<String, String>
        val avatars = get("participantAvatars") as? Map<String, String>

        var otherOnline = false
        var otherLastSeen = 0L
        var resolvedTitle = getString("title").orEmpty()
        var resolvedAvatar = getString("avatarUrl")
        if (type == ChatType.DIRECT && otherUid != null) {
            val cached = _contacts.value.firstOrNull { it.uid == otherUid }
            if (cached != null) {
                resolvedTitle = cached.displayName
                resolvedAvatar = cached.avatarUrl
                otherOnline = cached.isOnline
                otherLastSeen = cached.lastSeenAt
            } else {
                val peerSnap = runCatching { firestore.collection("users").document(otherUid).get().await() }.getOrNull()
                if (peerSnap != null && peerSnap.exists()) {
                    val peer = peerSnap.toUserProfile()
                    _contacts.update { list -> if (list.any { it.uid == peer.uid }) list else list + peer }
                    resolvedTitle = peer.displayName
                    resolvedAvatar = peer.avatarUrl
                    otherOnline = peer.isOnline
                    otherLastSeen = peer.lastSeenAt
                } else {
                    resolvedTitle = names?.get(otherUid) ?: phones?.get(otherUid) ?: "Chat"
                    resolvedAvatar = avatars?.get(otherUid)?.takeIf { it.isNotBlank() }
                }
            }
        }

        return ChatThread(
            id = id,
            type = type,
            title = resolvedTitle.ifBlank { "udu's chat" },
            description = getString("description").orEmpty(),
            avatarUrl = resolvedAvatar,
            participantIds = participantIds,
            adminIds = (get("adminIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            otherUserId = otherUid,
            otherUserPhone = otherUid?.let { phones?.get(it) },
            isOnline = otherOnline,
            lastSeenAt = otherLastSeen,
            typingUserIds = ((get("typingUserIds") as? List<*>)?.filterIsInstance<String>().orEmpty()) - myUid,
            lastMessagePreview = getString("lastMessagePreview").orEmpty(),
            lastMessageSenderId = getString("lastMessageSenderId").orEmpty(),
            lastMessageType = runCatching { MessageType.valueOf(getString("lastMessageType") ?: "TEXT") }
                .getOrDefault(MessageType.TEXT),
            lastMessageStatus = runCatching { MessageStatus.valueOf(getString("lastMessageStatus") ?: "SENT") }
                .getOrDefault(MessageStatus.SENT),
            lastMessageTime = getLong("lastMessageTime") ?: 0L,
            unreadCount = getLong("unread_$myUid")?.toInt() ?: 0,
            isPinned = getBoolean("pinned_$myUid") ?: false,
            isMuted = getBoolean("muted_$myUid") ?: false,
            isArchived = getBoolean("archived_$myUid") ?: false,
            isFavorite = getBoolean("favorite_$myUid") ?: false,
            disappearingSeconds = getLong("disappearingSeconds") ?: 0L,
            draftText = getString("draft_$myUid").orEmpty(),
            wallpaperId = getString("wallpaper_$myUid")
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toChatMessage(chatId: String): ChatMessage? {
        val rawReactions = (get("reactions") as? Map<*, *>)
            ?.mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }
            ?.toMap()
            ?: emptyMap()
        return ChatMessage(
            id = id,
            chatId = chatId,
            senderId = getString("senderId").orEmpty(),
            senderName = getString("senderName").orEmpty(),
            senderPhone = getString("senderPhone").orEmpty(),
            type = runCatching { MessageType.valueOf(getString("type") ?: "TEXT") }.getOrDefault(MessageType.TEXT),
            text = getString("text").orEmpty(),
            mediaUrl = getString("mediaUrl"),
            mediaMimeType = getString("mediaMimeType"),
            mediaDurationMs = getLong("mediaDurationMs") ?: 0L,
            fileName = getString("fileName"),
            fileSizeBytes = getLong("fileSizeBytes") ?: 0L,
            locationLat = getDouble("locationLat"),
            locationLng = getDouble("locationLng"),
            locationLabel = getString("locationLabel"),
            sharedContactName = getString("sharedContactName"),
            sharedContactPhone = getString("sharedContactPhone"),
            replyToMessageId = getString("replyToMessageId"),
            replyToSenderName = getString("replyToSenderName"),
            replyToPreview = getString("replyToPreview"),
            reactions = rawReactions,
            starredBy = (get("starredBy") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            deletedFor = (get("deletedFor") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            isDeletedForEveryone = getBoolean("isDeletedForEveryone") ?: false,
            isEdited = getBoolean("isEdited") ?: false,
            isForwarded = getBoolean("isForwarded") ?: false,
            viewOnce = getBoolean("viewOnce") ?: false,
            viewedBy = (get("viewedBy") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            status = runCatching { MessageStatus.valueOf(getString("status") ?: "SENT") }.getOrDefault(MessageStatus.SENT),
            createdAt = getLong("createdAt") ?: 0L,
            expiresAt = getLong("expiresAt")
        )
    }

    private fun DocumentSnapshot.toStatusUpdate(): StatusUpdate? {
        val rawViewers = (get("viewers") as? Map<*, *>)
            ?.mapNotNull { (k, v) -> if (k is String && v is Number) k to v.toLong() else null }
            ?.toMap()
            ?: emptyMap()
        return StatusUpdate(
            id = id,
            userId = getString("userId").orEmpty(),
            userName = getString("userName").orEmpty(),
            userPhone = getString("userPhone").orEmpty(),
            userAvatarUrl = getString("userAvatarUrl"),
            type = runCatching { StatusType.valueOf(getString("type") ?: "TEXT") }.getOrDefault(StatusType.TEXT),
            text = getString("text").orEmpty(),
            backgroundHex = getLong("backgroundHex") ?: 0xFF008069,
            mediaUrl = getString("mediaUrl"),
            caption = getString("caption").orEmpty(),
            createdAt = getLong("createdAt") ?: 0L,
            expiresAt = getLong("expiresAt") ?: 0L,
            viewers = rawViewers
        )
    }

    private fun DocumentSnapshot.toCallRecord(myUid: String): CallRecord? {
        val callerId = getString("callerId").orEmpty()
        return CallRecord(
            id = id,
            callerId = callerId,
            callerName = getString("callerName").orEmpty(),
            callerPhone = getString("callerPhone").orEmpty(),
            callerAvatarUrl = getString("callerAvatarUrl"),
            calleeId = getString("calleeId").orEmpty(),
            calleeName = getString("calleeName").orEmpty(),
            calleePhone = getString("calleePhone").orEmpty(),
            calleeAvatarUrl = getString("calleeAvatarUrl"),
            callType = runCatching { CallType.valueOf(getString("callType") ?: "VOICE") }.getOrDefault(CallType.VOICE),
            direction = if (callerId == myUid) CallDirection.OUTGOING else CallDirection.INCOMING,
            status = runCatching { CallStatus.valueOf(getString("status") ?: "ENDED") }.getOrDefault(CallStatus.ENDED),
            startedAt = getLong("startedAt") ?: 0L,
            endedAt = getLong("endedAt"),
            durationSeconds = getLong("durationSeconds") ?: 0L
        )
    }
}
