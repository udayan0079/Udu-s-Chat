package com.uduschat.app.core

/**
 * Metadata for country dialing codes and national length validation.
 */
data class CountryCode(
    val name: String,
    val iso: String,
    val dialCode: String,
    val flag: String,
    val exampleLocal: String,
    val minNationalDigits: Int,
    val maxNationalDigits: Int,
    val stripLeadingZero: Boolean = true
) {
    val dialDigits: String get() = dialCode.removePrefix("+")
}

object PhoneNumberUtils {

    private val E164_REGEX = Regex("^\\+[1-9]\\d{6,14}$")

    val supportedCountries: List<CountryCode> = listOf(
        CountryCode("Bangladesh", "BD", "+880", "\uD83C\uDDE7\uD83C\uDDE9", "01712-345678", 10, 10, true),
        CountryCode("India", "IN", "+91", "\uD83C\uDDEE\uD83C\uDDF3", "98765-43210", 10, 10, true),
        CountryCode("United States", "US", "+1", "\uD83C\uDDFA\uD83C\uDDF8", "(555) 123-4567", 10, 10, false),
        CountryCode("United Kingdom", "GB", "+44", "\uD83C\uDDEC\uD83C\uDDE7", "07911 123456", 9, 10, true),
        CountryCode("Pakistan", "PK", "+92", "\uD83C\uDDF5\uD83C\uDDF0", "0300 1234567", 10, 10, true),
        CountryCode("Saudi Arabia", "SA", "+966", "\uD83C\uDDF8\uD83C\uDDE6", "050 123 4567", 9, 9, true),
        CountryCode("United Arab Emirates", "AE", "+971", "\uD83C\uDDE6\uD83C\uDDEA", "050 123 4567", 9, 9, true),
        CountryCode("Malaysia", "MY", "+60", "\uD83C\uDDF2\uD83C\uDDFE", "012-345 6789", 9, 10, true),
        CountryCode("Singapore", "SG", "+65", "\uD83C\uDDF8\uD83C\uDDEC", "8123 4567", 8, 8, false),
        CountryCode("Indonesia", "ID", "+62", "\uD83C\uDDEE\uD83C\uDDE9", "0812-3456-7890", 9, 12, true),
        CountryCode("Canada", "CA", "+1", "\uD83C\uDDE8\uD83C\uDDE6", "(416) 555-0199", 10, 10, false),
        CountryCode("Australia", "AU", "+61", "\uD83C\uDDE6\uD83C\uDDFA", "0412 345 678", 9, 9, true),
        CountryCode("Germany", "DE", "+49", "\uD83C\uDDE9\uD83C\uDDEA", "0151 23456789", 10, 11, true),
        CountryCode("France", "FR", "+33", "\uD83C\uDDEB\uD83C\uDDF7", "06 12 34 56 78", 9, 9, true),
        CountryCode("Italy", "IT", "+39", "\uD83C\uDDEE\uD83C\uDDF9", "312 345 6789", 9, 10, false),
        CountryCode("Spain", "ES", "+34", "\uD83C\uDDEA\uD83C\uDDF8", "612 34 56 78", 9, 9, false),
        CountryCode("Qatar", "QA", "+974", "\uD83C\uDDF6\uD83C\uDDE6", "3312 3456", 8, 8, false),
        CountryCode("Kuwait", "KW", "+965", "\uD83C\uDDF0\uD83C\uDDFC", "5001 2345", 8, 8, false),
        CountryCode("Oman", "OM", "+968", "\uD83C\uDDF4\uD83C\uDDF2", "9212 3456", 8, 8, false),
        CountryCode("Turkey", "TR", "+90", "\uD83C\uDDF9\uD83C\uDDF7", "0532 123 4567", 10, 10, true),
        CountryCode("Japan", "JP", "+81", "\uD83C\uDDEF\uD83C\uDDF5", "090-1234-5678", 10, 10, true),
        CountryCode("South Korea", "KR", "+82", "\uD83C\uDDF0\uD83C\uDDF7", "010-1234-5678", 9, 10, true),
        CountryCode("Nepal", "NP", "+977", "\uD83C\uDDF3\uD83C\uDDF5", "984-1234567", 10, 10, true),
        CountryCode("Sri Lanka", "LK", "+94", "\uD83C\uDDF1\uD83C\uDDF0", "071 234 5678", 9, 9, true),
        CountryCode("Egypt", "EG", "+20", "\uD83C\uDDEA\uD83C\uDDEC", "010 1234 5678", 10, 10, true),
        CountryCode("Nigeria", "NG", "+234", "\uD83C\uDDF3\uD83C\uDDEC", "0802 123 4567", 10, 10, true),
        CountryCode("Brazil", "BR", "+55", "\uD83C\uDDE7\uD83C\uDDF7", "(11) 91234-5678", 10, 11, true),
        CountryCode("Philippines", "PH", "+63", "\uD83C\uDDF5\uD83C\uDDED", "0917 123 4567", 10, 10, true),
        CountryCode("Thailand", "TH", "+66", "\uD83C\uDDF9\uD83C\uDDED", "081 234 5678", 9, 9, true),
        CountryCode("Vietnam", "VN", "+84", "\uD83C\uDDFB\uD83C\uDDF3", "091 234 56 78", 9, 10, true)
    )

