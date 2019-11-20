package hr.azzi.socialgames.alias

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import hr.azzi.socialgames.alias.Adapters.TeamAdapter
import hr.azzi.socialgames.alias.Adapters.TeamAdapterDelegate
import hr.azzi.socialgames.alias.Models.Dictionary
import hr.azzi.socialgames.alias.Models.Team
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_new_game.*
import kotlinx.android.synthetic.main.add_team_footer.view.*
import kotlinx.android.synthetic.main.dialog_new_category.view.*
import kotlinx.android.synthetic.main.team_item.view.*

class NewGameActivity : AppCompatActivity(), TeamAdapterDelegate {

    val footerView by lazy {
        layoutInflater.inflate(R.layout.add_team_footer, null, false)
    }

    val teamDataSource = ArrayList<Team>()

    val adapter by lazy {
        TeamAdapter(this, teamDataSource)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_game)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()


        teamDataSource.add(Team("first","second","team"))
        teamDataSource.add(Team("first 2","second 2","team 2"))

        adapter.delegate = this
        listView.adapter = adapter
        listView.addFooterView(footerView)

        updateVsTextView()

        observe()
    }

    fun observe() {
        backButton.setOnClickListener {
            this.finish()
        }

        footerView.createTeamButton.setOnClickListener {
            this.editTeam()
        }

        listView.setOnItemClickListener { parent, view, position, id ->
            val team = teamDataSource[position]
            editTeam(team)
        }

        startGameButton.setOnClickListener {
            if (teamDataSource.filter { it.playing }.size < 2) {
                Toast.makeText(this, "At least 2 players needed", Toast.LENGTH_SHORT).show()
            } else {
                val intent =  Intent(this, SettingsActivity::class.java)
                val playingTeams = teamDataSource.filter {
                    it.playing
                }
                intent.putExtra("playingTeams", ArrayList(playingTeams))

                startActivity(intent)
            }
        }
    }

    fun editTeam(team: Team? = null) {
        val context = this
        val builder = AlertDialog.Builder(context)


        val view = layoutInflater.inflate(R.layout.dialog_new_category, null)
        builder.setView(view)

        team?.also {
            view.teamEditText.setText(it.teamName)
            view.player1EditText.setText(it.firstPlayer)
            view.player2EditText.setText(it.secondPlayer)
        } ?: kotlin.run {
            view.deleteContainer.visibility = View.GONE
        }

        val dialog = builder.show()

        view.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        view.createButton.setOnClickListener {

            team?.also {
                it.teamName = view.teamEditText.text.toString()
                it.firstPlayer = view.player1EditText.text.toString()
                it.secondPlayer = view.player2EditText.text.toString()
                adapter.notifyDataSetChanged()
            } ?: kotlin.run {
                val newTeam = Team(view.teamEditText.text.toString(), view.player1EditText.text.toString(), view.player2EditText.text.toString())
                teamDataSource.add(newTeam)
                adapter.notifyDataSetChanged()
            }

            updateVsTextView()
            dialog.dismiss()
        }

        view.deleteButton.setOnClickListener {
            team?.also {
                teamDataSource.remove(it)
                adapter.notifyDataSetChanged()
                updateVsTextView()
            }

            dialog.dismiss()
        }

    }

    fun updateVsTextView() {
        vsTextView.setText(teamDataSource.filter { it.playing }.map { it.teamName }.joinToString(" VS "))
    }

    // TeamAdapterDelegate

    override fun didChangeCheckboxValue(isChecked: Boolean, position: Int) {
        teamDataSource[position].playing = isChecked
        updateVsTextView()
    }
}