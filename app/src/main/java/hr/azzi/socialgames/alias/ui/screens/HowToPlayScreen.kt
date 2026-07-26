package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

/** One tutorial slide: illustration + title + body (ported from iOS `htp_step*`). */
private data class HtpStep(val image: Int, val titleRes: Int, val bodyRes: Int)

private val HtpSteps = listOf(
    HtpStep(R.drawable.htp_1, R.string.htp_step1_title, R.string.htp_step1_body),
    HtpStep(R.drawable.htp_2, R.string.htp_step2_title, R.string.htp_step2_body),
    HtpStep(R.drawable.htp_3, R.string.htp_step3_title, R.string.htp_step3_body),
    HtpStep(R.drawable.htp_4, R.string.htp_step4_title, R.string.htp_step4_body),
)

private val BlueTile = Color(0xFFEAF6FF)
private val DotIdle = Color(0xFFCBD5E1)

/**
 * "How to Play?" — a white card over the dimmed brand gradient with a swipeable 4-step
 * carousel, page dots and a GOT IT button. Matches Figma nodes 2401:203 / 2402:342.
 */
@Composable
fun HowToPlayScreen(onGotIt: () -> Unit) {
    val pager = rememberPagerState(pageCount = { HtpSteps.size })

    BrandBackground {
        // Dim scrim over the gradient (Figma rgba(11,23,38,0.5)).
        Box(Modifier.fillMaxSize().background(Color(0xFF0B1726).copy(alpha = 0.5f)))

        Box(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White)
                    .padding(vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.how_to_play),
                    color = Alias.textPrimary, fontFamily = Alias.body, fontWeight = FontWeight.Bold,
                    fontSize = 24.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(18.dp))

                StepCarousel(pager = pager, sidePadding = 24.dp, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(18.dp))
                PageDots(count = HtpSteps.size, current = pager.currentPage)
                Spacer(Modifier.height(18.dp))

                Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    PillButton(stringResource(R.string.got_it), PillKind.Primary, onClick = onGotIt)
                }
            }
        }
    }
}

/**
 * The swipeable step carousel, locked to the height of its TALLEST slide so the card
 * doesn't grow/shrink (blink) while paging. We subcompose all pages once, measure each
 * at the real page width, take the max height, then render the pager at that height —
 * shorter slides simply get empty space below. Adapts to language / font scale.
 */
@Composable
private fun StepCarousel(pager: PagerState, sidePadding: Dp, modifier: Modifier = Modifier) {
    SubcomposeLayout(modifier) { constraints ->
        val sidePx = sidePadding.roundToPx()
        val pageWidth = (constraints.maxWidth - sidePx * 2).coerceAtLeast(0)
        val pageConstraints = Constraints(minWidth = pageWidth, maxWidth = pageWidth)
        val maxH = subcompose("measure") {
            HtpSteps.forEach { StepPage(it) }
        }.maxOfOrNull { it.measure(pageConstraints).height } ?: 0

        val pagerPlaceable = subcompose("pager") {
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = sidePadding),
                pageSpacing = 16.dp,
                modifier = Modifier.height(maxH.toDp()),
            ) { page -> StepPage(HtpSteps[page]) }
        }.first().measure(constraints.copy(minHeight = maxH, maxHeight = maxH))

        layout(constraints.maxWidth, maxH) { pagerPlaceable.place(0, 0) }
    }
}

@Composable
private fun StepPage(step: HtpStep) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(22.dp)).background(BlueTile),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(step.image), contentDescription = null,
                modifier = Modifier.height(112.dp), contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(step.titleRes), color = Alias.textPrimary, fontFamily = Alias.body,
            fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(step.bodyRes), color = Alias.textSecondary, fontFamily = Alias.body,
            fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
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
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Alias.accent else DotIdle),
            )
        }
    }
}
