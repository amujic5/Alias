package hr.azzi.socialgames.alias

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.loadingview.LoadingDialog
import hr.azzi.socialgames.alias.Adapters.UserResultAdapter
import hr.azzi.socialgames.alias.Models.OnlineGame
import hr.azzi.socialgames.alias.Models.UserResult
import hr.azzi.socialgames.alias.Online.Models.UserManagerModel
import hr.azzi.socialgames.alias.Service.UserResultService
import kotlinx.android.synthetic.main.activity_online_result.*

class OnlineResultActivity : AppCompatActivity() {

    lateinit var game: OnlineGame

    val gameId: String by lazy {
        intent.getStringExtra("gameId")
    }
    val dialog: LoadingDialog by lazy {
        LoadingDialog.get(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_online_result)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        game = intent.getParcelableExtra("game") as OnlineGame

        loadData()
        observe()

    }

    fun observe() {
        closeButton2.setOnClickListener {
            finish()
        }
    }


    fun loadData() {
        dialog.show()
        UserResultService
            .instance
            .getUserResults(gameId) { userResult, adminUserResult ->

                dialog.hide()

                val index = userResult.size + 1

                for (user in game.user) {
                    if (user == adminUserResult.username) {
                        continue
                    }

                    try {
                        val userResult = userResult.first {
                            it.username == user
                        }
                    } catch (e: NoSuchElementException) {
                        val userR = UserResult(user, 0, index, user == UserManagerModel.username())
                        userResult.add(userR)
                    }
                }

                val adapter = UserResultAdapter(this, userResult)
                listView.adapter = adapter

                playerCountTextView.text = game.user.size.toString()
                explainerTextView.text = "Explainer: ${adminUserResult.username}"
                explainerScoreTextView.text = "${adminUserResult.score}"

                var userMe = userResult.find {
                    it.isMe
                }
                if (userMe == null) {
                    userMe = adminUserResult
                }
                userMe.let {
                    teamTextView.text = it.username
                    scoreTextView.text = "Score: ${it.score}"
                    numberTextView.text = it.index.toString()
                }

            }
    }

    companion object {
        fun createIntent(context: Context, gameId: String, onlineGame: OnlineGame) = Intent(context, OnlineResultActivity::class.java).apply {
            putExtra("game", onlineGame)
            putExtra("gameId", gameId)
        }
    }
}
