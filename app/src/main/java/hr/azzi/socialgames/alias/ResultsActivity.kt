package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.ui.screens.ResultsScreen
import hr.azzi.socialgames.alias.ui.screens.ResultsUiState
import hr.azzi.socialgames.alias.ui.screens.TeamRow
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class ResultsActivity : BaseActivity() {

    private val game: Game by lazy { intent.getParcelableExtra<Game>("game") as Game }

    private var uiState by mutableStateOf<ResultsUiState?>(null)
    private var shouldShowEditAnswers = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game.currentTeamHasFinishedTheRound()
        uiState = buildState()
        setContent {
            AliasTheme {
                uiState?.let {
                    ResultsScreen(
                        state = it,
                        onEditAnswers = { launchEditAnswers() },
                        onStart = { startNextRound() },
                        onFinish = { showWinner() },
                    )
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()
        if (shouldShowEditAnswers) {
            shouldShowEditAnswers = false
            launchEditAnswers()
        }
    }

    private fun buildState(): ResultsUiState {
        val rows = game.sortedTeams.mapIndexed { index, team ->
            TeamRow(index + 1, team.teamName, team.getScore(), index == 0 && game.nextTeam == null)
        }
        return ResultsUiState(
            teamName = game.currentTeam.teamName,
            correct = game.currentCorrectAnswers,
            skip = game.currentSkipAnswers,
            rows = rows,
            nextTeamName = game.nextTeam?.teamName,
            explaining = game.nextExplainingPlayerName,
            answering = game.nextAnsweringPlayerName,
        )
    }

    private fun launchEditAnswers() {
        val intent = Intent(this, EditAnswersActivity::class.java)
        intent.putExtra("game", game)
        startActivityForResult(intent, 1)
    }

    private fun startNextRound() {
        game.newRound()
        val intent = Intent(this, PlayActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("game", game)
        startActivity(intent)
        finish()
    }

    private fun showWinner() {
        val intent = Intent(this, WinnerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("game", game)
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val words = data?.getParcelableArrayListExtra<MarkedWord>("words")
        if (words != null) {
            game.setTeamMarkedWords(words)
            uiState = buildState()
        }
    }
}
