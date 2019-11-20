package hr.azzi.socialgames.alias

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_how_to_play.*

class HowToPlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_how_to_play)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        gotItButton.setOnClickListener {
            this.finish()
        }
    }
}
