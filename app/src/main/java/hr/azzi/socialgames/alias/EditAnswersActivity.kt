package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.toMutableStateList
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.ui.screens.EditAnswersScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class EditAnswersActivity : BaseActivity() {

    private val game: Game by lazy { intent.getParcelableExtra<Game>("game") as Game }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val words = game.currentTeamMarkedWords
            .map { MarkedWord(it.word, it.isCorrect) }
            .toMutableStateList()

        setContent {
            AliasTheme {
                EditAnswersScreen(
                    teamName = game.currentTeam.teamName,
                    words = words,
                    onToggle = { i, checked -> words[i] = MarkedWord(words[i].word, checked) },
                    onCancel = { finish() },
                    onSave = {
                        val intent = Intent()
                        intent.putExtra("words", ArrayList(words))
                        setResult(1, intent)
                        finish()
                    },
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }
}
