package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.LanguageManager
import hr.azzi.socialgames.alias.ui.theme.Alias

/** One carousel page: illustration, title, body. Mirrors the iOS `DualCameraStepCell`. */
private data class DualCamStep(val image: Int, val titleRes: Int, val bodyRes: Int)

private val HeroTile = Color(0xFFEAF3FD)
private val DotIdle = Color(0xFFCBD5E1)
private val SheetTitle = Color(0xFF0E1726)
private val SheetBody = Color(0xFF7E90AC)
private val SheetDivider = Color(0xFFE6ECF2)

/**
 * The illustrations carry hand-lettered speech bubbles, so they are drawn per language
 * rather than translated. Only four sets exist: Bosnian borrows the Croatian drawings,
 * and every language without artwork of its own falls back to English — a neighbouring
 * language reads better than a mismatched one.
 */
private fun artVariant(code: String): String = when (code) {
    "hr", "bs" -> "hr"
    "sr" -> "sr"
    "de" -> "de"
    else -> "en"
}

private fun steps(variant: String): List<DualCamStep> {
    val art = when (variant) {
        "hr" -> listOf(R.drawable.dualcam_1_hr, R.drawable.dualcam_2_hr, R.drawable.dualcam_3_hr)
        "sr" -> listOf(R.drawable.dualcam_1_sr, R.drawable.dualcam_2_sr, R.drawable.dualcam_3_sr)
        "de" -> listOf(R.drawable.dualcam_1_de, R.drawable.dualcam_2_de, R.drawable.dualcam_3_de)
        else -> listOf(R.drawable.dualcam_1_en, R.drawable.dualcam_2_en, R.drawable.dualcam_3_en)
    }
    return listOf(
        DualCamStep(art[0], R.string.dualcam_title_1, R.string.dualcam_body_1),
        DualCamStep(art[1], R.string.dualcam_title_2, R.string.dualcam_body_2),
        DualCamStep(art[2], R.string.dualcam_title_3, R.string.dualcam_body_3),
    )
}

/**
 * The dual-camera explainer — a three-page carousel over the localized illustration, then
 * the enable toggle. Figma "06 Settings – Dual Camera" (node 3155:381), same sheet the
 * iOS `CameraInfoViewController` shows.
 *
 * Presentational only: turning the switch on has to clear the camera and microphone
 * permissions first, and that flow lives with the caller so the settings checkbox and this
 * switch go through exactly the same path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualCameraSheet(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pager = rememberPagerState(pageCount = { 3 })
    val steps = steps(artVariant(LanguageManager.currentCode))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepCarousel(steps = steps, pager = pager, sidePadding = 10.dp, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(12.dp))
            PageDots(count = steps.size, current = pager.currentPage)
            Spacer(Modifier.height(18.dp))

            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(1.dp).background(SheetDivider))
            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.dualcam_enable),
                    color = SheetTitle, fontFamily = Alias.body, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = isOn,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Alias.success,
                        checkedBorderColor = Alias.success,
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Locked to the height of the tallest page so the sheet doesn't jump while paging — the
 * same trick `HowToPlayScreen` uses, and needed for the same reason: translations differ
 * in length and the copy sits above a fixed toggle row.
 */
@Composable
private fun StepCarousel(
    steps: List<DualCamStep>,
    pager: PagerState,
    sidePadding: Dp,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        val sidePx = sidePadding.roundToPx()
        val pageWidth = (constraints.maxWidth - sidePx * 2).coerceAtLeast(0)
        val pageConstraints = Constraints(minWidth = pageWidth, maxWidth = pageWidth)
        val maxH = subcompose("measure") {
            steps.forEach { StepPage(it) }
        }.maxOfOrNull { it.measure(pageConstraints).height } ?: 0

        val pagerPlaceable = subcompose("pager") {
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = sidePadding),
                pageSpacing = 16.dp,
                modifier = Modifier.height(maxH.toDp()),
            ) { page -> StepPage(steps[page]) }
        }.first().measure(constraints.copy(minHeight = maxH, maxHeight = maxH))

        layout(constraints.maxWidth, maxH) { pagerPlaceable.place(0, 0) }
    }
}

@Composable
private fun StepPage(step: DualCamStep) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(232.dp).clip(RoundedCornerShape(20.dp)).background(HeroTile),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(step.image), contentDescription = null,
                modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(step.titleRes), color = SheetTitle, fontFamily = Alias.body,
            fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp,
            textAlign = TextAlign.Center, maxLines = 2,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(step.bodyRes), color = SheetBody, fontFamily = Alias.body,
            fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp,
            textAlign = TextAlign.Center, maxLines = 3,
        )
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == current
            Box(
                Modifier.height(7.dp).width(if (active) 20.dp else 7.dp)
                    .clip(RoundedCornerShape(50)).background(if (active) Alias.accent else DotIdle),
            )
        }
    }
}
