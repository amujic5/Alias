package hr.azzi.socialgames.alias

import android.os.Bundle
import hr.azzi.socialgames.alias.databinding.ActivityHowToPlayBinding

class HowToPlayActivity : BaseActivity() {

    private lateinit var binding : ActivityHowToPlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHowToPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        binding.gotItButton.setOnClickListener {
            this.finish()
        }

    }
}
