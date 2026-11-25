package com.uduschat.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.uduschat.app.domain.model.AppPreferences
import com.uduschat.app.domain.model.FontScaleOption
import com.uduschat.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.udusDataStore by preferencesDataStore(name = "udus_chat_prefs")

class UserPreferencesStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WALLPAPER_ID = stringPreferencesKey("wallpaper_id")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val ENTER_IS_SEND = booleanPreferencesKey("enter_is_send")
        val HIGH_PRIORITY_NOTIF = booleanPreferencesKey("high_priority_notif")
        val VIBRATE_ON_MESSAGE = booleanPreferencesKey("vibrate_on_message")
        val AUTO_DL_WIFI = booleanPreferencesKey("auto_dl_wifi")
        val AUTO_DL_CELLULAR = booleanPreferencesKey("auto_dl_cellular")
        val SECURITY_NOTIFS = booleanPreferencesKey("security_notifs")
        val TWO_STEP_PIN = stringPreferencesKey("two_step_pin")

        val DEMO_PHONE = stringPreferencesKey("demo_phone")
        val DEMO_NAME = stringPreferencesKey("demo_name")
        val DEMO_ABOUT = stringPreferencesKey("demo_about")
        val DEMO_AVATAR = stringPreferencesKey("demo_avatar")
    }

    val preferencesFlow: Flow<AppPreferences> = context.udusDataStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            wallpaperId = prefs[Keys.WALLPAPER_ID] ?: "default",
            fontScale = prefs[Keys.FONT_SCALE]
                ?.let { runCatching { FontScaleOption.valueOf(it) }.getOrNull() }
                ?: FontScaleOption.MEDIUM,
            enterIsSend = prefs[Keys.ENTER_IS_SEND] ?: false,
            highPriorityNotifications = prefs[Keys.HIGH_PRIORITY_NOTIF] ?: true,
            vibrateOnMessage = prefs[Keys.VIBRATE_ON_MESSAGE] ?: true,
            mediaAutoDownloadWifi = prefs[Keys.AUTO_DL_WIFI] ?: true,
            mediaAutoDownloadCellular = prefs[Keys.AUTO_DL_CELLULAR] ?: false,
            securityNotifications = prefs[Keys.SECURITY_NOTIFS] ?: true,
            twoStepPin = prefs[Keys.TWO_STEP_PIN]?.takeIf { it.isNotBlank() }
        )
    }

    val demoSessionFlow: Flow<Triple<String, String, String>?> = context.udusDataStore.data.map { prefs ->
        val phone = prefs[Keys.DEMO_PHONE]
        val name = prefs[Keys.DEMO_NAME]
        val about = prefs[Keys.DEMO_ABOUT] ?: "Hey there! I am using udu's chat."
        if (!phone.isNullOrBlank() && !name.isNullOrBlank()) {
            Triple(phone, name, about)
        } else {
            null
        }
    }

    suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        context.udusDataStore.edit { prefs ->
            val current = AppPreferences(
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                wallpaperId = prefs[Keys.WALLPAPER_ID] ?: "default",
                fontScale = prefs[Keys.FONT_SCALE]
                    ?.let { runCatching { FontScaleOption.valueOf(it) }.getOrNull() }
                    ?: FontScaleOption.MEDIUM,
                enterIsSend = prefs[Keys.ENTER_IS_SEND] ?: false,
                highPriorityNotifications = prefs[Keys.HIGH_PRIORITY_NOTIF] ?: true,
                vibrateOnMessage = prefs[Keys.VIBRATE_ON_MESSAGE] ?: true,
                mediaAutoDownloadWifi = prefs[Keys.AUTO_DL_WIFI] ?: true,
                mediaAutoDownloadCellular = prefs[Keys.AUTO_DL_CELLULAR] ?: false,
                securityNotifications = prefs[Keys.SECURITY_NOTIFS] ?: true,
                twoStepPin = prefs[Keys.TWO_STEP_PIN]?.takeIf { it.isNotBlank() }
            )
            val updated = transform(current)
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.WALLPAPER_ID] = updated.wallpaperId
            prefs[Keys.FONT_SCALE] = updated.fontScale.name
            prefs[Keys.ENTER_IS_SEND] = updated.enterIsSend
            prefs[Keys.HIGH_PRIORITY_NOTIF] = updated.highPriorityNotifications
            prefs[Keys.VIBRATE_ON_MESSAGE] = updated.vibrateOnMessage
            prefs[Keys.AUTO_DL_WIFI] = updated.mediaAutoDownloadWifi
            prefs[Keys.AUTO_DL_CELLULAR] = updated.mediaAutoDownloadCellular
            prefs[Keys.SECURITY_NOTIFS] = updated.securityNotifications
            if (updated.twoStepPin.isNullOrBlank()) {
                prefs.remove(Keys.TWO_STEP_PIN)
            } else {
                prefs[Keys.TWO_STEP_PIN] = updated.twoStepPin
            }
        }
    }

    suspend fun saveDemoSession(phone: String, name: String, about: String, avatarUri: String?) {
        context.udusDataStore.edit { prefs ->
            prefs[Keys.DEMO_PHONE] = phone
            prefs[Keys.DEMO_NAME] = name
            prefs[Keys.DEMO_ABOUT] = about
            if (avatarUri.isNullOrBlank()) prefs.remove(Keys.DEMO_AVATAR) else prefs[Keys.DEMO_AVATAR] = avatarUri
        }
    }

    suspend fun clearDemoSession() {
        context.udusDataStore.edit { prefs ->
            prefs.remove(Keys.DEMO_PHONE)
            prefs.remove(Keys.DEMO_NAME)
            prefs.remove(Keys.DEMO_ABOUT)
            prefs.remove(Keys.DEMO_AVATAR)
        }
    }
}
