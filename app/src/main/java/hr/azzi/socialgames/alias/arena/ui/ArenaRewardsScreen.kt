package hr.azzi.socialgames.alias.arena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.JSONService
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.arena.ArenaPalette
import hr.azzi.socialgames.alias.arena.ArenaSummary
import hr.azzi.socialgames.alias.arena.LevelReward
import hr.azzi.socialgames.alias.arena.LevelRewards
import hr.azzi.socialgames.alias.ui.theme.Alias
import androidx.compose.ui.platform.LocalContext

/**
 * The two reward screens: the level-up celebration, and the whole ladder.
 *
 * Android port of iOS `ArenaRewardsView.swift`.
 */

// MARK: - Level up

/**
 * What a player sees the moment a level lands something.
 *
 * Fixed bright blue, like the Arena promo and for the same reason: it is one branded
 * surface drawn as a unit, and half of it repainted for daylight would be gold on white.
 * It is also on screen for about four seconds.
 *
 * Shown only when the level actually gave something. A level with nothing on it passes
 * silently — a celebration that congratulates you on receiving nothing teaches people to
 * dismiss the next one without reading it.
 */
@Composable
fun ArenaLevelUpDialog(
    level: Int,
    rewards: List<LevelReward>,
    onDone: () -> Unit,
    onViewAll: (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1B63D6), Color(0xFF0A3B93)))),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LevelStar(level)
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(
                        when {
                            rewards.contains(LevelReward.NoAds) -> R.string.arena_levelup_noads
                            rewards.any { it.isDeck } -> R.string.arena_levelup_deck
                            rewards.isNotEmpty() -> R.string.arena_levelup_avatar
                            else -> R.string.arena_levelup_generic
                        }
                    ),
                    fontFamily = Alias.display, fontSize = 31.sp, color = Color.White,
                    textAlign = TextAlign.Center, maxLines = 2,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    LevelRewards.nextRewardLevel(level)
                        ?.let { stringResource(R.string.arena_levelup_next, it) }
                        ?: stringResource(R.string.arena_levelup_next_none),
                    fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center, maxLines = 2,
                )

                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    rewards.forEach { RewardCard(it) }
                }

                Spacer(Modifier.height(22.dp))
                Text(
                    stringResource(R.string.arena_levelup_reached, level),
                    fontFamily = Alias.display, fontSize = 21.sp, color = Color.White, maxLines = 1,
                )

                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.arena_levelup_ok),
                    fontFamily = Alias.display, fontSize = 24.sp, color = Alias.onAccent,
                    textAlign = TextAlign.Center, maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Alias.accent)
                        .clickable(onClick = onDone)
                        .padding(vertical = 18.dp),
                )
                if (onViewAll != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.arena_levelup_view_all),
                        fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = Color.White,
                        modifier = Modifier.clickable(onClick = onViewAll).padding(4.dp),
                    )
                }
            }
        }
    }
}

/** The level, on a star. */
@Composable
private fun LevelStar(level: Int) {
    Box(Modifier.size(186.dp), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Filled.Star, null, tint = Color(0xFFFFD23D),
            modifier = Modifier.size(186.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp)) {
            Text(
                stringResource(R.string.arena_levelup_level_word),
                fontFamily = Alias.display, fontSize = 20.sp, color = Color(0xFF0E1726), maxLines = 1,
            )
            Text(
                "$level",
                fontFamily = Alias.display, fontSize = 52.sp, color = Color(0xFF0E1726), maxLines = 1,
            )
        }
    }
}

@Composable
private fun RewardCard(reward: LevelReward) {
    val gold = reward == LevelReward.NoAds
    Column(
        Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (gold) Brush.linearGradient(listOf(Color(0xFFFFE486), Color(0xFFFFC529)))
                else Brush.linearGradient(listOf(Color.White, Color.White))
            )
            .border(3.dp, if (gold) Color.White.copy(alpha = 0.75f) else Color(0xFFFFD23D), RoundedCornerShape(22.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(
                when (reward) {
                    is LevelReward.Deck -> R.string.arena_reward_badge_deck
                    is LevelReward.Avatar -> R.string.arena_reward_badge_avatar
                    LevelReward.NoAds -> R.string.arena_reward_badge_noads
                }
            ),
            fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 12.sp,
            color = Color.White, maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(ArenaPalette.live)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
        RewardArt(reward, 108.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                rewardTitle(reward),
                fontFamily = Alias.display, fontSize = 19.sp, color = Color(0xFF0E1726),
                textAlign = TextAlign.Center, maxLines = 2,
            )
            if (gold) {
                Text(
                    stringResource(R.string.arena_reward_noads_sub),
                    fontFamily = Alias.body, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                    color = Color(0xFF0E1726).copy(alpha = 0.7f),
                    textAlign = TextAlign.Center, maxLines = 2,
                )
            }
        }
    }
}

