package hr.azzi.socialgames.alias

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.MediaController
import kotlinx.android.synthetic.main.activity_video.*


class VideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        videoView.setMediaController(MediaController(this))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

    }

    override fun onResume() {
        super.onResume()
        val fileURIString = intent.getStringExtra("fileURIString")
        if (fileURIString != null) {
            val uri = Uri.parse(fileURIString)
            videoView.setVideoURI(uri)
        }
        videoView.requestFocus()
        videoView.start()
    }

}
