package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import hr.azzi.socialgames.alias.Models.DictionaryModel
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.Team
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.Service.DictionaryService
import hr.azzi.socialgames.alias.Service.JSONService
import hr.azzi.socialgames.alias.ui.screens.SettingsScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class SettingsActivity : BaseActivity() {

    private val boardGame: BoardGame by lazy { intent.getParcelableExtra("boardGame")!! }
    private val teams: ArrayList<Team> by lazy {
        intent.getParcelableArrayListExtra<Team>("playingTeams") ?: arrayListOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dictionaries = JSONService.getDictionaries(this, boardGame)
        setContent {
            AliasTheme {
                SettingsScreen(
                    teams = teams,
                    dictionaries = dictionaries,
                    onBack = { finish() },
                    onPlay = { index, time, score -> play(dictionaries, index, time, score) },
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    private fun play(dictionaries: List<DictionaryModel>, index: Int, time: Int, score: Int) {
        val dictionary = dictionaries.getOrNull(index) ?: dictionaries.firstOrNull() ?: return
        DictionaryService.playingDictionary = dictionary
        val game = Game(false, time, score, teams, ArrayList(dictionary.words), 0)
        startActivity(Intent(this, PlayActivity::class.java).putExtra("game", game))
    }
}
