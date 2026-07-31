package hr.azzi.socialgames.alias.Service.RoundVideo

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import kotlin.math.abs

/** The two cameras the clip is filmed with, how each one's frames must be turned, and how big they are. */
data class CameraPair(
    val backId: String,
    val backOrientation: Int,
    val backSize: Size,
    val frontId: String,
    val frontOrientation: Int,
    val frontSize: Size,
) {
    companion object {

        /**
         * The front + back pair to film with, preferring one the device says it can run
         * concurrently.
         *
         * The fallback is deliberate and is not a way around [hr.azzi.socialgames.alias
         * .Service.DualCameraSupport]: that gate has already said yes by the time anything
         * here runs. It exists because the emulator streams both cameras happily while
         * declaring no pair at all, and losing the ability to develop against it costs more
         * than the fallback does — on real hardware the gate will have used the declared
         * pair to answer, so this lands on the same two cameras anyway.
         */
        fun resolve(context: Context): CameraPair? {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return null
            return try {
                val facing = manager.cameraIdList.associateWith { id ->
                    manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
                }
                val declared = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    manager.concurrentCameraIds.firstNotNullOfOrNull { combination ->
                        val back = combination.firstOrNull {
                            facing[it] == CameraCharacteristics.LENS_FACING_BACK
                        }
                        val front = combination.firstOrNull {
                            facing[it] == CameraCharacteristics.LENS_FACING_FRONT
                        }
                        if (back != null && front != null) back to front else null
                    }
                } else {
                    null
                }

                val (backId, frontId) = declared ?: run {
                    val back = facing.entries.firstOrNull {
                        it.value == CameraCharacteristics.LENS_FACING_BACK
                    }?.key
                    val front = facing.entries.firstOrNull {
                        it.value == CameraCharacteristics.LENS_FACING_FRONT
                    }?.key
                    if (back == null || front == null) return null
                    Log.w(TAG, "no declared concurrent pair — falling back to $back + $front")
                    back to front
                }

                CameraPair(
                    backId = backId,
                    backOrientation = manager.orientationOf(backId),
                    backSize = manager.captureSizeOf(backId),
                    frontId = frontId,
                    frontOrientation = manager.orientationOf(frontId),
                    frontSize = manager.captureSizeOf(frontId),
                ).also {
                    // Worth a line in the log: everything the compositor does about turning
                    // and cropping is derived from these four numbers, so when a clip comes
                    // out sideways or stretched this is the first thing to read.
                    Log.w(
                        TAG,
                        "back ${it.backId} ${it.backOrientation}° ${it.backSize}, " +
                            "front ${it.frontId} ${it.frontOrientation}° ${it.frontSize}",
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "could not resolve a camera pair", t)
                null
            }
        }

        private fun CameraManager.orientationOf(id: String): Int =
            getCameraCharacteristics(id).get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

        /**
         * The size to film at, chosen from what the camera actually offers.
         *
         * Asking for 1280×720 and assuming that is what arrives is the mistake this replaces:
         * a device that does not support it delivers something else, and then every
         * assumption downstream — which side is long, what to crop — is wrong, which reads on
         * screen as a picture both turned and stretched.
         *
         * Preference order is closest to the clip's own 16:9-ish shape first, then largest,
         * capped at 720p because that is all concurrent capture guarantees.
         */
        private fun CameraManager.captureSizeOf(id: String): Size {
            val configs = getCameraCharacteristics(id)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = configs?.getOutputSizes(SurfaceTexture::class.java)?.toList()
            if (sizes.isNullOrEmpty()) {
                return Size(RoundVideoSpec.CAPTURE_WIDTH, RoundVideoSpec.CAPTURE_HEIGHT)
            }

            val wanted = RoundVideoSpec.CAPTURE_WIDTH.toFloat() / RoundVideoSpec.CAPTURE_HEIGHT
            val withinCap = sizes.filter {
                it.width <= RoundVideoSpec.CAPTURE_WIDTH && it.height <= RoundVideoSpec.CAPTURE_HEIGHT
            }
            val candidates = withinCap.ifEmpty { sizes }
            return candidates.minByOrNull { size ->
                val aspect = abs(size.width.toFloat() / size.height - wanted)
                // Aspect first, area second — a stretched frame is worse than a smaller one.
                aspect * 1000f - size.width.toFloat() * size.height / 1_000_000f
            } ?: Size(RoundVideoSpec.CAPTURE_WIDTH, RoundVideoSpec.CAPTURE_HEIGHT)
        }

        private const val TAG = "RoundVideo"
    }
}

