package hr.azzi.socialgames.alias

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** Debug builds: use the Debug provider. Register the printed debug token in
 *  Firebase console → App Check → Manage debug tokens. */
object AppCheckSetup {
    fun install() {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
