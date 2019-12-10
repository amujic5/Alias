package hr.azzi.socialgames.alias

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.Service.DictionaryService
import hr.azzi.socialgames.alias.Service.SoundSystem
import kotlinx.android.synthetic.main.activity_play.*
import kotlinx.android.synthetic.main.dialog_play.view.*

import java.io.File
import java.util.concurrent.Executors
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.net.Uri
import android.Manifest
import android.annotation.SuppressLint
import android.view.ViewGroup

import android.graphics.Color
import android.os.*

import android.view.MotionEvent
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.lifecycle.LifecycleOwner
import hr.azzi.socialgames.alias.Service.RecordingFlag
import java.util.concurrent.Executor

@SuppressLint("RestrictedApi, ClickableViewAccessibility")
class PlayActivity : AppCompatActivity() {

    // camera
    private lateinit var videoCapture: VideoCapture
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private val REQUEST_CODE_PERMISSIONS = 10


    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var mInterstitialAd: InterstitialAd

    var timer: CountDownTimer? = null
    var estimation = 60
    var isRunning = false

    var finishDialog: AlertDialog? = null
    var dialog: AlertDialog? = null

    val game: Game by lazy {
        intent.getParcelableExtra("game") as Game
    }

    val soundSystem: SoundSystem by lazy {
        SoundSystem(this)
    }

    var savedFile: File? = null
    var savedFiles: ArrayList<File> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        log()
        loadAd()
        loadIntrestialAd()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        estimation = game.time

        observe()
        teamNameTextView.text = game.currentTeam.teamName
        showNewWord()


