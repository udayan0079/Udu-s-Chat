package com.uduschat.app.domain.model

enum class VisibilityOption(val label: String) {
    EVERYONE("Everyone"),
    CONTACTS("My contacts"),
    NOBODY("Nobody")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class FontScaleOption(val label: String, val scale: Float) {
    SMALL("Small", 0.9f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.15f)
}

data class PrivacySettings(
    val lastSeenVisibility: VisibilityOption = VisibilityOption.EVERYONE,
    val profilePhotoVisibility: VisibilityOption = VisibilityOption.EVERYONE,
    val aboutVisibility: VisibilityOption = VisibilityOption.EVERYONE,
    val readReceiptsEnabled: Boolean = true,
    val defaultDisappearingSeconds: Long = 0L,
    val blockedUids: List<String> = emptyList()
)

data class UserProfile(
    val uid: String = "",
    val phoneNumber: String = "",
    val displayName: String = "",
    val about: String = "Hey there! I am using udu's chat.",
    val avatarUrl: String? = null,
    val isOnline: Boolean = true,
    val lastSeenAt: Long = System.currentTimeMillis(),
    val fcmToken: String? = null,
    val privacy: PrivacySettings = PrivacySettings()
)

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val wallpaperId: String = "default",
    val fontScale: FontScaleOption = FontScaleOption.MEDIUM,
    val enterIsSend: Boolean = false,
    val highPriorityNotifications: Boolean = true,
    val vibrateOnMessage: Boolean = true,
    val mediaAutoDownloadWifi: Boolean = true,
    val mediaAutoDownloadCellular: Boolean = false,
    val securityNotifications: Boolean = true,
    val twoStepPin: String? = null
)

enum class AuthStep {
    WELCOME,
    PHONE_INPUT,
    OTP_VERIFY,
    PROFILE_SETUP,
    AUTHENTICATED
}

data class AuthState(
    val step: AuthStep = AuthStep.WELCOME,
    val pendingPhoneE164: String = "",
    val verificationId: String? = null,
    val demoOtpHint: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDemoMode: Boolean = false
)