    val defaultCountry: CountryCode = supportedCountries.first()

    fun isValidE164(phone: String): Boolean = E164_REGEX.matches(phone.trim())

    /**
     * Normalizes raw user or contact phone input to strict E.164 (e.g., +8801712345678).
     * Returns null when the phone number is structurally invalid.
     */
    fun normalizeToE164(
        rawInput: String,
        defaultCountry: CountryCode = PhoneNumberUtils.defaultCountry
    ): String? {
        val cleaned = rawInput.trim()
            .replace(Regex("[\\s\\-().]"), "")
        if (cleaned.isEmpty()) return null

        val internationalInput = when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("00") && cleaned.length > 4 -> "+" + cleaned.drop(2)
            else -> null
        }

        if (internationalInput != null) {
            val digitsOnly = internationalInput.drop(1)
            if (!digitsOnly.all { it.isDigit() }) return null
            val matched = findCountryByE164Digits(digitsOnly)
            if (matched != null) {
                var national = digitsOnly.removePrefix(matched.dialDigits)
                if (matched.stripLeadingZero && national.startsWith("0") &&
                    national.length - 1 in matched.minNationalDigits..matched.maxNationalDigits
                ) {
                    national = national.drop(1)
                }
                if (national.length !in matched.minNationalDigits..matched.maxNationalDigits) {
                    return null
                }
                val candidate = "+${matched.dialDigits}$national"
                return candidate.takeIf(::isValidE164)
            }
            val candidate = "+$digitsOnly"
            return candidate.takeIf(::isValidE164)
        }

        val digitsOnly = cleaned.filter { it.isDigit() }
        if (digitsOnly.isEmpty() || digitsOnly.length != cleaned.length) return null

        // If user typed country dial digits without '+', preserve them when national length fits.
        val dialDigits = defaultCountry.dialDigits
        if (digitsOnly.startsWith(dialDigits)) {
            val afterDial = digitsOnly.removePrefix(dialDigits)
            if (afterDial.length in defaultCountry.minNationalDigits..defaultCountry.maxNationalDigits) {
                val candidate = "+$dialDigits$afterDial"
                return candidate.takeIf(::isValidE164)
            }
        }

        val national = if (defaultCountry.stripLeadingZero) {
            digitsOnly.trimStart('0')
        } else {
            digitsOnly
        }
        if (national.length !in defaultCountry.minNationalDigits..defaultCountry.maxNationalDigits) {
            return null
        }
        val candidate = "+$dialDigits$national"
        return candidate.takeIf(::isValidE164)
    }

    fun findCountryByDialCode(dialCode: String): CountryCode? =
        supportedCountries.firstOrNull { it.dialCode == dialCode }

    fun findCountryByE164(e164: String): CountryCode? {
        if (!e164.startsWith("+")) return null
        return findCountryByE164Digits(e164.drop(1))
    }

    private fun findCountryByE164Digits(digits: String): CountryCode? =
        supportedCountries
            .sortedByDescending { it.dialDigits.length }
            .firstOrNull { digits.startsWith(it.dialDigits) }

    fun formatDisplayPhone(e164: String): String {
        if (!e164.startsWith("+")) return e164
        val country = findCountryByE164(e164) ?: return e164
        val national = e164.removePrefix(country.dialCode)
        return when (country.dialCode) {
            "+880" -> if (national.length == 10) {
                "${country.dialCode} ${national.substring(0, 4)}-${national.substring(4)}"
            } else {
                "${country.dialCode} $national"
            }
            "+1" -> if (national.length == 10) {
                "${country.dialCode} (${national.substring(0, 3)}) ${national.substring(3, 6)}-${national.substring(6)}"
            } else {
                "${country.dialCode} $national"
            }
            "+91", "+92" -> if (national.length == 10) {
                "${country.dialCode} ${national.substring(0, 5)}-${national.substring(5)}"
            } else {
                "${country.dialCode} $national"
            }
            else -> "${country.dialCode} ${national.chunked(4).joinToString(" ")}"
        }
    }
}
