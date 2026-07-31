package hr.azzi.socialgames.alias.Service

import android.content.Context
import android.media.MediaPlayer.*
import hr.azzi.socialgames.alias.R.raw.*

class SoundSystem(val context: Context) {

    fun playRightButton() = play(right_button)

    fun playSkipButton() = play(wrong_button)

    fun playTikTok() = play(countdown)

    fun playEnd() = play(end)

    /**
     * Every sound goes through here so the settings switch has one place to be honoured.
     * A player is created per shot and released on completion, which is how this always
     * worked — these are all short one-offs.
     */
    private fun play(resId: Int) {
        if (!GameSettings.soundEnabled(context)) return
        val mediaPlayer = create(context, resId) ?: return
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener { it.release() }
    }
}