/**
 * Both cameras, streaming into the surfaces the compositor draws from — or one, if that is
 * all the device turns out to manage.
 *
 * Each camera gets its own device and its own capture session; there is no combined session
 * in Camera2. They are opened one after the other rather than together, and that order is
 * the whole design: a device that cannot really run both evicts the first camera when the
 * second opens, so opening them at once would lose the back one — the full-bleed image and
 * the clock the whole clip is written against — and keep the small inset instead.
 *
 * Opened in sequence, the back camera is established first and the front is the one at risk.
 * If it cannot be had, it is dropped and the round is filmed with the back camera alone: the
 * compositor simply stops drawing the inset, and everything else about the clip is unchanged.
 */
class DualCameraSession(
    private val context: Context,
    private val pair: CameraPair,
    private val handler: Handler,
) {

    private val devices = mutableListOf<CameraDevice>()
    private val sessions = mutableListOf<CameraCaptureSession>()
    private var closed = false

    /** Set once the front has been given up on, so a late callback cannot revive it. */
    private var frontAbandoned = false

    private lateinit var backSurface: Surface
    private lateinit var frontSurface: Surface
    private lateinit var onDegraded: (String) -> Unit
    private lateinit var onFailure: (String) -> Unit

    /**
     * @param onDegraded the front camera could not be had; the clip is back-camera only.
     * @param onFailure nothing usable at all — the round is played without a clip.
     */
    fun open(
        back: Surface,
        front: Surface,
        onDegraded: (String) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        backSurface = back
        frontSurface = front
        this.onDegraded = onDegraded
        this.onFailure = onFailure
        openBack()
    }

    @SuppressLint("MissingPermission")
    private fun openBack() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            manager.openCamera(pair.backId, backCallback, handler)
        } catch (t: Throwable) {
            onFailure("openCamera(back): ${t.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun openFront() {
        if (closed || frontAbandoned) return
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            manager.openCamera(pair.frontId, frontCallback, handler)
        } catch (t: Throwable) {
            giveUpOnFront("openCamera(front): ${t.message}")
        }
    }

    private val backCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(device: CameraDevice) {
            if (closed) {
                device.close()
                return
            }
            devices += device
            // The front is only attempted once the back is actually streaming, so what the
            // device does about the second one can be told apart from a slow first one.
            stream(device, backSurface, onReady = { openFront() }, onProblem = { onFailure(it) })
        }

        override fun onDisconnected(device: CameraDevice) {
            devices -= device
            device.close()
            if (frontAbandoned || closed) {
                // Already down to one camera and it has gone too: nothing is left to film with.
                onFailure("back camera was disconnected")
                return
            }
            // This is a device saying it cannot do both, in the only way it says it. The
            // front is what has to go, and the back is opened again without it.
            Log.w(TAG, "back camera evicted when the front opened — dropping the front")
            giveUpOnFront("the back camera was evicted by the front")
            openBack()
        }

        override fun onError(device: CameraDevice, error: Int) {
            devices -= device
            device.close()
            onFailure("back camera error $error")
        }
    }

    private val frontCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(device: CameraDevice) {
            if (closed || frontAbandoned) {
                device.close()
                return
            }
            devices += device
            stream(
                device, frontSurface,
                onReady = { Log.w(TAG, "both cameras streaming") },
                onProblem = { giveUpOnFront(it) },
            )
        }

        override fun onDisconnected(device: CameraDevice) {
            devices -= device
            device.close()
            giveUpOnFront("front camera was disconnected")
        }

        override fun onError(device: CameraDevice, error: Int) {
            devices -= device
            device.close()
            giveUpOnFront("front camera error $error")
        }
    }

    /** Drops the front for good and tells the recorder to stop expecting its frames. */
    private fun giveUpOnFront(reason: String) {
        if (frontAbandoned) return
        frontAbandoned = true
        Log.w(TAG, "filming with the back camera only: $reason")
        onDegraded(reason)
    }

    @Suppress("DEPRECATION") // The successor form is API 28+; this runs on 30+ but reads the same.
    private fun stream(
        device: CameraDevice,
        surface: Surface,
        onReady: () -> Unit,
        onProblem: (String) -> Unit,
    ) {
        try {
            device.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        if (closed) {
                            session.close()
                            return
                        }
                        sessions += session
                        runCatching {
                            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                                .apply { addTarget(surface) }
                                .build()
                            session.setRepeatingRequest(request, null, handler)
                        }.onFailure {
                            onProblem("camera ${device.id} would not start: ${it.message}")
                            return
                        }
                        onReady()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onProblem("session for camera ${device.id} would not configure")
                    }
                },
                handler,
            )
        } catch (t: Throwable) {
            onProblem("createCaptureSession(${device.id}): ${t.message}")
        }
    }

    fun close() {
        closed = true
        sessions.forEach { runCatching { it.close() } }
        sessions.clear()
        devices.forEach { runCatching { it.close() } }
        devices.clear()
    }

    private companion object {
        const val TAG = "RoundVideo"
    }
}
