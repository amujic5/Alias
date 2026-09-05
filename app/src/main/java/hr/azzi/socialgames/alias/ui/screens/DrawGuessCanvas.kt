package hr.azzi.socialgames.alias.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias

/**
 * One continuous mark.
 *
 * The stroke keeps its own [Path] and extends it as the finger moves. The first version
 * of this kept raw points and rebuilt the path inside the draw block, which meant every
 * frame reconstructed every stroke from scratch: the cost of a frame grew with the size
 * of the drawing, so the pen got heavier the longer the round went on. A path built once
 * and appended to costs the same on the first stroke as on the fiftieth.
 */
class DrawStroke(
    val color: Color,
    val width: Float,
    val alpha: Float,
    val cap: StrokeCap,
) {
    val path = Path()
    private var last: Offset? = null

    fun start(at: Offset) {
        path.moveTo(at.x, at.y)
        last = at
    }

    /**
     * Adds a segment, unless the finger has barely moved.
     *
     * Touch events arrive far denser than the drawing needs — dropping the ones under a
     * couple of dp keeps the path short without any visible change to the line, and short
     * paths are what keep the redraw cheap.
     */
    fun extendTo(next: Offset, minDistance: Float): Boolean {
        val prev = last ?: return false
        if ((next - prev).getDistance() < minDistance) return false
        // Quadratic through the midpoint — the same curve the batch version drew, only
        // laid down one segment at a time.
        path.quadraticTo(prev.x, prev.y, (prev.x + next.x) / 2f, (prev.y + next.y) / 2f)
        last = next
        return true
    }

    /** A tap. Without a second point there is nothing for the round cap to draw. */
    fun dot(at: Offset) {
        path.moveTo(at.x, at.y)
        path.lineTo(at.x + 0.01f, at.y + 0.01f)
        last = at
    }
}

/**
 * The eight colours a drawing can use — enough that a sun, a tree and a fire are three
 * different things, few enough to pick one without looking away from the clock.
 */
private val InkColours = listOf(
    Color(0xFF0E1726), Color(0xFFFF453A), Color(0xFFFF9F0A), Color(0xFFFFD23D),
    Color(0xFF34C759), Color(0xFF1E88EA), Color(0xFFAF52DE), Color(0xFF8B5E3C),
)

internal enum class DrawInk { PEN, PENCIL, MARKER }

/**
 * Holds the drawing and the tools.
 *
 * Hoisted out of the composable because the play screen needs it too: it decides when to
 * lock the page, and it hands the finished drawing to the share card.
 */
class DrawGuessState {
    val strokes = mutableStateListOf<DrawStroke>()

    /**
     * Bumped once per segment, and read by the canvas so it knows to redraw.
     *
     * A stroke grows by mutating its own path, which Compose cannot see. The alternative —
     * replacing the last list entry on every touch event — allocated a stroke per event
     * and invalidated the whole list to say one path got longer.
     */
    internal var version by mutableIntStateOf(0)
    internal var ink by mutableStateOf(DrawInk.PEN)
    internal var nib by mutableIntStateOf(1)          // 0 thin, 1 medium, 2 thick
    internal var colourIndex by mutableIntStateOf(0)
    internal var erasing by mutableStateOf(false)

    /** True while the page is still blank — a round nobody drew in has nothing to share. */
    val isEmpty: Boolean get() = strokes.isEmpty()

    /** Set when the whistle blows: the round is over, and a drawing that keeps changing
     *  while the table argues about the answer helps nobody. */
    var frozen by mutableStateOf(false)

    /** The page's size in pixels, so the share card can redraw it at its own shape. */
    var canvasWidth by mutableIntStateOf(0)
    var canvasHeight by mutableIntStateOf(0)

