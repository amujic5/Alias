package hr.azzi.socialgames.alias.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import hr.azzi.socialgames.alias.Models.Team
import hr.azzi.socialgames.alias.R
import kotlinx.android.synthetic.main.team_item.view.*


class TeamAdapter(private val context: Context,
                  private val dataSource: ArrayList<Team>) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.team_item, parent, false)
        val team = dataSource[position]
        rowView.teamTextView.text = team.teamName
        rowView.player1TextView.text = team.firstPlayer
        rowView.player2TextView.text = team.secondPlayer

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