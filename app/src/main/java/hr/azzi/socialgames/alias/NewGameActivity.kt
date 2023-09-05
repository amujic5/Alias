package hr.azzi.socialgames.alias

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import hr.azzi.socialgames.alias.Adapters.TeamAdapter
import hr.azzi.socialgames.alias.Models.Team
import hr.azzi.socialgames.alias.databinding.ActivityNewGameBinding
import hr.azzi.socialgames.alias.databinding.AddTeamFooterBinding
import hr.azzi.socialgames.alias.databinding.DialogNewCategoryBinding

class NewGameActivity : AppCompatActivity() {

    private lateinit var binding : ActivityNewGameBinding

    val footerView by lazy {
        AddTeamFooterBinding.inflate(layoutInflater)
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
        binding = ActivityNewGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        binding.listView.adapter = adapter
        binding.listView.addFooterView(footerView.root)

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
        binding.backButton.setOnClickListener {
            this.finish()
        }

        footerView.createTeamButton.setOnClickListener {
            this.editTeam()
        }

        binding.listView.setOnItemClickListener { parent, view, position, id ->
            val team = teamDataSource[position]
            editTeam(team)
        }

        binding.startGameButton.setOnClickListener {
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


        val dialogNewCategoryBinding = DialogNewCategoryBinding.inflate(layoutInflater)
        builder.setView(dialogNewCategoryBinding.root)

        team?.also {
            dialogNewCategoryBinding.teamEditText.setText(it.teamName)
            dialogNewCategoryBinding.player1EditText.setText(it.firstPlayer)
            dialogNewCategoryBinding.player2EditText.setText(it.secondPlayer)
        } ?: kotlin.run {
            dialogNewCategoryBinding.deleteContainer.visibility = View.GONE
        }

        val dialog = builder.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogNewCategoryBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialogNewCategoryBinding.createButton.setOnClickListener {

            team?.also {
                it.teamName = dialogNewCategoryBinding.teamEditText.text.toString()
                it.firstPlayer = dialogNewCategoryBinding.player1EditText.text.toString()
                it.secondPlayer = dialogNewCategoryBinding.player2EditText.text.toString()
                adapter.notifyDataSetChanged()
            } ?: kotlin.run {
                val newTeam = Team(dialogNewCategoryBinding.player1EditText.text.toString(), dialogNewCategoryBinding.player2EditText.text.toString(), dialogNewCategoryBinding.teamEditText.text.toString())
                teamDataSource.add(newTeam)
                adapter.notifyDataSetChanged()
            }
            updateVsTextView()
            saveTeams()

            dialog.dismiss()
        }

        dialogNewCategoryBinding.deleteButton.setOnClickListener {
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
        binding.vsTextView.setText(teamDataSource.filter { it.playing }.map { it.teamName }.joinToString(" VS "))
    }

}