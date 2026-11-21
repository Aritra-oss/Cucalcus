
package com.example.platform

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

/**
 * Returns only the default Firebase app.
 * Named Firebase apps must never be mistaken for the default app.
 */
private fun getDefaultFirebaseApp(context: Context): FirebaseApp? {
    return FirebaseApp.getApps(context.applicationContext)
        .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
}

/**
 * Ensures that the default Firebase app is initialized.
 */
fun ensureFirebaseInitialized(context: Context): FirebaseApp? {
    val appCtx = context.applicationContext

    // 1. Check whether the default Firebase app already exists.
    getDefaultFirebaseApp(appCtx)?.let { return it }

    // 2. Try standard google-services.json initialization.
    runCatching {
        FirebaseApp.initializeApp(appCtx)
    }.onFailure {
        Log.w(TAG, "Standard Firebase initialization failed.", it)
    }

    getDefaultFirebaseApp(appCtx)?.let { return it }

    // 3. Search Firebase resources in possible package namespaces.
    val pkgCandidates = listOf(
        appCtx.packageName,
        context.packageName,
        appCtx.applicationInfo.packageName,
        "com.example"
    ).distinct()

    fun resolveStringResource(key: String): String? {
        for (pkg in pkgCandidates) {
            val id = appCtx.resources.getIdentifier(key, "string", pkg)
            if (id != 0) {
                val value = runCatching {
                    appCtx.getString(id)
                }.getOrNull()?.trim()

                if (!value.isNullOrEmpty()) return value
            }
        }
        return null
    }

    val googleAppId = resolveStringResource("google_app_id")
    val apiKey = resolveStringResource("google_api_key")
    val projectId = resolveStringResource("project_id")
    val gcmSenderId = resolveStringResource("gcm_defaultSenderId")
    val storageBucket = resolveStringResource("google_storage_bucket")

    if (googleAppId.isNullOrBlank() || apiKey.isNullOrBlank()) {
        Log.e(
            TAG,
            "Firebase resources missing google_app_id or google_api_key. " +
                "Checked packages: $pkgCandidates"
        )
        return null
    }

    val optionsBuilder = FirebaseOptions.Builder()
        .setApplicationId(googleAppId)
        .setApiKey(apiKey)

    if (!projectId.isNullOrBlank()) {
        optionsBuilder.setProjectId(projectId)
    }

    if (!gcmSenderId.isNullOrBlank()) {
        optionsBuilder.setGcmSenderId(gcmSenderId)
    }

    if (!storageBucket.isNullOrBlank()) {
        optionsBuilder.setStorageBucket(storageBucket)
    }

    // 4. Explicitly initialize the default Firebase app.
    return runCatching {
        FirebaseApp.initializeApp(appCtx, optionsBuilder.build())
    }.onFailure {
        Log.e(TAG, "Explicit default Firebase initialization failed.", it)
    }.getOrNull() ?: getDefaultFirebaseApp(appCtx)
}

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        val firebaseApp = ensureFirebaseInitialized(context)

        if (firebaseApp == null) {
            Log.e(
                TAG,
                "Default FirebaseApp could not be initialized. Skipping App Check."
            )
            return
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = firebaseApp.persistenceKey

                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )

                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()

                Log.d(TAG, "App Check debug secret configured.")
            } else {
                Log.w(TAG, "No App Check debug token found.")
            }

            FirebaseAppCheck.getInstance(firebaseApp)
                .installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )

            Log.d(TAG, "Debug App Check provider installed.")

        } else {
            val resId = listOf(context.packageName, "com.example")
                .distinct()
                .firstNotNullOfOrNull { pkg ->
                    context.resources
                        .getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", pkg)
                        .takeIf { it != 0 }
                } ?: 0

            if (resId != 0) {
                val siteKey = context.getString(resId)

                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance(firebaseApp)
                        .installAppCheckProviderFactory(
                            RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                        )

                    Log.d(TAG, "Recaptcha App Check provider installed.")
                } else {
                    Log.e(TAG, "reCAPTCHA site key is blank.")
                }
            } else {
                Log.e(TAG, "reCAPTCHA site key resource not found.")
            }
        }

    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check.", e)
    }
}
