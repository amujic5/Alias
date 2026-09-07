package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias

private val BadgeTint = Color(0xFFEAF1FF)
private val CloudBlue = Color(0xFF1F74E0)
private val SendBlue = Color(0xFF2F7BF0)
private val BodyInk = Color(0xFF46587A)
private val NotNowBg = Color(0xFFEEF2F8)

private const val TERMS_URL = "https://aliaswords.com/terms"
private const val PRIVACY_URL = "https://aliaswords.com/privacy"

/**
 * Asks before a replay leaves the device — Figma "09.e Send to Alias (modal)".
 *
 * This dialog is the consent, not a formality: the video is two people's faces, and until
 * Send Replay is tapped nothing is uploaded. The wording says plainly where it goes and
 * what it might be used for, and dismissing it is the same answer as "Not now".
 */
@Composable
fun SendToAliasDialog(onApprove: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Alias.surface)
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(BadgeTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CloudUpload, null, tint = CloudBlue, modifier = Modifier.size(46.dp))
                Icon(
                    Icons.Filled.Star, null, tint = Alias.accent,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 14.dp).size(20.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.send_alias_title),
                color = Alias.textPrimary, fontFamily = Alias.body,
                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.send_alias_body),
                color = BodyInk, fontFamily = Alias.body, fontWeight = FontWeight.Medium,
                fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.send_alias_note),
                color = Alias.textSecondary, fontFamily = Alias.body,
                fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Alias.divider))
            Spacer(Modifier.height(14.dp))
            Text(
                legalNotice(),
                color = Alias.textSecondary, fontFamily = Alias.body,
                fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DialogButton(
                    text = stringResource(R.string.send_alias_not_now),
                    background = NotNowBg, ink = Alias.textPrimary,
                    modifier = Modifier.weight(0.42f), onClick = onDismiss,
                )
                DialogButton(
                    text = stringResource(R.string.send_alias_send),
                    background = SendBlue, ink = Color.White, icon = true,
                    modifier = Modifier.weight(0.58f), onClick = onApprove,
                )
            }
        }
    }
}

/**
 * "By sending, you agree to our Terms of Use and acknowledge our Privacy Policy."
 *
 * The two link labels are their own resources and the sentence is a template with
 * positional placeholders, so a translation is free to reorder them — and the ranges are
 * found by locating the labels this code just inserted, never by searching for the English
 * words. Several languages also need the labels declined (Croatian "naše Uvjete
 * korištenja", not "naše Uvjeti korištenja"), which is why the label resources carry the
 * case the sentence needs rather than a bare title.
 */
@Composable
private fun legalNotice(): AnnotatedString {
    val terms = stringResource(R.string.send_alias_legal_terms)
    val privacy = stringResource(R.string.send_alias_legal_privacy)
    val sentence = stringResource(R.string.send_alias_legal, terms, privacy)

    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = SendBlue, textDecoration = TextDecoration.Underline),
    )

    // Ordered by where they actually landed, so the walk below stays in step with the
    // sentence whatever order the translation put them in.
    val links = listOf(terms to TERMS_URL, privacy to PRIVACY_URL)
        .mapNotNull { (label, url) ->
            val at = sentence.indexOf(label)
            if (at < 0) null else Triple(at, label, url)
        }
        .sortedBy { it.first }

    return buildAnnotatedString {
        var cursor = 0
        links.forEach { (at, label, url) ->
            // A label that overlaps one already consumed would double-append; skip it and
            // let it render as plain text rather than corrupt the sentence.
            if (at < cursor) return@forEach
            append(sentence.substring(cursor, at))
            withLink(LinkAnnotation.Url(url, linkStyles)) { append(label) }
            cursor = at + label.length
        }
        append(sentence.substring(cursor))
    }
}

@Composable
private fun DialogButton(
    text: String,
    background: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    icon: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon) {
            Icon(Icons.Filled.CloudUpload, null, tint = ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text, color = ink, fontFamily = Alias.body,
            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
            maxLines = 1, textAlign = TextAlign.Center,
        )
    }
}