    fun undo() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
        version++
    }

    fun clear() {
        strokes.clear()
        version++
    }

    internal val currentColour: Color get() = InkColours[colourIndex]

    /** Nib widths differ per ink because the inks do: a marker at a pen's width is a pen. */
    internal fun strokeWidth(density: Float): Float {
        val dp = when (ink) {
            DrawInk.MARKER -> listOf(12f, 22f, 36f)[nib]
            else -> listOf(3f, 7f, 15f)[nib]
        }
        return dp * density
    }

    internal val strokeAlpha: Float
        get() = when {
            erasing -> 1f
            ink == DrawInk.MARKER -> 0.45f
            ink == DrawInk.PENCIL -> 0.75f
            else -> 1f
        }

    internal val strokeCap: StrokeCap
        get() = if (ink == DrawInk.MARKER && !erasing) StrokeCap.Square else StrokeCap.Round

    /** A fresh stroke carrying whatever the toolbar is set to right now. */
    internal fun newStroke(density: Float) = DrawStroke(
        color = if (erasing) Color.White else currentColour,
        width = strokeWidth(density),
        alpha = strokeAlpha,
        cap = strokeCap,
    )
}

@Composable
fun rememberDrawGuessState(): DrawGuessState = remember { DrawGuessState() }

/**
 * The round's finished drawing, on its way from the play screen to the results screen.
 *
 * Held in memory rather than passed in the Intent: a bitmap of this size would sit right
 * against the 1 MB Binder transaction limit, and the two screens are a heartbeat apart in
 * the same process. Losing it to process death costs a share button, not a round.
 */
object DrawGuessRound {
    var drawing: Bitmap? = null
    var word: String? = null

    fun clear() { drawing = null; word = null }
}

/**
 * The drawing half of Draw & Guess: a canvas and the toolbar under it.
 *
 * Two toolbar rows because it cannot be one — six tools, three nibs and eight colours do
 * not fit across a phone at a size anyone can hit. What the mark is goes up top, what it
 * looks like goes underneath.
 *
 * The eraser paints white rather than deleting strokes. On a white page the result is the
 * same, it works mid-stroke where hit-testing would not, and undo still walks back through
 * it in the order things actually happened.
 */
@Composable
fun DrawGuessCanvas(state: DrawGuessState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    // Two dp of finger travel before a segment is worth recording.
    val minSegment = 2f * density
    Column(modifier) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(24.dp), clip = false)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.5.dp, Alias.divider, RoundedCornerShape(24.dp))
                .onSizeChanged { state.canvasWidth = it.width; state.canvasHeight = it.height }
                .pointerInput(state.frozen) {
                    if (state.frozen) return@pointerInput
                    detectDragGestures(
                        onDragStart = { at ->
                            state.strokes.add(state.newStroke(density).apply { start(at) })
                            state.version++
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val stroke = state.strokes.lastOrNull() ?: return@detectDragGestures
                            if (stroke.extendTo(change.position, minSegment)) state.version++
                        },
                    )
                }
                .pointerInput(state.frozen) {
                    if (state.frozen) return@pointerInput
                    // A tap is a dot. Without this, poking the page does nothing at all,
                    // which reads as a canvas that is not working.
                    detectTapGestures { at ->
                        state.strokes.add(state.newStroke(density).apply { dot(at) })
                        state.version++
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // Read so the draw phase re-runs when a stroke grows: the path itself is
                // mutated in place and Compose has no way to notice that.
                @Suppress("UNUSED_EXPRESSION") state.version
                state.strokes.forEach { s ->
                    drawPath(
                        path = s.path,
                        color = s.color,
                        alpha = s.alpha,
                        style = Stroke(width = s.width, cap = s.cap, join = StrokeJoin.Round),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Toolbar(state)
    }
}

@Composable
private fun Toolbar(state: DrawGuessState) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ToolButton(
                selected = !state.erasing && state.ink == DrawInk.PEN,
                content = { Icon(Icons.Filled.Edit, null, tint = it, modifier = Modifier.size(20.dp)) },
                onClick = { state.ink = DrawInk.PEN; state.erasing = false },
            )
            ToolButton(
                selected = !state.erasing && state.ink == DrawInk.PENCIL,
                content = { Icon(Icons.Filled.Create, null, tint = it, modifier = Modifier.size(20.dp)) },
                onClick = { state.ink = DrawInk.PENCIL; state.erasing = false },
            )
            ToolButton(
                selected = !state.erasing && state.ink == DrawInk.MARKER,
                content = { Icon(Icons.Filled.Brush, null, tint = it, modifier = Modifier.size(20.dp)) },
                onClick = { state.ink = DrawInk.MARKER; state.erasing = false },
            )
            ToolButton(
                selected = state.erasing,
                content = { Image(painterResource(R.drawable.ic_eraser), null, modifier = Modifier.size(20.dp)) },
                onClick = { state.erasing = true },
            )
            ToolButton(
                selected = false,
                content = { Icon(Icons.AutoMirrored.Filled.Undo, null, tint = it, modifier = Modifier.size(20.dp)) },
                onClick = { state.undo() },
            )
            ToolButton(
                selected = false,
                content = { Icon(Icons.Filled.Delete, null, tint = it, modifier = Modifier.size(20.dp)) },
                onClick = { state.clear() },
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            // Nib size, drawn at the size it stands for — three identical circles with a
            // number under them would need reading, and this does not.
            listOf(5.dp, 10.dp, 16.dp).forEachIndexed { index, dot ->
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (state.nib == index) Alias.divider else Color.Transparent)
                        .clickable { state.nib = index },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(dot).clip(CircleShape).background(Color(0xFF5A6B84)))
                }
            }
            Spacer(Modifier.width(2.dp))
            Box(Modifier.width(1.dp).height(20.dp).background(Alias.divider))
            Spacer(Modifier.width(2.dp))
            InkColours.forEachIndexed { index, colour ->
                val selected = !state.erasing && state.colourIndex == index
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colour)
                        .border(if (selected) 3.dp else 0.dp, Alias.accent, CircleShape)
                        // Picking a colour is also how you come back from the eraser —
                        // nobody taps the pen first, they tap the colour they want next.
                        .clickable { state.colourIndex = index; state.erasing = false },
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    selected: Boolean,
    content: @Composable (Color) -> Unit,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) Alias.accent else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content(if (selected) Alias.textPrimary else Color(0xFF5A6B84)) }
}

