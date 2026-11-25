package com.uduschat.app.domain.model

enum class StatusType {
    TEXT,
    IMAGE
}

data class StatusUpdate(
    val id: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val userAvatarUrl: String? = null,
    val type: StatusType = StatusType.TEXT,
    val text: String = "",
    val backgroundHex: Long = 0xFF075E54,
    val mediaUrl: String? = null,
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24L * 60L * 60L * 1000L,
    val viewers: Map<String, Long> = emptyMap()
)

data class UserStatusGroup(
    val userId: String,
    val userName: String,
    val userPhone: String,
    val userAvatarUrl: String? = null,
    val items: List<StatusUpdate>,
    val hasUnviewed: Boolean
)

data class ChannelItem(
    val id: String,
    val name: String,
    val description: String,
    val followerCount: Int,
    val isFollowing: Boolean,
    val latestUpdate: String,
    val latestTime: Long
)
