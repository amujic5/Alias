package hr.azzi.socialgames.alias

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import hr.azzi.socialgames.alias.Adapters.TeamAdapter
import hr.azzi.socialgames.alias.Models.Team
import kotlinx.android.synthetic.main.activity_new_game.*
import kotlinx.android.synthetic.main.add_team_footer.view.*
import kotlinx.android.synthetic.main.dialog_new_category.view.*
import com.google.gson.Gson

class NewGameActivity : AppCompatActivity() {

    val footerView by lazy {
        layoutInflater.inflate(R.layout.add_team_footer, null, false)
    }

    var teamDataSource = ArrayList<Team>()

    val adapter by lazy {
        TeamAdapter(this, teamDataSource)
    }

    val preferences: SharedPreferences by lazy {
        this.getSharedPreferences("teams", Context.MODE_PRIVATE)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_game)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        listView.adapter = adapter
        listView.addFooterView(footerView)

        loadTeams()

        updateVsTextView()

        observe()
    }

    fun loadTeams() {
        val gson = Gson()
        val json = preferences.getString("teams", null)
        if (json != null) {
            val obj: List<Team> = gson.fromJson(json, Array<Team>::class.java).toList()
            for (team in obj) {
                teamDataSource.add(team)
            }
            adapter.notifyDataSetChanged()
        }
    }

    fun saveTeams() {
        val gson = Gson()
        val json = gson.toJson(teamDataSource)
        preferences.edit().putString("teams", json).apply()
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
                Toast.makeText(this, resources.getString(R.string.two_teams_needed), Toast.LENGTH_SHORT).show()
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
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

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
                val newTeam = Team(view.player1EditText.text.toString(), view.player2EditText.text.toString(), view.teamEditText.text.toString())
                teamDataSource.add(newTeam)
                adapter.notifyDataSetChanged()
            }
            updateVsTextView()
            saveTeams()

            dialog.dismiss()
        }

        view.deleteButton.setOnClickListener {
            team?.also {
                teamDataSource.remove(it)
                adapter.notifyDataSetChanged()
                updateVsTextView()
                saveTeams()
            }

            dialog.dismiss()
        }

    }

    fun updateVsTextView() {
        vsTextView.setText(teamDataSource.filter { it.playing }.map { it.teamName }.joinToString(" VS "))
    }

}