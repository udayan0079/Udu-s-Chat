package com.uduschat.app

import com.uduschat.app.core.TimeFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeFormattersTest {

    @Test
    fun formatsDurationAndFileSizeAccurately() {
        assertEquals("00:05", TimeFormatters.formatDuration(5_000L))
        assertEquals("04:02", TimeFormatters.formatDuration(242_000L))
        assertEquals("512 B", TimeFormatters.formatFileSize(512L))
        assertEquals("2 KB", TimeFormatters.formatFileSize(2048L))
        assertEquals("1.5 MB", TimeFormatters.formatFileSize(1_572_864L))
    }

    @Test
    fun formatsTodayAndYesterdayDividers() {
        val now = 1_764_000_000_000L
        assertEquals("Today", TimeFormatters.formatDateDivider(now, now))
        assertEquals("Yesterday", TimeFormatters.formatDateDivider(now - 24L * 3600_000L, now))
        assertTrue(TimeFormatters.formatLastSeen(true, now, now) == "online")
    }
}
