package hr.azzi.socialgames.alias

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import hr.azzi.socialgames.alias.Adapters.VideoAdapter
import hr.azzi.socialgames.alias.Adapters.VideoAdapterDelegate
import hr.azzi.socialgames.alias.Models.Video
import hr.azzi.socialgames.alias.Service.DictionaryService
import hr.azzi.socialgames.alias.Service.RecordingFlag
import kotlinx.android.synthetic.main.activity_video_list.*

class VideoListActivity : AppCompatActivity(), VideoAdapterDelegate {


    lateinit var videos: ArrayList<Video>
    lateinit var videoAdapter: VideoAdapter
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.setCurrentScreen(this, "Video list", null /* class override */)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        val fileURIStrings = intent.getStringArrayListExtra("fileURIStrings")
        val videoList = fileURIStrings?.mapIndexed { index, value ->
            val uri = Uri.parse(value)
            Video("Video no. $index" , uri, value)
        }
        videos = ArrayList(videoList)
        videoAdapter = VideoAdapter(this, videos)
        listView.adapter = videoAdapter
        videoAdapter.delegate = this

        backButton.setOnClickListener {
            finish()
        }

    }

    fun logShare() {
        if (RecordingFlag.recordingEnabled) {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Video list")
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Video list")
            firebaseAnalytics.logEvent("share_video", bundle)

            // by example
            val bundle2 = Bundle()
            bundle2.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "video")
            bundle2.putString(FirebaseAnalytics.Param.ITEM_ID, "video")
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle2)
        }
    }

    fun logPlay() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Video list")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Video list")
        firebaseAnalytics.logEvent("play_video", bundle)
    }


    fun createInstagramIntent(type: String, uri: Uri) {
        val share = Intent(Intent.ACTION_SEND);

        share.type = type
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        share.putExtra(Intent.EXTRA_STREAM, uri)

        // Broadcast the Intent.
        startActivity(Intent.createChooser(share, "Share to"))
    }

    fun shareVideo(uri: Uri) {
        logShare()
        createInstagramIntent("video/*", uri)
    }

    // VideoAdapterDelegate
    override fun didTapShare(position: Int) {
        val video = videos[position]
        shareVideo(video.uri)

    }
    override fun didTapPlay(position: Int) {
        logPlay()
        val intent =  Intent(this, VideoActivity::class.java)
        val fileURIString = videos[position].uriString
        intent.putExtra("fileURIString", fileURIString)
        startActivity(intent)
    }

}
