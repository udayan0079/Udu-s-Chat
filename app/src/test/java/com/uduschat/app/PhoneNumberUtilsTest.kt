package com.uduschat.app

import com.uduschat.app.core.PhoneNumberUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberUtilsTest {

    private val bangladesh = PhoneNumberUtils.supportedCountries.first { it.iso == "BD" }
    private val us = PhoneNumberUtils.supportedCountries.first { it.iso == "US" }
    private val uk = PhoneNumberUtils.supportedCountries.first { it.iso == "GB" }

    @Test
    fun normalizesLocalBangladeshNumberWithLeadingZero() {
        val result = PhoneNumberUtils.normalizeToE164("01712-345678", bangladesh)
        assertEquals("+8801712345678", result)
    }

    @Test
    fun normalizesBangladeshNumberWhenDialDigitsIncludedWithoutPlus() {
        val result = PhoneNumberUtils.normalizeToE164("8801712345678", bangladesh)
        assertEquals("+8801712345678", result)
    }

    @Test
    fun normalizesDoubleZeroInternationalFormat() {
        val result = PhoneNumberUtils.normalizeToE164("0044 7911 123456", bangladesh)
        assertEquals("+447911123456", result)
    }

    @Test
    fun normalizesFormattedUsNumber() {
        val result = PhoneNumberUtils.normalizeToE164("(555) 123-4567", us)
        assertEquals("+15551234567", result)
    }

    @Test
    fun normalizesUkWithZeroAfterDialCode() {
        val result = PhoneNumberUtils.normalizeToE164("+4407911123456", uk)
        assertEquals("+447911123456", result)
    }

    @Test
    fun rejectsShortOrAlphabeticInputs() {
        assertNull(PhoneNumberUtils.normalizeToE164("1234", bangladesh))
        assertNull(PhoneNumberUtils.normalizeToE164("01712abc678", bangladesh))
        assertFalse(PhoneNumberUtils.isValidE164("01712345678"))
        assertTrue(PhoneNumberUtils.isValidE164("+8801712345678"))
    }

    @Test
    fun formatsKnownE164DisplayStrings() {
        assertEquals("+880 1712-345678", PhoneNumberUtils.formatDisplayPhone("+8801712345678"))
        assertEquals("+1 (555) 123-4567", PhoneNumberUtils.formatDisplayPhone("+15551234567"))
    }
}
