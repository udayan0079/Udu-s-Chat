package com.uduschat.app.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeFormatters {

    private fun timeFormat(): SimpleDateFormat =
        SimpleDateFormat("h:mm a", Locale.getDefault())

    private fun dayOfWeekFormat(): SimpleDateFormat =
        SimpleDateFormat("EEE", Locale.getDefault())

    private fun dateFormat(): SimpleDateFormat =
        SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    private fun fullDateHeaderFormat(): SimpleDateFormat =
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    fun formatChatListTime(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        if (timestampMs <= 0L) return ""
        return when {
            isSameDay(timestampMs, nowMs) -> timeFormat().format(Date(timestampMs))
            isYesterday(timestampMs, nowMs) -> "Yesterday"
            nowMs - timestampMs < 6L * 24L * 60L * 60L * 1000L -> dayOfWeekFormat().format(Date(timestampMs))
            else -> dateFormat().format(Date(timestampMs))
        }
    }

    fun formatMessageTime(timestampMs: Long): String {
        if (timestampMs <= 0L) return ""
        return timeFormat().format(Date(timestampMs))
    }

    fun formatDateDivider(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        return when {
            isSameDay(timestampMs, nowMs) -> "Today"
            isYesterday(timestampMs, nowMs) -> "Yesterday"
            else -> fullDateHeaderFormat().format(Date(timestampMs))
        }
    }

    fun formatLastSeen(
        isOnline: Boolean,
        lastSeenMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): String {
        if (isOnline) return "online"
        if (lastSeenMs <= 0L) return "offline"
        return when {
            isSameDay(lastSeenMs, nowMs) -> "last seen today at ${timeFormat().format(Date(lastSeenMs))}"
            isYesterday(lastSeenMs, nowMs) -> "last seen yesterday at ${timeFormat().format(Date(lastSeenMs))}"
            else -> "last seen ${dateFormat().format(Date(lastSeenMs))}"
        }
    }

    fun formatRelativeShort(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diffSec = ((nowMs - timestampMs).coerceAtLeast(0L)) / 1000L
        return when {
            diffSec < 60L -> "Just now"
            diffSec < 3600L -> "${diffSec / 60L}m ago"
            isSameDay(timestampMs, nowMs) -> "Today, ${timeFormat().format(Date(timestampMs))}"
            isYesterday(timestampMs, nowMs) -> "Yesterday, ${timeFormat().format(Date(timestampMs))}"
            else -> dateFormat().format(Date(timestampMs))
        }
    }

    fun formatDuration(durationMs: Long): String {
        val totalSec = (durationMs.coerceAtLeast(0L) / 1000L)
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
            else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun isSameDay(aMs: Long, bMs: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = aMs }
        val calB = Calendar.getInstance().apply { timeInMillis = bMs }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(targetMs: Long, nowMs: Long): Boolean {
        val calNow = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val calTarget = Calendar.getInstance().apply { timeInMillis = targetMs }
        return calTarget.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            calTarget.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
    }
}
