package hr.azzi.socialgames.alias.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import hr.azzi.socialgames.alias.Models.Team
import hr.azzi.socialgames.alias.Models.TeamScoreItem
import hr.azzi.socialgames.alias.R
import kotlinx.android.synthetic.main.result_item.view.*
import kotlinx.android.synthetic.main.team_item.view.*
import kotlinx.android.synthetic.main.team_item.view.teamTextView


class TeamScoreAdapter(private val context: Context,
                  private val dataSource: ArrayList<TeamScoreItem>) : BaseAdapter() {

    var delegate: TeamAdapterDelegate? = null
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.result_item, parent, false)
        val teamScoreItem = dataSource[position]
        rowView.teamTextView.text = teamScoreItem.teamName
        rowView.numberTextView.text = teamScoreItem.index.toString()
        rowView.scoreTextView.text = "Score: " + teamScoreItem.score

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