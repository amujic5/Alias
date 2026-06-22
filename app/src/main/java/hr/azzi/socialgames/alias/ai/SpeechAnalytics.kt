package hr.azzi.socialgames.alias.ai

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Debug analytics for AI speech: whether the user actually had usable speech for
 * a given language/deck.
 *
 * Event names:
 *   TTS voice (the gate): android_debug_speech_<lang>_<success|fail>
 *   STT recognition:      android_debug_speech_<lang>_stt_<success|fail>
 *   e.g. android_debug_speech_sr_fail, android_debug_speech_hr_stt_success
 * Params (queryable regardless of name): language, deck, type (tts|stt), result.
 */
object SpeechAnalytics {

    /** TTS voice availability, decided at the voice gate. */
    fun voiceResult(context: Context, language: AILanguage, deckId: String, success: Boolean) =
        log(context, language, deckId, type = "tts", infix = "", success = success)

    /** STT outcome: a transcript came back (success) or the language is missing (fail). */
    fun sttResult(context: Context, language: AILanguage, deckId: String, success: Boolean) =
        log(context, language, deckId, type = "stt", infix = "stt_", success = success)

    private fun log(
        context: Context, language: AILanguage, deckId: String,
        type: String, infix: String, success: Boolean,
    ) {
        val result = if (success) "success" else "fail"
        // language.code → en/fr/de/it/hr/sr.
        val event = "android_debug_speech_${language.code}_$infix$result"
        val params = Bundle().apply {
            putString("language", language.code)
            putString("deck", deckId)
            putString("type", type)
            putString("result", result)
        }
        runCatching { FirebaseAnalytics.getInstance(context).logEvent(event, params) }
    }
}
