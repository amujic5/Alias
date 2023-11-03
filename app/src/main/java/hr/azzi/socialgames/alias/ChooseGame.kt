package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import hr.azzi.socialgames.alias.Adapters.BoardGameAdapter
import hr.azzi.socialgames.alias.Adapters.BoardGameAdapterDelegate
import hr.azzi.socialgames.alias.Service.JSONService
import hr.azzi.socialgames.alias.databinding.ActivityChooseGameBinding

class ChooseGame : AppCompatActivity(), BoardGameAdapterDelegate {

    private lateinit var binding : ActivityChooseGameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_game)

        binding = ActivityChooseGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        val adapter = BoardGameAdapter(JSONService.boardGames)
        adapter.delegate = this
        binding.recyclerView.adapter = adapter
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.backButton.setOnClickListener {
            this.finish()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    override fun didClick(position: Int) {
        val boardGame = JSONService.boardGames[position]

        val intent = Intent(this, NewGameActivity::class.java)
        intent.putExtra("boardGame", boardGame)
        startActivity(intent)
    }
}