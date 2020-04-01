package hr.azzi.socialgames.alias

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import hr.azzi.socialgames.alias.Adapters.UserResultAdapter
import hr.azzi.socialgames.alias.Service.UserResultService
import kotlinx.android.synthetic.main.activity_online_result.*

class OnlineResultActivity : AppCompatActivity() {

    val gameId: String by lazy {
        intent.getStringExtra("gameId")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_online_result)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        loadData()
        observe()
    }

    fun observe() {
        closeButton2.setOnClickListener {
            finish()
        }
    }


    fun loadData() {
        UserResultService
            .instance
            .getUserResults(gameId) { userResult, adminUserResult ->

                val adapter = UserResultAdapter(this, userResult)
                listView.adapter = adapter

                playerCountTextView.text = userResult.size.toString()
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
        fun createIntent(context: Context, gameId: String) = Intent(context, OnlineResultActivity::class.java).apply {
            putExtra("gameId", gameId)
        }
    }
}
