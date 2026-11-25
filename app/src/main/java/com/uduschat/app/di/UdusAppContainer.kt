package com.uduschat.app.di

import android.content.Context
import com.uduschat.app.core.CloudConfigStatus
import com.uduschat.app.core.DeviceContactsHelper
import com.uduschat.app.core.FirebaseBootstrap
import com.uduschat.app.core.VoiceNoteManager
import com.uduschat.app.data.local.UserPreferencesStore
import com.uduschat.app.data.repository.DemoUdusRepository
import com.uduschat.app.data.repository.FirebaseUdusRepository
import com.uduschat.app.domain.repository.UdusRepository

class UdusAppContainer(context: Context) {
    private val appContext = context.applicationContext

    val cloudStatus: CloudConfigStatus = FirebaseBootstrap.initialize(appContext)
    val preferencesStore = UserPreferencesStore(appContext)
    val voiceNoteManager = VoiceNoteManager(appContext)
    val deviceContactsHelper = DeviceContactsHelper(appContext)

    val repository: UdusRepository = if (cloudStatus.isFirebaseConfigured) {
        FirebaseUdusRepository(appContext)
    } else {
        DemoUdusRepository(preferencesStore)
    }
}
