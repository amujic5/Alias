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
 * Both cameras, streaming at once into the surfaces the compositor draws from.
 *
 * Each camera gets its own device and its own capture session — there is no combined
 * session in Camera2. What makes them run together is that the pair came from
 * `getConcurrentCameraIds()`, so the camera service has already agreed to the combination
 * and will not evict one when the other opens.
 */
class DualCameraSession(
    private val context: Context,
    private val pair: CameraPair,
    private val handler: Handler,
) {

    private val devices = mutableListOf<CameraDevice>()
    private val sessions = mutableListOf<CameraCaptureSession>()
    private var closed = false

    @SuppressLint("MissingPermission")
    fun open(back: Surface, front: Surface, onFailure: (String) -> Unit) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            manager.openCamera(pair.backId, callback(back, onFailure), handler)
            manager.openCamera(pair.frontId, callback(front, onFailure), handler)
        } catch (t: Throwable) {
            onFailure("openCamera: ${t.message}")
        }
    }

    private fun callback(surface: Surface, onFailure: (String) -> Unit) =
        object : CameraDevice.StateCallback() {

            override fun onOpened(device: CameraDevice) {
                if (closed) {
                    device.close()
                    return
                }
                devices += device
                startStreaming(device, surface, onFailure)
            }

            override fun onDisconnected(device: CameraDevice) {
                // On a device that cannot really run both, this is how it says so: the
                // second open evicts the first rather than failing outright.
                Log.w(TAG, "camera ${device.id} disconnected")
                device.close()
                devices -= device
                onFailure("camera ${device.id} was evicted")
            }

            override fun onError(device: CameraDevice, error: Int) {
                Log.w(TAG, "camera ${device.id} error $error")
                device.close()
                devices -= device
                onFailure("camera ${device.id} error $error")
            }
        }

    @Suppress("DEPRECATION") // createCaptureSession(List, ...) is the only form below API 28's successor everywhere we run
    private fun startStreaming(device: CameraDevice, surface: Surface, onFailure: (String) -> Unit) {
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
                        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                            .apply { addTarget(surface) }
                            .build()
                        session.setRepeatingRequest(request, null, handler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onFailure("session for camera ${device.id} would not configure")
                    }
                },
                handler,
            )
        } catch (t: Throwable) {
            onFailure("createCaptureSession: ${t.message}")
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
