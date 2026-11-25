package com.uduschat.app.ui.screens.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uduschat.app.core.PhoneNumberUtils
import com.uduschat.app.core.TimeFormatters
import com.uduschat.app.domain.model.ActiveCallSession
import com.uduschat.app.domain.model.CallDirection
import com.uduschat.app.domain.model.CallRecord
import com.uduschat.app.domain.model.CallStatus
import com.uduschat.app.domain.model.CallType
import com.uduschat.app.domain.model.UserProfile
import com.uduschat.app.ui.components.AvatarCircle
import com.uduschat.app.ui.theme.LocalUdusColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsTabScreen(
    callLogs: List<CallRecord>,
    contacts: List<UserProfile>,
    currentUserUid: String,
    isFirebaseConfigured: Boolean,
    onStartCall: (peerUid: String, peerName: String, peerPhone: String, peerAvatarUrl: String?, callType: CallType) -> Unit,
    onSimulateIncomingCall: () -> Unit,
    onClearCallLog: () -> Unit
) {
    val udus = LocalUdusColors.current
    var showNewCallDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Calls", fontWeight = FontWeight.Bold) },
                actions = {
                    if (!isFirebaseConfigured) {
                        IconButton(onClick = onSimulateIncomingCall) {
                            Icon(Icons.Default.PhoneCallback, contentDescription = "Test incoming call")
                        }
                    }
                    if (callLogs.isNotEmpty()) {
                        IconButton(onClick = onClearCallLog) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear call log")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewCallDialog = true },
                containerColor = udus.brandEmerald,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Call, contentDescription = "New call")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNewCallDialog = true }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(udus.brandEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Create call link", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Share a link for your udu's chat voice or video call",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Recent", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
            }

            if (callLogs.isEmpty()) {
                item {
                    Text(
                        text = "No recent calls. Tap the call button to place a voice or video call.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(callLogs, key = { it.id }) { call ->
                    val isOutgoing = call.callerId == currentUserUid || call.direction == CallDirection.OUTGOING
                    val peerUid = if (isOutgoing) call.calleeId else call.callerId
                    val peerName = if (isOutgoing) call.calleeName else call.callerName
                    val peerPhone = if (isOutgoing) call.calleePhone else call.callerPhone
                    val peerAvatar = if (isOutgoing) call.calleeAvatarUrl else call.callerAvatarUrl
                    val isMissed = call.status == CallStatus.MISSED || call.status == CallStatus.DECLINED

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartCall(peerUid, peerName, peerPhone, peerAvatar, call.callType) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarCircle(name = peerName, avatarUrl = peerAvatar, size = 48.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = peerName,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        isMissed -> Icons.AutoMirrored.Filled.CallMissed
                                        isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                                        else -> Icons.AutoMirrored.Filled.CallReceived
                                    },
                                    contentDescription = null,
                                    tint = if (isMissed) MaterialTheme.colorScheme.error else udus.brandEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                val durText = if (call.durationSeconds > 0L) " (${TimeFormatters.formatDuration(call.durationSeconds * 1000L)})" else ""
                                Text(
                                    text = "${TimeFormatters.formatRelativeShort(call.startedAt)}$durText",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onStartCall(peerUid, peerName, peerPhone, peerAvatar, call.callType) }) {
                            Icon(
                                imageVector = if (call.callType == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Call back",
                                tint = udus.brandEmerald
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(84.dp)) }
        }
    }

    if (showNewCallDialog) {
        AlertDialog(
            onDismissRequest = { showNewCallDialog = false },
            title = { Text("Select contact to call") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(contacts, key = { it.uid }) { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarCircle(name = user.displayName, avatarUrl = user.avatarUrl, size = 40.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.displayName, fontWeight = FontWeight.Medium)
                                Text(PhoneNumberUtils.formatDisplayPhone(user.phoneNumber), style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(onClick = {
                                showNewCallDialog = false
                                onStartCall(user.uid, user.displayName, user.phoneNumber, user.avatarUrl, CallType.VOICE)
                            }) {
                                Icon(Icons.Default.Call, null, tint = udus.brandEmerald)
                            }
                            IconButton(onClick = {
                                showNewCallDialog = false
                                onStartCall(user.uid, user.displayName, user.phoneNumber, user.avatarUrl, CallType.VIDEO)
                            }) {
                                Icon(Icons.Default.Videocam, null, tint = udus.brandEmerald)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewCallDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun ActiveCallOverlay(
    session: ActiveCallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onSwitchCamera: () -> Unit
) {
    var elapsedSec by remember(session.callId, session.connectedAtMs) { mutableLongStateOf(0L) }

    LaunchedEffect(session.status, session.connectedAtMs) {
        val connectedAt = session.connectedAtMs
        if (session.status == CallStatus.CONNECTED && connectedAt != null) {
            while (true) {
                elapsedSec = ((System.currentTimeMillis() - connectedAt) / 1000L).coerceAtLeast(0L)
                delay(500L)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A2421), Color(0xFF0B141A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (session.callType == CallType.VIDEO) "udu's chat Video Call" else "udu's chat Voice Call",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(24.dp))
                AvatarCircle(name = session.peerName, avatarUrl = session.peerAvatarUrl, size = 112.dp)
                Spacer(Modifier.height(18.dp))
                Text(
                    text = session.peerName,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = PhoneNumberUtils.formatDisplayPhone(session.peerPhone),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(10.dp))
                val statusLabel = when {
                    session.status == CallStatus.RINGING && session.isIncoming -> "Incoming ${session.callType.name.lowercase()} call..."
                    session.status == CallStatus.RINGING -> "Ringing..."
                    session.status == CallStatus.CONNECTED -> TimeFormatters.formatDuration(elapsedSec * 1000L)
                    else -> "Connecting..."
                }
                Text(
                    text = statusLabel,
                    color = Color(0xFF25D366),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                if (session.callType == CallType.VIDEO && session.isVideoEnabled) {
                    Spacer(Modifier.height(22.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 140.dp, height = 190.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E2D36)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Videocam, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (session.isFrontCamera) "Front Camera" else "Rear Camera",
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            if (session.isIncoming && session.status == CallStatus.RINGING) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onDecline,
                            modifier = Modifier.size(66.dp).clip(CircleShape).background(Color(0xFFE53935))
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Decline", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier.size(66.dp).clip(CircleShape).background(Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Accept", color = Color.White)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                        .background(Color(0xFF1C2B33))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlButton(
                        icon = if (session.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeUp,
                        active = session.isSpeakerOn,
                        onClick = onToggleSpeaker
                    )
                    CallControlButton(
                        icon = if (session.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        active = session.isVideoEnabled,
                        onClick = onToggleVideo
                    )
                    if (session.isVideoEnabled) {
                        CallControlButton(
                            icon = Icons.Default.Cameraswitch,
                            active = false,
                            onClick = onSwitchCamera
                        )
                    }
                    CallControlButton(
                        icon = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        active = session.isMuted,
                        onClick = onToggleMute
                    )
                    IconButton(
                        onClick = onEnd,
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE53935))
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End call", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color.White.copy(alpha = 0.14f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color(0xFF0B141A) else Color.White
        )
    }
}
