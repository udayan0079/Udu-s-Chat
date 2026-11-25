package com.uduschat.app.domain.model

enum class CallType {
    VOICE,
    VIDEO
}

enum class CallDirection {
    INCOMING,
    OUTGOING
}

enum class CallStatus {
    RINGING,
    CONNECTED,
    ENDED,
    MISSED,
    DECLINED
}

data class CallRecord(
    val id: String,
    val callerId: String,
    val callerName: String,
    val callerPhone: String,
    val callerAvatarUrl: String? = null,
    val calleeId: String,
    val calleeName: String,
    val calleePhone: String,
    val calleeAvatarUrl: String? = null,
    val callType: CallType = CallType.VOICE,
    val direction: CallDirection = CallDirection.OUTGOING,
    val status: CallStatus = CallStatus.ENDED,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val durationSeconds: Long = 0L
)

data class ActiveCallSession(
    val callId: String,
    val peerUid: String,
    val peerName: String,
    val peerPhone: String,
    val peerAvatarUrl: String? = null,
    val callType: CallType,
    val isIncoming: Boolean,
    val status: CallStatus,
    val connectedAtMs: Long? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoEnabled: Boolean = false,
    val isFrontCamera: Boolean = true
)
