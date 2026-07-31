package hr.azzi.socialgames.alias.ui.screens

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias

private val StageTop = Color(0xFF3C4759)
private val StageBottom = Color(0xFF1E2634)
private val PlateBackground = Color(0xE00E1726)
private val PillBackground = Color(0xB80E1726)

/**
 * The play screen in camera mode (Figma "11b Play - Playing · Camera Mode", node 3202:590).
 *
 * The two feeds are not two views: the recorder is already compositing both cameras, so it
 * draws the stage into this one surface as well. Everything on top — the labels, the word
 * plate — is Compose, which is why it uses the app's own type and colours rather than being
 * baked into the video the way the clip's overlay is.
 */
@Composable
fun CameraStage(
    word: String,
    onSurface: (Surface?, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    /** False once the front camera has been dropped — there is no inset to label. */
    hasFrontCamera: Boolean = true,
) {
    val current = rememberUpdatedState(onSurface)

    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(StageTop, StageBottom))),
    ) {
        val callback = remember {
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) = Unit

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    current.value(holder.surface, width, height)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    current.value(null, 0, 0)
                }
            }
        }
        // The surface outlives the composition on a fast back-press, and the compositor would
        // go on drawing into one Android has already torn down.
        DisposableEffect(Unit) { onDispose { current.value(null, 0, 0) } }

        AndroidView(
            factory = { context -> SurfaceView(context).apply { holder.addCallback(callback) } },
            modifier = Modifier.fillMaxSize(),
        )

        // Sits over the PiP the compositor draws in the same corner.
        if (hasFrontCamera) {
            CameraPill(
                label = stringResource(R.string.camera_label_front),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 20.dp),
            )
        }
        CameraPill(
            label = stringResource(R.string.camera_label_back),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 26.dp),
        )

        WordPlate(word, Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp))
    }
}

@Composable
private fun WordPlate(word: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 38.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PlateBackground)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.camera_your_word),
            color = Color.White.copy(alpha = 0.72f),
            fontFamily = Alias.body,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 0.88.sp,
        )
        Text(
            word,
            color = Color.White,
            fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 38.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** The little "which camera is this" tag, with its live dot. */
@Composable
private fun CameraPill(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(15.dp))
            .background(PillBackground)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(12.dp))
        Text(
            label,
            color = Color.White,
            fontFamily = Alias.body,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 0.22.sp,
        )
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Alias.brandTop))
    }
}
