package hr.azzi.socialgames.alias.Adapters

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import hr.azzi.socialgames.alias.Models.OnlineGame
import hr.azzi.socialgames.alias.R
import kotlinx.android.synthetic.main.online_game_item.view.*


class OnlineGameAdapter(private val context: Context,
                  private val dataSource: ArrayList<OnlineGame>) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.online_game_item, parent, false)
        val onlineGame = dataSource[position]

        rowView.languageCodeTextView.text = onlineGame.dictionary
        rowView.playersCountTextView.text = "Number of players: ${onlineGame.user.size}"

        if (onlineGame.isWaiting()) {
            val s = SpannableString("Status: starting soon...")
            s.setSpan(
                ForegroundColorSpan(Color.RED), 7, s.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            rowView.statusTextView.setText(s, TextView.BufferType.SPANNABLE)
        }



        return rowView
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.size
    }
}