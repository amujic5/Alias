package hr.azzi.socialgames.alias

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** Release builds: attest with Play Integrity. Needs the app's SHA-256 registered
 *  in Firebase + the Play Integrity API enabled for the Play app. */
object AppCheckSetup {
    fun install() {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
