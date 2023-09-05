package hr.azzi.socialgames.alias.Views

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import hr.azzi.socialgames.alias.R
import kotlinx.android.synthetic.main.text_layout.view.*

class TipTextView @JvmOverloads constructor(context: Context): FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.text_layout, this, true)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun setLabel(text: String?) {
        infoTextView.text = text
    }

}
