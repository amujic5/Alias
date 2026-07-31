package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
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
        setComposeContent {
            AliasTheme {
                SettingsScreen(
                    teams = teams,
                    dictionaries = dictionaries,
                    boardGame = boardGame,
                    onBack = { finish() },
                    onPlay = { index, time, score, captureMoment ->
                        play(dictionaries, index, time, score, captureMoment)
                    },
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    private fun play(
        dictionaries: List<DictionaryModel>,
        index: Int,
        time: Int,
        score: Int,
        captureMoment: Boolean,
    ) {
        val dictionary = dictionaries.getOrNull(index) ?: dictionaries.firstOrNull() ?: return
        DictionaryService.playingDictionary = dictionary
        val game = Game(false, time, score, teams, ArrayList(dictionary.words), 0)
        startActivity(
            Intent(this, PlayActivity::class.java)
                .putExtra("game", game)
                // Nothing reads this yet — the dual-camera recorder is not built. It is the
                // handover point the round will pick the choice up from.
                .putExtra("captureMoment", captureMoment)
        )
    }
}
