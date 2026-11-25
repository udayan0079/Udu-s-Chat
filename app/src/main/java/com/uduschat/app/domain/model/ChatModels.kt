package com.uduschat.app.domain.model

enum class ChatType {
    DIRECT,
    GROUP
}

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    LOCATION,
    CONTACT,
    SYSTEM
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

enum class ChatFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    FAVORITES("Favorites"),
    GROUPS("Groups")
}

data class ChatThread(
    val id: String,
    val type: ChatType = ChatType.DIRECT,
    val title: String,
    val description: String = "",
    val avatarUrl: String? = null,
    val participantIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val otherUserId: String? = null,
    val otherUserPhone: String? = null,
    val isOnline: Boolean = false,
    val lastSeenAt: Long = 0L,
    val typingUserIds: List<String> = emptyList(),
    val lastMessagePreview: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageType: MessageType = MessageType.TEXT,
    val lastMessageStatus: MessageStatus = MessageStatus.SENT,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val disappearingSeconds: Long = 0L,
    val draftText: String = "",
    val wallpaperId: String? = null
)

data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderPhone: String = "",
    val type: MessageType = MessageType.TEXT,
    val text: String = "",
    val mediaUrl: String? = null,
    val mediaLocalPath: String? = null,
    val mediaMimeType: String? = null,
    val mediaDurationMs: Long = 0L,
    val fileName: String? = null,
    val fileSizeBytes: Long = 0L,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationLabel: String? = null,
    val sharedContactName: String? = null,
    val sharedContactPhone: String? = null,
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToPreview: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val starredBy: List<String> = emptyList(),
    val deletedFor: List<String> = emptyList(),
    val isDeletedForEveryone: Boolean = false,
    val isEdited: Boolean = false,
    val isForwarded: Boolean = false,
    val viewOnce: Boolean = false,
    val viewedBy: List<String> = emptyList(),
    val status: MessageStatus = MessageStatus.SENT,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
) {
    fun displayPreview(): String {
        if (isDeletedForEveryone) return "This message was deleted"
        return when (type) {
            MessageType.TEXT -> text
            MessageType.IMAGE -> if (text.isNotBlank()) "\uD83D\uDCF7 $text" else "\uD83D\uDCF7 Photo"
            MessageType.VIDEO -> if (text.isNotBlank()) "\uD83C\uDFA5 $text" else "\uD83C\uDFA5 Video"
            MessageType.AUDIO -> "\uD83C\uDFA4 Voice message"
            MessageType.DOCUMENT -> "\uD83D\uDCC4 ${fileName ?: "Document"}"
            MessageType.LOCATION -> "\uD83D\uDCCD ${locationLabel ?: "Location"}"
            MessageType.CONTACT -> "\uD83D\uDC64 ${sharedContactName ?: "Contact"}"
            MessageType.SYSTEM -> text
        }
    }
}

data class MessageDraftAttachment(
    val type: MessageType,
    val uriOrPath: String? = null,
    val fileName: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val durationMs: Long = 0L,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationLabel: String? = null,
    val sharedContactName: String? = null,
    val sharedContactPhone: String? = null,
    val viewOnce: Boolean = false
)
