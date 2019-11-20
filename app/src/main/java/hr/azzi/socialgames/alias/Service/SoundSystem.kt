package hr.azzi.socialgames.alias.Service

import android.content.Context
import android.media.MediaPlayer.*
import hr.azzi.socialgames.alias.R.raw.*

class SoundSystem(val context: Context) {

    fun playRightButton() {
        val mediaPlayer = create(context, right_button)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }

    fun playSkipButton() {
        val mediaPlayer = create(context, wrong_button)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }

    fun playTikTok() {
        val mediaPlayer = create(context, countdown)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }

    fun playEnd() {
        val mediaPlayer = create(context, end)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }

}