// MARK: - Reward grid

/**
 * The whole ladder: what you have, what is next, and what is coming.
 *
 * Adaptive rather than fixed like the celebration, because this one is a screen a player
 * opens deliberately from their profile and reads, not a four-second flourish.
 */
@Composable
fun ArenaRewardGridScreen(summary: ArenaSummary, onBack: () -> Unit) {
    val nextLevel = LevelRewards.nextRewardLevel(summary.level)

    Column(Modifier.fillMaxSize().background(ArenaColors.backdrop)) {
        ArenaScreenHeader(stringResource(R.string.arena_reward_grid_title), onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 28.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                ProgressCard(summary)
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                InfoCard()
            }
            // Everything except the grand prize, which is not a grid item.
            items(
                LevelRewards.ladder.filter { it.second != LevelReward.NoAds },
                key = { it.second.key },
            ) { (level, reward) ->
                RewardTile(level, reward, summary.level, nextLevel)
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                GrandPrizeCard(summary)
            }
        }
    }
}

@Composable
private fun ProgressCard(summary: ArenaSummary) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ArenaColors.card)
            .border(1.dp, ArenaColors.border, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, null, tint = Alias.accent, modifier = Modifier.size(58.dp))
            Text("${summary.level}", fontFamily = Alias.display, fontSize = 22.sp, color = Alias.onAccent)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.arena_reward_level, summary.level),
                    fontFamily = Alias.display, fontSize = 22.sp, color = ArenaColors.title, maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.ai_hub_xp_progress, summary.xpIntoLevel, summary.xpForLevel),
                    fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = ArenaColors.body, maxLines = 1,
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(ArenaColors.well)) {
                Box(
                    Modifier.fillMaxWidth(summary.levelProgress).height(10.dp)
                        .clip(RoundedCornerShape(50)).background(Alias.accent),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.arena_reward_hint),
                fontFamily = Alias.body, fontSize = 12.sp, color = ArenaColors.body, maxLines = 1,
            )
        }
    }
}

@Composable
private fun InfoCard() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ArenaColors.card)
            .border(1.dp, ArenaColors.border, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.CardGiftcard, null, tint = Alias.accent, modifier = Modifier.size(22.dp))
        Column {
            Text(
                stringResource(R.string.arena_reward_info_title),
                fontFamily = Alias.display, fontSize = 17.sp, color = ArenaColors.title, maxLines = 1,
            )
            Text(
                stringResource(R.string.arena_reward_info_sub),
                fontFamily = Alias.body, fontSize = 12.sp, color = ArenaColors.body, maxLines = 2,
            )
        }
    }
}

@Composable
private fun RewardTile(level: Int, reward: LevelReward, currentLevel: Int, nextLevel: Int?) {
    val owned = currentLevel >= level
    val isNext = level == nextLevel
    // Far enough out that naming it would be arithmetic rather than anticipation.
    val hidden = !owned && !reward.isAlwaysRevealed && level > currentLevel + LevelRewards.REVEAL_HORIZON

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 168.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ArenaColors.card)
            .border(
                if (owned || isNext) 2.dp else 1.dp,
                if (owned || isNext) Alias.accent else ArenaColors.border,
                RoundedCornerShape(18.dp),
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.arena_reward_level, level),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 13.sp,
                color = ArenaColors.title, maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            when {
                isNext -> Text(
                    stringResource(R.string.arena_reward_next),
                    fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 10.sp,
                    color = Color.White, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(ArenaPalette.live)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
                !owned -> Box(
                    Modifier.size(24.dp).clip(CircleShape).background(Alias.accent),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Lock, null, tint = Alias.onAccent, modifier = Modifier.size(13.dp)) }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (hidden) MysteryArt(reward.isDeck, 72.dp) else RewardArt(reward, 72.dp, dim = !owned)
        Spacer(Modifier.height(6.dp))
        Text(
            if (hidden) stringResource(
                if (reward.isDeck) R.string.arena_reward_mystery_deck else R.string.arena_reward_mystery_avatar
            ) else rewardTitle(reward),
            fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            color = if (owned) ArenaColors.title else ArenaColors.body,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        if (owned) {
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Alias.accent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Filled.Check, null, tint = Alias.onAccent, modifier = Modifier.size(11.dp))
                Text(
                    stringResource(R.string.arena_reward_owned),
                    fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 10.sp,
                    color = Alias.onAccent, maxLines = 1,
                )
            }
        }
    }
}

/**
 * No Ads, at the end of everything, in a card of its own.
 *
 * It was a grid tile like the other twenty-four, half a screen wide and reading as the
 * twenty-fifth avatar. It is not one of those: it is the only reward on the ladder that
 * changes how the whole app behaves, and the only one anybody would otherwise pay for.
 */
