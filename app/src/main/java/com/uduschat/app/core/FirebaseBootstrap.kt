package com.uduschat.app.core

import android.content.Context
import com.google.firebase.FirebaseApp

data class CloudConfigStatus(
    val isFirebaseConfigured: Boolean,
    val projectId: String?,
    val appId: String?
) {
    val modeLabel: String
        get() = if (isFirebaseConfigured) {
            "Global Cloud (Firebase)"
        } else {
            "Local Demo Mode"
        }
}

/**
 * Safely checks whether real Firebase credentials (google-services.json) are compiled
 * into the app before accessing Firebase singletons.
 */
object FirebaseBootstrap {

    fun initialize(context: Context): CloudConfigStatus {
        val appContext = context.applicationContext
        val googleAppIdRes = appContext.resources.getIdentifier(
            "google_app_id",
            "string",
            appContext.packageName
        )
        if (googleAppIdRes == 0) {
            return CloudConfigStatus(
                isFirebaseConfigured = false,
                projectId = null,
                appId = null
            )
        }

        val appId = runCatching { appContext.getString(googleAppIdRes) }.getOrNull().orEmpty()
        if (appId.isBlank() || appId.contains("000000000000")) {
            return CloudConfigStatus(
                isFirebaseConfigured = false,
                projectId = null,
                appId = null
            )
        }

        val firebaseApp = runCatching {
            FirebaseApp.getApps(appContext).firstOrNull()
                ?: FirebaseApp.initializeApp(appContext)
        }.getOrNull()

        return if (firebaseApp != null) {
            CloudConfigStatus(
                isFirebaseConfigured = true,
                projectId = firebaseApp.options.projectId,
                appId = firebaseApp.options.applicationId
            )
        } else {
            CloudConfigStatus(
                isFirebaseConfigured = false,
                projectId = null,
                appId = null
            )
        }
    }
}