/**
 * The drawing on a white page, as a bitmap the share card can lay under the word.
 *
 * Rendered from the strokes rather than captured off the screen: the canvas is gone by the
 * time the results screen asks for it, and the strokes are still here.
 */
fun DrawGuessState.toBitmap(width: Int, height: Int): Bitmap? {
    if (isEmpty || width <= 0 || height <= 0) return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    strokes.forEach { s ->
        paint.color = s.color.copy(alpha = s.alpha).toArgb()
        paint.strokeWidth = s.width
        paint.strokeCap = if (s.cap == StrokeCap.Square)
            android.graphics.Paint.Cap.SQUARE else android.graphics.Paint.Cap.ROUND
        canvas.drawPath(s.path.asAndroidPath(), paint)
    }
    return bitmap
}

/**
 * Asked when the clock runs out with nobody having pressed either way.
 *
 * The card names the word while asking: half the fun of a drawing game is finding out what
 * that shape was supposed to be, and without the answer on screen the round ends in an
 * argument instead of a laugh. Not dismissible — the round cannot be left unscored.
 */
@Composable
fun DrawGuessVerdictDialog(word: String, onVerdict: (Boolean) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(20.dp, RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.draw_guess_was_it_guessed),
                color = Alias.textPrimary, fontFamily = Alias.display,
                fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.draw_guess_times_up),
                color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF4F7FB))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.draw_guess_the_word_was).uppercase(),
                    color = Alias.textSecondary, fontFamily = Alias.body,
                    fontWeight = FontWeight.Bold, fontSize = 11.sp,
                )
                Text(
                    word, color = Alias.textPrimary, fontFamily = Alias.display,
                    fontWeight = FontWeight.ExtraBold, fontSize = 30.sp,
                    maxLines = 1, textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(20.dp))
            VerdictButton(
                label = stringResource(R.string.draw_guess_guessed),
                background = Alias.success, ink = Color.White,
            ) { onVerdict(true) }
            Spacer(Modifier.height(12.dp))
            VerdictButton(
                label = stringResource(R.string.draw_guess_not_guessed),
                background = Color.White, ink = Color(0xFF3E4A5C), outlined = true,
            ) { onVerdict(false) }
        }
    }
}

@Composable
private fun VerdictButton(
    label: String,
    background: Color,
    ink: Color,
    outlined: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .then(if (outlined) Modifier.border(1.5.dp, Alias.divider, RoundedCornerShape(18.dp)) else Modifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = ink, fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}
