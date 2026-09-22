package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground

private val CardFill = Color.White.copy(alpha = 0.12f)
private val CardBorder = Color.White.copy(alpha = 0.35f)
private val SectionLabel = Color.White.copy(alpha = 0.55f)
private val BadgeBlue = Color(0xFF2685F5)
private val BadgeGreen = Color(0xFF35C15E)
private val BadgeTikTok = Color(0xFF010101)

/**
 * The app's settings (Figma "16 Settings", node 3051:406).
 *
 * The gear on the home screen used to open the language picker directly. It opens this
 * instead, and the picker is one row of it — which is what makes room for the rest: the two
 * switches, the saved rounds, and the legal links.
 */
@Composable
fun AppSettingsScreen(
    language: String,
    soundEnabled: Boolean,
    vibrateEnabled: Boolean,
    versionName: String,
    versionCode: Int,
    onBack: () -> Unit,
    onLanguage: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onVibrateChanged: (Boolean) -> Unit,
    onMedia: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onShare: () -> Unit,
    onTikTok: (() -> Unit)? = null,
) {
    BrandBackground {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Text(
                    stringResource(R.string.settings),
                    color = Color.White, fontFamily = Alias.display,
                    fontWeight = FontWeight.ExtraBold, fontSize = 34.sp,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }

            Section(stringResource(R.string.settings_general)) {
                SettingsRow(
                    icon = Icons.Filled.Language, badge = BadgeBlue,
                    title = stringResource(R.string.settings_language),
                    onClick = onLanguage,
                ) {
                    Text(
                        language, color = Color.White.copy(alpha = 0.9f),
                        fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    )
                    Spacer(Modifier.size(6.dp))
                    Chevron()
                }
            }

            Section(stringResource(R.string.settings_audio)) {
                SettingsRow(
                    icon = Icons.Filled.VolumeUp, badge = BadgeGreen,
                    title = stringResource(R.string.settings_sound_effects),
                    onClick = { onSoundChanged(!soundEnabled) },
                ) {
                    BrandSwitch(soundEnabled, onSoundChanged)
                }
                RowDivider()
                SettingsRow(
                    icon = Icons.Filled.Vibration, badge = BadgeGreen,
                    title = stringResource(R.string.settings_vibrate),
                    onClick = { onVibrateChanged(!vibrateEnabled) },
                ) {
                    BrandSwitch(vibrateEnabled, onVibrateChanged)
                }
            }

            // NOTIFICATIONS
            //
            // Switching one on is a request, not a setting. The app's own preference is
            // stored either way, but it decides nothing on its own: permission belongs to
            // the system, and a switch left on over a denied permission is a promise the
            // app cannot keep.
            Section(stringResource(R.string.settings_notifications)) {
                ArenaReminderRow(
                    kind = hr.azzi.socialgames.alias.notifications.AliasNotificationKind.ARENA,
                    icon = Icons.Filled.FlashOn,
                    title = stringResource(R.string.settings_notif_arena),
                )
                RowDivider()
                ArenaReminderRow(
                    kind = hr.azzi.socialgames.alias.notifications.AliasNotificationKind.ALIAS_NIGHT,
                    icon = Icons.Filled.Groups,
                    title = stringResource(R.string.settings_notif_night),
                )
            }

            // APPEARANCE
            Section(stringResource(R.string.settings_appearance)) {
                AppearanceRow()
            }

            Section(stringResource(R.string.settings_media)) {
                SettingsRow(
                    icon = Icons.Filled.VideoLibrary, badge = BadgeBlue,
                    title = stringResource(R.string.settings_media_row),
                    onClick = onMedia,
                ) { Chevron() }
            }

            // The Balkan account only speaks to the Balkan audience, so the row is there only
            // for those UI languages — the caller decides by passing a handler, or not.
            if (onTikTok != null) {
                Section(stringResource(R.string.settings_follow_us)) {
                    SettingsRow(
                        badge = BadgeTikTok,
                        badgeContent = {
                            Image(
                                painterResource(R.drawable.ic_tiktok), null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        title = stringResource(R.string.settings_tiktok),
                        onClick = onTikTok,
                    ) { Chevron() }
                }
            }

            Section(stringResource(R.string.settings_about)) {
                SettingsRow(
                    icon = Icons.Filled.Shield, badge = BadgeBlue,
                    title = stringResource(R.string.settings_privacy), onClick = onPrivacy,
                ) { Chevron() }
                RowDivider()
                SettingsRow(
                    icon = Icons.Filled.Description, badge = BadgeBlue,
                    title = stringResource(R.string.settings_terms), onClick = onTerms,
                ) { Chevron() }
                RowDivider()
                SettingsRow(
                    icon = Icons.Filled.Share, badge = BadgeBlue,
                    title = stringResource(R.string.settings_share), onClick = onShare,
                ) { Chevron() }
            }

            Text(
                stringResource(R.string.settings_version, versionName, versionCode),
                color = Color.White.copy(alpha = 0.6f), fontFamily = Alias.body,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String, rows: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title, color = SectionLabel, fontFamily = Alias.body,
            fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.2.sp,
        )
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardFill)
                .border(1.5.dp, CardBorder, RoundedCornerShape(20.dp)),
        ) { rows() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    badge: Color,
    title: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) = SettingsRow(
    badge = badge,
    badgeContent = { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) },
    title = title,
    onClick = onClick,
    trailing = trailing,
)

@Composable
private fun SettingsRow(
    badge: Color,
    badgeContent: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }
            .padding(start = 14.dp, end = 16.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(badge),
            contentAlignment = Alignment.Center,
        ) {
            badgeContent()
        }
        Text(
            title, color = Color.White, fontFamily = Alias.body,
            fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 70.dp).height(1.dp).background(Color.White.copy(alpha = 0.16f)))
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
        tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun BrandSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Alias.success,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color.White.copy(alpha = 0.25f),
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}


