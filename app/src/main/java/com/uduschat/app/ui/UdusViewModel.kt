package com.uduschat.app.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uduschat.app.core.CountryCode
import com.uduschat.app.core.PhoneNumberUtils
import com.uduschat.app.core.RecordedVoiceNote
import com.uduschat.app.core.VoicePlaybackState
import com.uduschat.app.di.UdusAppContainer
import com.uduschat.app.domain.model.ActiveCallSession
import com.uduschat.app.domain.model.AppPreferences
import com.uduschat.app.domain.model.AuthState
import com.uduschat.app.domain.model.CallRecord
import com.uduschat.app.domain.model.CallType
import com.uduschat.app.domain.model.ChannelItem
import com.uduschat.app.domain.model.ChatMessage
import com.uduschat.app.domain.model.ChatThread
import com.uduschat.app.domain.model.MessageDraftAttachment
import com.uduschat.app.domain.model.MessageType
import com.uduschat.app.domain.model.PrivacySettings
import com.uduschat.app.domain.model.StatusUpdate
import com.uduschat.app.domain.model.UserProfile
import com.uduschat.app.domain.repository.UdusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UdusViewModel(
    private val container: UdusAppContainer
) : ViewModel() {

    val repository: UdusRepository = container.repository
    val cloudStatus = container.cloudStatus

    val authState: StateFlow<AuthState> = repository.authState
    val currentUser: StateFlow<UserProfile?> = repository.currentUser
    val contacts: StateFlow<List<UserProfile>> = repository.contacts
    val chats: StateFlow<List<ChatThread>> = repository.chats
    val statuses: StateFlow<List<StatusUpdate>> = repository.statuses
    val channels: StateFlow<List<ChannelItem>> = repository.channels
    val callLogs: StateFlow<List<CallRecord>> = repository.callLogs
    val activeCall: StateFlow<ActiveCallSession?> = repository.activeCall
    val starredMessages: StateFlow<List<ChatMessage>> = repository.starredMessages

    val preferences: StateFlow<AppPreferences> = container.preferencesStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())

    val isRecordingVoice: StateFlow<Boolean> = container.voiceNoteManager.isRecording
    val recordingDurationMs: StateFlow<Long> = container.voiceNoteManager.recordingDurationMs
    val voicePlaybackState: StateFlow<VoicePlaybackState> = container.voiceNoteManager.playbackState

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = repository.observeMessages(chatId)
    fun observeChat(chatId: String): Flow<ChatThread?> = repository.observeChat(chatId)

    // Auth
    fun proceedFromWelcome() = repository.proceedFromWelcome()
    fun clearAuthError() = repository.clearAuthError()
    fun backToPhoneInput() = repository.backToPhoneInput()

    fun sendOtp(rawPhone: String, country: CountryCode, activity: Activity?) {
        val e164 = PhoneNumberUtils.normalizeToE164(rawPhone, country)
        viewModelScope.launch {
            if (e164 == null) {
                repository.sendOtp(rawPhone, activity)
            } else {
                repository.sendOtp(e164, activity)
            }
        }
    }

    fun resendOtp(activity: Activity?) {
        viewModelScope.launch { repository.resendOtp(activity) }
    }

    fun verifyOtp(code: String) {
        viewModelScope.launch { repository.verifyOtp(code) }
    }

    fun completeProfileSetup(name: String, about: String, avatarUri: String?) {
        viewModelScope.launch { repository.completeProfileSetup(name, about, avatarUri) }
    }

    fun updateProfile(name: String, about: String, avatarUri: String?) {
        viewModelScope.launch { repository.updateProfile(name, about, avatarUri) }
    }

    fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        viewModelScope.launch { container.preferencesStore.updatePreferences(transform) }
    }

    fun updatePrivacy(transform: (PrivacySettings) -> PrivacySettings) {
        viewModelScope.launch { repository.updatePrivacy(transform) }
    }

    fun setOnlinePresence(online: Boolean) {
        viewModelScope.launch { repository.setOnlinePresence(online) }
    }

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    fun deleteAccount() {
        viewModelScope.launch { repository.deleteAccount() }
    }

    // Contacts & Chat creation
    suspend fun findAndOpenChatByPhone(
        rawPhone: String,
        country: CountryCode
    ): Result<String> {
        val e164 = PhoneNumberUtils.normalizeToE164(rawPhone, country)
            ?: return Result.failure(IllegalArgumentException("Please enter a valid international phone number."))
        if (e164 == currentUser.value?.phoneNumber) {
            return Result.failure(IllegalArgumentException("That is your own phone number."))
        }
        val user = repository.findUserByPhone(e164).getOrElse { return Result.failure(it) }
            ?: return Result.failure(
                IllegalStateException("No udu's chat user found for ${PhoneNumberUtils.formatDisplayPhone(e164)}. Invite them to join!")
            )
        return repository.startOrOpenDirectChat(user)
    }

    suspend fun openDirectChat(user: UserProfile): Result<String> =
        repository.startOrOpenDirectChat(user)

    fun syncDeviceContacts(defaultCountry: CountryCode, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val raw = container.deviceContactsHelper.loadContacts(defaultCountry)
            val synced = repository.syncDeviceContacts(raw).getOrDefault(emptyList())
            onDone(synced.size)
        }
    }

    suspend fun createGroup(
        title: String,
        description: String,
        memberUids: List<String>
    ): Result<String> = repository.createGroupChat(title, description, memberUids)

    suspend fun sendBroadcast(recipientUids: List<String>, text: String): Result<Int> =
        repository.sendBroadcastMessage(recipientUids, text)

    // Messaging & Voice Notes
    fun sendMessage(
        chatId: String,
        text: String,
        attachment: MessageDraftAttachment? = null,
        replyTo: ChatMessage? = null
    ) {
        viewModelScope.launch {
            repository.sendMessage(chatId, text, attachment, replyTo)
        }
    }

    fun startVoiceRecording(): Boolean = container.voiceNoteManager.startRecording()

    fun stopAndSendVoiceNote(chatId: String, replyTo: ChatMessage? = null) {
        val recorded: RecordedVoiceNote? = container.voiceNoteManager.stopRecording()
        if (recorded != null) {
            sendMessage(
                chatId = chatId,
                text = "",
                attachment = MessageDraftAttachment(
                    type = MessageType.AUDIO,
                    uriOrPath = recorded.filePath,
                    fileName = "Voice note",
                    mimeType = "audio/mp4",
                    durationMs = recorded.durationMs
                ),
                replyTo = replyTo
            )
        }
    }

    fun cancelVoiceRecording() = container.voiceNoteManager.cancelRecording()

    fun toggleVoicePlayback(message: ChatMessage) {
        val source = message.mediaLocalPath ?: message.mediaUrl
        container.voiceNoteManager.togglePlayback(
            messageId = message.id,
            source = source,
            fallbackDurationMs = message.mediaDurationMs
        )
    }

    fun cycleVoiceSpeed() = container.voiceNoteManager.cycleSpeed()

    fun markChatAsRead(chatId: String) {
        viewModelScope.launch { repository.markChatAsRead(chatId) }
    }

    fun saveDraft(chatId: String, draft: String) {
        viewModelScope.launch { repository.saveDraft(chatId, draft) }
    }

    fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        viewModelScope.launch { repository.toggleReaction(chatId, messageId, emoji) }
    }

    fun toggleStarMessage(chatId: String, messageId: String) {
        viewModelScope.launch { repository.toggleStarMessage(chatId, messageId) }
    }

    fun editMessage(chatId: String, messageId: String, newText: String) {
        viewModelScope.launch { repository.editMessage(chatId, messageId, newText) }
    }

    fun deleteMessage(chatId: String, messageId: String, forEveryone: Boolean) {
        viewModelScope.launch { repository.deleteMessage(chatId, messageId, forEveryone) }
    }

    fun forwardMessages(targetChatIds: List<String>, messages: List<ChatMessage>) {
        viewModelScope.launch { repository.forwardMessages(targetChatIds, messages) }
    }

    fun markViewOnceOpened(chatId: String, messageId: String) {
        viewModelScope.launch { repository.markViewOnceOpened(chatId, messageId) }
    }

    fun togglePinChat(chatId: String) = viewModelScope.launch { repository.togglePinChat(chatId) }
    fun toggleMuteChat(chatId: String) = viewModelScope.launch { repository.toggleMuteChat(chatId) }
    fun toggleArchiveChat(chatId: String) = viewModelScope.launch { repository.toggleArchiveChat(chatId) }
    fun toggleFavoriteChat(chatId: String) = viewModelScope.launch { repository.toggleFavoriteChat(chatId) }
    fun markChatUnread(chatId: String) = viewModelScope.launch { repository.markChatUnread(chatId) }
    fun clearChat(chatId: String) = viewModelScope.launch { repository.clearChatMessages(chatId) }
    fun deleteChat(chatId: String) = viewModelScope.launch { repository.deleteChat(chatId) }
    fun setDisappearingTimer(chatId: String, seconds: Long) = viewModelScope.launch { repository.setDisappearingTimer(chatId, seconds) }
    fun setChatWallpaper(chatId: String, wallpaperId: String?) = viewModelScope.launch { repository.setChatWallpaper(chatId, wallpaperId) }
    fun blockUser(uid: String, block: Boolean) = viewModelScope.launch { repository.blockUser(uid, block) }
    fun updateGroupInfo(chatId: String, title: String, desc: String) = viewModelScope.launch { repository.updateGroupInfo(chatId, title, desc) }
    fun addGroupMembers(chatId: String, uids: List<String>) = viewModelScope.launch { repository.addGroupMembers(chatId, uids) }
    fun removeGroupMember(chatId: String, uid: String) = viewModelScope.launch { repository.removeGroupMember(chatId, uid) }
    fun toggleGroupAdmin(chatId: String, uid: String) = viewModelScope.launch { repository.toggleGroupAdmin(chatId, uid) }
    fun leaveGroup(chatId: String) = viewModelScope.launch { repository.leaveGroup(chatId) }

    // Status & Channels
    fun postTextStatus(text: String, bgHex: Long) = viewModelScope.launch { repository.postTextStatus(text, bgHex) }
    fun postImageStatus(uri: String, caption: String) = viewModelScope.launch { repository.postImageStatus(uri, caption) }
    fun markStatusViewed(statusId: String) = viewModelScope.launch { repository.markStatusViewed(statusId) }
    fun deleteStatus(statusId: String) = viewModelScope.launch { repository.deleteStatus(statusId) }
    suspend fun replyToStatus(status: StatusUpdate, replyText: String): Result<String> = repository.replyToStatus(status, replyText)
    fun toggleChannelFollow(channelId: String) = viewModelScope.launch { repository.toggleChannelFollow(channelId) }

    // Calls
    fun startCall(peerUid: String, peerName: String, peerPhone: String, peerAvatarUrl: String?, callType: CallType) {
        viewModelScope.launch { repository.startCall(peerUid, peerName, peerPhone, peerAvatarUrl, callType) }
    }

    fun acceptIncomingCall() = viewModelScope.launch { repository.acceptIncomingCall() }
    fun declineIncomingCall() = viewModelScope.launch { repository.declineIncomingCall() }
    fun endActiveCall() = viewModelScope.launch { repository.endActiveCall() }
    fun toggleCallMute() = repository.toggleCallMute()
    fun toggleCallSpeaker() = repository.toggleCallSpeaker()
    fun toggleCallVideo() = repository.toggleCallVideo()
    fun switchCallCamera() = repository.switchCallCamera()
    fun simulateIncomingCall() = viewModelScope.launch { repository.simulateIncomingCall() }
    fun clearCallLog() = viewModelScope.launch { repository.clearCallLog() }

    override fun onCleared() {
        super.onCleared()
        container.voiceNoteManager.release()
    }

    class Factory(private val container: UdusAppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UdusViewModel(container) as T
        }
    }
}
