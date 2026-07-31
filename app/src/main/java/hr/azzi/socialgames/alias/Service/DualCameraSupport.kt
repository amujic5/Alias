package hr.azzi.socialgames.alias.Service

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import hr.azzi.socialgames.alias.BuildConfig

/**
 * Whether this device can film both cameras at once — the one thing Capture Moment needs
 * and the reason it is not offered everywhere.
 *
 * iOS asks `AVCaptureMultiCamSession.isMultiCamSupported` and gets a yes on every device
 * still in use. Android has no such luxury: concurrent capture arrived in API 30, is
 * optional for manufacturers, and a large part of the mid-range simply cannot do it. So
 * the feature is gated on a real capability check rather than on an OS version — a device
 * on Android 14 may still say no.
 *
 * The declaration is not trusted on its own, though. `FEATURE_CAMERA_CONCURRENT` turns out
 * to under-report: an SM-A376B does not declare it and films both cameras perfectly well, so
 * a device that stays quiet is given the benefit of the doubt rather than turned away. What
 * is still worth reading is the device that *does* declare it — there
 * `getConcurrentCameraIds()` says which combinations, and only a pair that is actually front
 * + back is any use to us. A pair of back cameras (wide + tele) is a legitimate answer to the
 * first question and worthless for this feature.
 *
 * The cost of the benefit of the doubt is borne where it can be: a device that genuinely
 * cannot do it evicts the first camera when the second opens, `DualCameraSession` reports
 * that, and the round is played without a clip.
 */
object DualCameraSupport {

    private const val TAG = "DualCameraSupport"

    /** See [debugOverride]. */
    private const val OVERRIDE_SETTING = "alias_dual_camera"

    /** Resolved once — the answer cannot change while the app is running. */
    @Volatile
    private var cached: Boolean? = null

    fun isSupported(context: Context): Boolean =
        cached ?: synchronized(this) {
            cached ?: resolve(context).also { cached = it }
        }

    /**
     * Debug builds only: an escape hatch from the check below.
     *
     * The emulator will happily stream both cameras at once, but its camera HAL never
     * declares the front + back pair, so the honest check hides the entire feature there and
     * leaves nothing to develop against. This forces the answer either way:
     *
     *     adb shell settings put global alias_dual_camera 1   # pretend it is supported
     *     adb shell settings put global alias_dual_camera 0   # pretend it is not
     *     adb shell settings delete global alias_dual_camera  # ask the device again
     *
     * Forcing it on says nothing about whether a device can really do this — on hardware that
     * does not declare the pair, opening the second camera tends to evict the first.
     */
    private fun debugOverride(context: Context): Boolean? {
        if (!BuildConfig.DEBUG) return null
        return when (Settings.Global.getInt(context.contentResolver, OVERRIDE_SETTING, -1)) {
            1 -> true
            0 -> false
            else -> null
        }
    }

    /**
     * How the answer was arrived at. Not a gate — the gate is a yes or a no — but the thing
     * to read first when someone reports that Capture Moment is missing, or that it is there
     * and produces nothing.
     */
    enum class Diagnosis {
        /** Below API 30; concurrent capture did not exist yet. */
        TOO_OLD,

        /** The device declares the pair. The strongest answer there is. */
        DECLARED_PAIR,

        /** It declares the feature but no front + back pair — wide + tele is not this. */
        DECLARED_NO_PAIR,

        /**
         * It says nothing, and is offered it anyway. An SM-A376B declares neither the feature
         * nor a pair and films both cameras perfectly well, so silence is not taken for no.
         */
        UNDECLARED,

        /** No camera service, or it threw while being asked what it can do. */
        UNAVAILABLE,
    }

    private fun resolve(context: Context): Boolean {
        debugOverride(context)?.let {
            Log.w(TAG, "debug override: supported=$it")
            return it
        }
        val (supported, diagnosis) = diagnose(context)
        // One line with everything the answer was based on. Without the device on the desk
        // this is the whole of what a report has to go on, so it says what was asked as well
        // as what came back.
        Log.w(
            TAG,
            "capture moment $supported ($diagnosis) — ${Build.MANUFACTURER} ${Build.MODEL}, " +
                "API ${Build.VERSION.SDK_INT}, feature=${hasFeature(context)}, " +
                "concurrentCameraIds=${concurrentIds(context)}",
        )
        return supported
    }

    private fun diagnose(context: Context): Pair<Boolean, Diagnosis> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false to Diagnosis.TOO_OLD

        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return false to Diagnosis.UNAVAILABLE

        return try {
            val facing = manager.cameraIdList.associateWith { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
            }
            val declaredPair = manager.concurrentCameraIds.any { combination ->
                val facings = combination.mapNotNull { facing[it] }
                facings.contains(CameraCharacteristics.LENS_FACING_FRONT) &&
                    facings.contains(CameraCharacteristics.LENS_FACING_BACK)
            }
            when {
                declaredPair -> true to Diagnosis.DECLARED_PAIR
                hasFeature(context) -> false to Diagnosis.DECLARED_NO_PAIR
                else -> true to Diagnosis.UNDECLARED
            }
        } catch (t: Throwable) {
            Log.w(TAG, "concurrent camera query failed", t)
            false to Diagnosis.UNAVAILABLE
        }
    }

    private fun hasFeature(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT)

    private fun concurrentIds(context: Context): String =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            "n/a"
        } else {
            runCatching {
                (context.getSystemService(Context.CAMERA_SERVICE) as CameraManager)
                    .concurrentCameraIds.toString()
            }.getOrElse { "unreadable" }
        }
}
