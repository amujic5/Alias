package hr.azzi.socialgames.alias.Views

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import hr.azzi.socialgames.alias.databinding.TextLayoutBinding

class TipTextView @JvmOverloads constructor(context: Context): FrameLayout(context) {

    private val binding = TextLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun setLabel(text: String?) {
        binding.infoTextView.text = text
    }

}
