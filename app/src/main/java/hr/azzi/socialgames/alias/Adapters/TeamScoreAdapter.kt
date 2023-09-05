package hr.azzi.socialgames.alias.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import hr.azzi.socialgames.alias.Models.TeamScoreItem
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.databinding.ResultItemBinding


class TeamScoreAdapter(private val context: Context,
                  private val dataSource: ArrayList<TeamScoreItem>) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = ResultItemBinding.inflate(inflater)
        val teamScoreItem = dataSource[position]
        rowView.teamTextView.text = teamScoreItem.teamName
        rowView.numberTextView.text = teamScoreItem.index.toString()
        rowView.scoreTextView.text = context.resources.getString(R.string.score) + " " + teamScoreItem.score

        return rowView.root
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