package hr.azzi.socialgames.alias

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.google.gson.Gson
import hr.azzi.socialgames.alias.Models.Team
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.ui.screens.NewGameScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class NewGameActivity : BaseActivity() {

    private val boardGame: BoardGame by lazy { intent.getParcelableExtra("boardGame")!! }
    private val preferences: SharedPreferences by lazy { getSharedPreferences("teams", Context.MODE_PRIVATE) }

    private lateinit var teams: SnapshotStateList<Team>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        teams = loadTeams().toMutableStateList()
        setContent {
            AliasTheme {
                NewGameScreen(
                    teams = teams,
                    onBack = { finish() },
                    onChanged = { saveTeams() },
                    onStart = { startGame() },
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    private fun loadTeams(): List<Team> {
        val json = preferences.getString("teams", null) ?: return emptyList()
        return runCatching { Gson().fromJson(json, Array<Team>::class.java).toList() }.getOrDefault(emptyList())
    }

    private fun saveTeams() {
        preferences.edit().putString("teams", Gson().toJson(teams.toList())).apply()
    }

    private fun startGame() {
        val playingTeams = teams.filter { it.playing }
        if (playingTeams.size < 2) {
            Toast.makeText(this, getString(R.string.two_teams_needed), Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, SettingsActivity::class.java)
                .putExtra("playingTeams", ArrayList(playingTeams))
                .putExtra("boardGame", boardGame)
        )
    }
}
