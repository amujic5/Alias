package hr.azzi.socialgames.alias.Online

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import hr.azzi.socialgames.alias.Models.UserLeader
import hr.azzi.socialgames.alias.R
import kotlinx.android.synthetic.main.result_item.view.*

class LeaderboardAdapter(private val context: Context,
                        private val dataSource: ArrayList<UserLeader>) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.leader_item, parent, false)
        val userResult = dataSource[position]
        rowView.teamTextView.text = userResult.username
        rowView.numberTextView.text = userResult.index.toString()
        rowView.scoreTextView.text = context.resources.getString(R.string.score) + " " + userResult.totalScore.toString()

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
