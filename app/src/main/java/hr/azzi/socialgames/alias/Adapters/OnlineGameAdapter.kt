package hr.azzi.socialgames.alias.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
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
        rowView.statusTextView.text = "Status: " + onlineGame.status
        rowView.playersCountTextView.text = "Number of players: ${onlineGame.user.size}"

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