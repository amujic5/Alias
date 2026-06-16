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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Models.DictionaryModel
import hr.azzi.socialgames.alias.Models.Team
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.Overline
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun SettingsScreen(
    teams: List<Team>,
    dictionaries: List<DictionaryModel>,
    onBack: () -> Unit,
    onPlay: (dictionaryIndex: Int, time: Int, score: Int) -> Unit,
) {
    var time by remember { mutableIntStateOf(60) }    // range 20..80
    var score by remember { mutableIntStateOf(100) }  // range 40..120
    var selected by remember { mutableIntStateOf(0) }

    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                DisplayText(stringResource(R.string.settings).uppercase(), 22)
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (teams.isNotEmpty()) VsPill(teams.filter { it.playing }.map { it.teamName })

                SettingSlider(stringResource(R.string.score_settings).uppercase(), score, 40, 120) { score = it }
                SettingSlider(stringResource(R.string.time_limit_seconds).uppercase(), time, 20, 80) { time = it }

                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Overline("DICTIONARY")
                        Text(dictionaries.getOrNull(selected)?.language ?: "",
                            color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    dictionaries.chunked(5).forEach { rowItems ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { dict ->
                                val idx = dictionaries.indexOf(dict)
                                Flag(dict.imageURLString, selected = idx == selected) { selected = idx }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                PillButton(stringResource(R.string.play), PillKind.Primary) { onPlay(selected, time, score) }
            }
        }
    }
}

@Composable
private fun VsPill(names: List<String>) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(names.joinToString("   VS   "), color = Color.White, fontFamily = Alias.display,
            fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun SettingSlider(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Overline(label)
            DisplayText("$value", 24, color = Alias.accent)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Alias.accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$min", color = Color.White.copy(alpha = 0.7f), fontFamily = Alias.body, fontSize = 12.sp)
            Text("$max", color = Color.White.copy(alpha = 0.7f), fontFamily = Alias.body, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Flag(url: String?, selected: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val id = remember(url) { if (url != null) ctx.resources.getIdentifier(url, "drawable", ctx.packageName) else 0 }
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(width = 54.dp, height = 38.dp).clip(shape)
            .border(if (selected) 3.dp else 0.dp, if (selected) Alias.accent else Color.Transparent, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (id != 0) Image(painterResource(id), null, Modifier.fillMaxSize().clip(shape), contentScale = ContentScale.Crop)
    }
}