@Composable
private fun GrandPrizeCard(summary: ArenaSummary) {
    val level = LevelRewards.NO_ADS_LEVEL
    val owned = summary.level >= level
    val toGo = maxOf(0, level - summary.level)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFD95A), Color(0xFFFFB300))))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RewardArt(LevelReward.NoAds, 76.dp)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.arena_reward_level, level),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 11.sp,
                letterSpacing = 0.9.sp, color = Alias.onAccent.copy(alpha = 0.62f), maxLines = 1,
            )
            Text(
                stringResource(R.string.remove_ads_title),
                fontFamily = Alias.display, fontSize = 24.sp, color = Alias.onAccent, maxLines = 1,
            )
            Text(
                stringResource(R.string.arena_reward_noads_sub),
                fontFamily = Alias.body, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                color = Alias.onAccent.copy(alpha = 0.72f), maxLines = 2,
            )
        }
        // Owned says so; locked says how much further, which is the one number somebody
        // looking at this card actually wants.
        Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (owned) {
                Icon(Icons.Filled.Verified, null, tint = Alias.onAccent, modifier = Modifier.size(26.dp))
            } else {
                Text("$toGo", fontFamily = Alias.display, fontSize = 24.sp, color = Alias.onAccent, maxLines = 1)
                Text(
                    stringResource(R.string.arena_reward_levels_to_go),
                    fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 9.sp,
                    letterSpacing = 0.5.sp, color = Alias.onAccent.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center, maxLines = 2,
                )
            }
        }
    }
}

// MARK: - Shared pieces

@Composable
private fun rewardTitle(reward: LevelReward): String = when (reward) {
    is LevelReward.Deck -> {
        val ctx = LocalContext.current
        val deck = JSONService.boardGames.firstOrNull { it.id == reward.id }
        // A deck's `name` is a localization key for most decks and a plain word for the
        // two that were never translated ("NBA", "Movies & TV").
        deck?.let {
            val res = ctx.resources.getIdentifier(it.name, "string", ctx.packageName)
            if (res != 0) stringResource(res) else it.name
        } ?: ""
    }
    is LevelReward.Avatar -> stringResource(hr.azzi.socialgames.alias.arena.AliasAvatar.named(reward.id).nameRes)
    LevelReward.NoAds -> stringResource(R.string.remove_ads_title)
}

/** A reward's picture: the avatar itself, or the deck's box art. */
@Composable
private fun RewardArt(reward: LevelReward, size: androidx.compose.ui.unit.Dp, dim: Boolean = false) {
    val alpha = if (dim) 0.6f else 1f
    when (reward) {
        LevelReward.NoAds -> NoAdsArt(size)
        is LevelReward.Avatar -> Box(Modifier.size(size).clip(CircleShape)) {
            ArenaAvatarImage(reward.id, size, ring = null)
        }
        is LevelReward.Deck -> {
            val ctx = LocalContext.current
            val deck = JSONService.boardGames.firstOrNull { it.id == reward.id }
            val resId = deck?.let { ctx.resources.getIdentifier(it.image, "drawable", ctx.packageName) } ?: 0
            Box(Modifier.size(size).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                if (resId != 0) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(resId), contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.size(size),
                    )
                } else {
                    Icon(Icons.Filled.CardGiftcard, null, tint = Alias.blue600, modifier = Modifier.size(size * 0.4f))
                }
            }
        }
    }
}

/**
 * Drawn rather than an icon. A prohibition sign says "no" without saying no to *what*,
 * and at 40dp in a grid it reads as a road sign. A banner with a line through it says it
 * in one glance at any size.
 */
@Composable
private fun NoAdsArt(size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE486), Color(0xFFFFB300)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(size * 0.05f)) {
            Box(Modifier.size(size * 0.42f, size * 0.17f).clip(RoundedCornerShape(size * 0.05f))
                .background(Alias.onAccent.copy(alpha = 0.85f)))
            Box(Modifier.size(size * 0.30f, size * 0.055f).clip(RoundedCornerShape(size * 0.03f))
                .background(Alias.onAccent.copy(alpha = 0.45f)))
        }
        Box(Modifier.size(size * 0.62f, size * 0.135f).rotate(-45f).clip(RoundedCornerShape(50)).background(Color.White))
        Box(Modifier.size(size * 0.62f, size * 0.075f).rotate(-45f).clip(RoundedCornerShape(50)).background(Alias.onAccent))
    }
}

/** A reward too far off to name yet. */
@Composable
private fun MysteryArt(isDeck: Boolean, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(ArenaColors.well), contentAlignment = Alignment.Center) {
        Icon(
            if (isDeck) Icons.Filled.CardGiftcard else Icons.Filled.Person,
            null, tint = ArenaColors.body.copy(alpha = 0.5f), modifier = Modifier.size(size * 0.34f),
        )
        Text("?", fontFamily = Alias.display, fontSize = (size.value * 0.36f).sp, color = ArenaColors.body)
    }
}
