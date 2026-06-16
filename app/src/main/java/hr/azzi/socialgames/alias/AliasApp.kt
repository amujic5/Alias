package hr.azzi.socialgames.alias

import android.app.Application

/** App entry — installs the variant-specific Firebase App Check provider. */
class AliasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { AppCheckSetup.install() }
    }
}
