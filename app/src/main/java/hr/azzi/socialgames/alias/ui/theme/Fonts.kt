@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package hr.azzi.socialgames.alias.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import hr.azzi.socialgames.alias.R

private fun w(weight: Int) = FontVariation.Settings(FontVariation.weight(weight))

/** Baloo 2 (variable) — friendly rounded display font, matches Figma headings/logo. */
val Baloo2 = FontFamily(
    Font(R.font.baloo2, FontWeight.Normal, variationSettings = w(400)),
    Font(R.font.baloo2, FontWeight.Medium, variationSettings = w(500)),
    Font(R.font.baloo2, FontWeight.SemiBold, variationSettings = w(600)),
    Font(R.font.baloo2, FontWeight.Bold, variationSettings = w(700)),
    Font(R.font.baloo2, FontWeight.ExtraBold, variationSettings = w(800)),
    Font(R.font.baloo2, FontWeight.Black, variationSettings = w(800)),
)

/** Plus Jakarta Sans (variable) — body/label font, matches Figma. */
val PlusJakarta = FontFamily(
    Font(R.font.plusjakartasans, FontWeight.Normal, variationSettings = w(400)),
    Font(R.font.plusjakartasans, FontWeight.Medium, variationSettings = w(500)),
    Font(R.font.plusjakartasans, FontWeight.SemiBold, variationSettings = w(600)),
    Font(R.font.plusjakartasans, FontWeight.Bold, variationSettings = w(700)),
    Font(R.font.plusjakartasans, FontWeight.Black, variationSettings = w(800)),
)