        if (RecordingFlag.recordingEnabled) {
            viewFinder.alpha = 0.1F
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }

            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
        } else {
            viewFinder.visibility = View.GONE
        }

    }

    fun loadAd() {
        MobileAds.initialize(this) {}

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun loadIntrestialAd() {
        MobileAds.initialize(this) {}
        mInterstitialAd = InterstitialAd(this)
        val prod = "ca-app-pub-1489905432577426/3906492992"
        val test =  "ca-app-pub-3940256099942544/1033173712"
        mInterstitialAd.adUnitId = prod
        mInterstitialAd.loadAd(AdRequest.Builder().build())
    }

    fun log() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)


        firebaseAnalytics.setCurrentScreen(this, "PlayActivity", null /* class override */)

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "PlayActivity")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "PlayActivity")
        bundle.putString("language", DictionaryService.playingDictionary?.language)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    fun observe() {

        pauseButton.setOnClickListener {
            pause()
            showDialog()
        }

        correctButton.setOnClickListener {
            game.addMarkedWord(MarkedWord(wordTextView.text.toString(), true))
            showNewWord()
            soundSystem.playRightButton()
            updateLables()
        }

        skipButton.setOnClickListener {
            game.addMarkedWord(MarkedWord(wordTextView.text.toString(), false))
            showNewWord()
            soundSystem.playSkipButton()
            updateLables()
        }
    }

    fun showDialog() {
        this.dialog?.dismiss()
        this.finishDialog?.dismiss()

        val context = this
        val builder = AlertDialog.Builder(context)

        builder.setCancelable(false)

        val view = layoutInflater.inflate(R.layout.dialog_play, null)
        builder.setView(view)

        view.explainingTextView.text = game.explainingPlayerName
        view.answeringTextView.text = game.answeringPlayerName

        val correct = game.currentCorrectAnswers
        view.correctTextView.text = "$correct " + resources.getString(R.string.corrected)
        var skipped = game.currentSkipAnswers
        view.skipTextView.text = "$skipped " + resources.getString(R.string.skipped)
        view.teamTextView.text = game.currentTeam.teamName

        val dialog = builder.show()
        this.dialog = dialog

        view.stopButton.setOnClickListener {
            dialog.dismiss()
            showFinishDialog()
        }

        view.playButton.setOnClickListener {
            dialog.dismiss()
            resume()
        }

    }

    fun showFinishDialog() {
        this.dialog?.dismiss()
        this.finishDialog?.dismiss()

        val builder = AlertDialog.Builder(this)

        builder.setTitle(getString(R.string.warning))

        // Display a message on alert dialog
        builder.setMessage(getString(R.string.are_you_sure_quit))

        // Set a positive button and its click listener on alert dialog
        builder.setPositiveButton(getString(R.string.yes)){ dialog, which ->
            finish()
        }


        // Display a negative button on alert dialog
        builder.setNegativeButton(getString(R.string.no)){ dialog, which ->
            showDialog()
        }

        // Finally, make the alert dialog using builder
        val dialog: AlertDialog = builder.create()

        // Display the alert dialog on app interface
        dialog.show()

        this.finishDialog = dialog
    }


    override fun onPause() {
        super.onPause()
        pause()
    }

    override fun onResume() {
        super.onResume()
        showDialog()
    }

    override fun onBackPressed() {
        pause()
        showDialog()
    }

    fun updateLables() {
        val correct = game.currentCorrectAnswers
        correctTextView.text = "$correct " + resources.getString(R.string.corrected)

        var skipped = game.currentSkipAnswers
        skipTextView.text = "$skipped " + resources.getString(R.string.skipped)
    }

    // WORD

    fun showNewWord() {
        wordTextView.text = game.newWord
    }

    // COUNTER
    private fun pause() {
        timer?.cancel()
        isRunning = false
        if (RecordingFlag.recordingEnabled) {
            videoCapture.stopRecording()
        }
    }

    private fun resume() {
        isRunning = true
        timer = object: CountDownTimer((estimation * 10000).toLong(), 1000) {
            @SuppressLint("RestrictedApi")
            override fun onTick(millisUntilFinished: Long) {

                if (!isRunning) {
                    cancel()
                    return
                }
                if (estimation < 10) {
                    soundSystem.playTikTok()
                }

                estimation--
                if (estimation < 0) {
                    estimation = 0
                }
                timeTextView.text = "$estimation"

                if (estimation == 0) {
                    if (RecordingFlag.recordingEnabled) {
                        videoCapture.stopRecording()
                    }
                    soundSystem.playEnd()
                    isRunning = false
                    cancel()
                    Handler().postDelayed({
                        showResults()
                    }, 1000)

                }
            }

            override fun onFinish() {

            }
        }
        timer?.start()

        if (RecordingFlag.recordingEnabled) {
            // camera
            val file = File(externalMediaDirs.first(),
                "${System.currentTimeMillis()}.mp4")

            videoCapture.startRecording(file, Executors.newScheduledThreadPool(1), object: VideoCapture.OnVideoSavedListener {
                override fun onVideoSaved(file: File) {
                    Log.i("tag", "Video File : $file")
                    savedFile = file
                    savedFiles.add(file)
                }

                override fun onError(
                    videoCaptureError: VideoCapture.VideoCaptureError,
                    message: String,
                    cause: Throwable?
                ) {
                    Log.i("tag", "Video Error: $message")
                }

            })
        }
    }

    fun showResults() {
        val intent =  Intent(this, ResultsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("game", game)

        if (savedFile != null) {
            val uriString = Uri.fromFile(savedFile).toString()
            intent.putExtra("fileURIString", uriString)
        }

        val fileStrings = savedFiles.map {
            Uri.fromFile(it).toString()
        }
        intent.putStringArrayListExtra("fileURIStrings", ArrayList(fileStrings))

        startActivity(intent)
        finish()
        if (mInterstitialAd.isLoaded) {
            mInterstitialAd.show()
        }
    }


    // Camera stuff

    // Camera

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun startCamera() {
        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().build()
        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Create a configuration object for the video use case
        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setTargetRotation(viewFinder.display.rotation)
        }.build()
        videoCapture = VideoCapture(videoCaptureConfig)

        preview.setOnPreviewOutputUpdateListener {
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            viewFinder.surfaceTexture = it.surfaceTexture
            parent.addView(viewFinder, 0)
            updateTransform()
        }

        // Bind use cases to lifecycle
        CameraX.bindToLifecycle(this, preview, videoCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }


}
