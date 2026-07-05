package hr.azzi.socialgames.alias

import android.app.Application
import hr.azzi.socialgames.alias.Service.Billing

/** App entry — installs the variant-specific Firebase App Check provider. */
class AliasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { AppCheckSetup.install() }
        runCatching { Billing.start(this) }
    }
}