/**
 * One reminder, with the switch that turns it on.
 *
 * The preference is stored whatever the system says, so a player who grants permission
 * later gets what they already chose rather than the default.
 */
@Composable
private fun ArenaReminderRow(
    kind: hr.azzi.socialgames.alias.notifications.AliasNotificationKind,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
) {
    val context = LocalContext.current
    val notif = hr.azzi.socialgames.alias.notifications.AliasNotifications
    var on by remember { mutableStateOf(notif.isEnabled(context, kind)) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Denied: the switch goes back, because it cannot do anything, and the only place
        // the answer can still be changed is the system settings.
        if (!granted) {
            on = false
            notif.setEnabled(context, kind, false)
        }
    }

    val set: (Boolean) -> Unit = { want ->
        on = want
        notif.setEnabled(context, kind, want)
        if (want && !notif.isAllowed(context) &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            notif.markAsked(context)
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    SettingsRow(icon = icon, badge = BadgeGreen, title = title, onClick = { set(!on) }) {
        BrandSwitch(on, set)
    }
}

/**
 * System / Light / Dark.
 *
 * A segmented row rather than three rows: they are one choice, and three switches would
 * let somebody pick none of them.
 */
@Composable
private fun AppearanceRow() {
    val context = LocalContext.current
    val state = hr.azzi.socialgames.alias.arena.AppAppearance.load(context)

    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        hr.azzi.socialgames.alias.arena.AppAppearance.entries.forEach { option ->
            val on = state.value == option
            Text(
                stringResource(
                    when (option) {
                        hr.azzi.socialgames.alias.arena.AppAppearance.SYSTEM -> R.string.settings_appearance_system
                        hr.azzi.socialgames.alias.arena.AppAppearance.LIGHT -> R.string.settings_appearance_light
                        hr.azzi.socialgames.alias.arena.AppAppearance.DARK -> R.string.settings_appearance_dark
                    }
                ),
                color = if (on) Alias.onAccent else Color.White,
                fontFamily = Alias.body,
                fontWeight = if (on) FontWeight.Black else FontWeight.SemiBold,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) Alias.accent else Color.White.copy(alpha = 0.12f))
                    .clickable {
                        hr.azzi.socialgames.alias.arena.AppAppearance.set(context, option)
                    }
                    .padding(vertical = 10.dp),
            )
        }
    }
}
