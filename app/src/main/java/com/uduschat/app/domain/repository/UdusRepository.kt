package com.uduschat.app.domain.repository

import android.app.Activity
import com.uduschat.app.core.DeviceContact
import com.uduschat.app.domain.model.ActiveCallSession
import com.uduschat.app.domain.model.AuthState
import com.uduschat.app.domain.model.CallRecord
import com.uduschat.app.domain.model.CallType
import com.uduschat.app.domain.model.ChannelItem
import com.uduschat.app.domain.model.ChatMessage
import com.uduschat.app.domain.model.ChatThread
import com.uduschat.app.domain.model.MessageDraftAttachment
import com.uduschat.app.domain.model.PrivacySettings
import com.uduschat.app.domain.model.StatusUpdate
import com.uduschat.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UdusRepository {
    val authState: StateFlow<AuthState>
    val currentUser: StateFlow<UserProfile?>
    val contacts: StateFlow<List<UserProfile>>
    val chats: StateFlow<List<ChatThread>>
    val statuses: StateFlow<List<StatusUpdate>>
    val channels: StateFlow<List<ChannelItem>>
    val callLogs: StateFlow<List<CallRecord>>
    val activeCall: StateFlow<ActiveCallSession?>
    val starredMessages: StateFlow<List<ChatMessage>>

    fun observeMessages(chatId: String): Flow<List<ChatMessage>>
    fun observeChat(chatId: String): Flow<ChatThread?>

    // Auth & Profile
    fun proceedFromWelcome()
    fun clearAuthError()
    suspend fun sendOtp(phoneE164: String, activity: Activity?)
    suspend fun resendOtp(activity: Activity?)
    suspend fun verifyOtp(code: String)
    fun backToPhoneInput()
    suspend fun completeProfileSetup(displayName: String, about: String, avatarUri: String?)
    suspend fun updateProfile(displayName: String, about: String, avatarUri: String?)
    suspend fun updatePrivacy(transform: (PrivacySettings) -> PrivacySettings)
    suspend fun setOnlinePresence(isOnline: Boolean)
    suspend fun signOut()
    suspend fun deleteAccount()

    // Discovery & Creation
    suspend fun findUserByPhone(phoneE164: String): Result<UserProfile?>
    suspend fun syncDeviceContacts(deviceContacts: List<DeviceContact>): Result<List<UserProfile>>
    suspend fun startOrOpenDirectChat(user: UserProfile): Result<String>
    suspend fun createGroupChat(
        title: String,
        description: String,
        memberUids: List<String>,
        avatarUri: String? = null
    ): Result<String>
    suspend fun sendBroadcastMessage(recipientUids: List<String>, text: String): Result<Int>

    // Messaging
    suspend fun sendMessage(
        chatId: String,
        text: String,
        attachment: MessageDraftAttachment? = null,
        replyTo: ChatMessage? = null
    )
    suspend fun markChatAsRead(chatId: String)
    suspend fun setTyping(chatId: String, isTyping: Boolean)
    suspend fun saveDraft(chatId: String, draft: String)
    suspend fun toggleReaction(chatId: String, messageId: String, emoji: String)
    suspend fun toggleStarMessage(chatId: String, messageId: String)
    suspend fun editMessage(chatId: String, messageId: String, newText: String)
    suspend fun deleteMessage(chatId: String, messageId: String, forEveryone: Boolean)
    suspend fun forwardMessages(targetChatIds: List<String>, messages: List<ChatMessage>)
    suspend fun markViewOnceOpened(chatId: String, messageId: String)

    // Thread Actions
    suspend fun togglePinChat(chatId: String)
    suspend fun toggleMuteChat(chatId: String)
    suspend fun toggleArchiveChat(chatId: String)
    suspend fun toggleFavoriteChat(chatId: String)
    suspend fun markChatUnread(chatId: String)
    suspend fun clearChatMessages(chatId: String)
    suspend fun deleteChat(chatId: String)
    suspend fun setDisappearingTimer(chatId: String, seconds: Long)
    suspend fun setChatWallpaper(chatId: String, wallpaperId: String?)
    suspend fun blockUser(uid: String, block: Boolean)
    suspend fun updateGroupInfo(chatId: String, title: String, description: String)
    suspend fun addGroupMembers(chatId: String, newMemberUids: List<String>)
    suspend fun removeGroupMember(chatId: String, memberUid: String)
    suspend fun toggleGroupAdmin(chatId: String, memberUid: String)
    suspend fun leaveGroup(chatId: String)

    // Updates / Status / Channels
    suspend fun postTextStatus(text: String, backgroundHex: Long)
    suspend fun postImageStatus(mediaUri: String, caption: String)
    suspend fun markStatusViewed(statusId: String)
    suspend fun deleteStatus(statusId: String)
    suspend fun replyToStatus(status: StatusUpdate, replyText: String): Result<String>
    suspend fun toggleChannelFollow(channelId: String)

    // Calls
    suspend fun startCall(
        peerUid: String,
        peerName: String,
        peerPhone: String,
        peerAvatarUrl: String?,
        callType: CallType
    )
    suspend fun acceptIncomingCall()
    suspend fun declineIncomingCall()
    suspend fun endActiveCall()
    fun toggleCallMute()
    fun toggleCallSpeaker()
    fun toggleCallVideo()
    fun switchCallCamera()
    suspend fun simulateIncomingCall()
    suspend fun clearCallLog()
}
