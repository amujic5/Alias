package hr.azzi.socialgames.alias.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat
import hr.azzi.socialgames.alias.Models.UserResult
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.databinding.ResultItemBinding

class UserResultAdapter(private val context: Context,
                       private val dataSource: ArrayList<UserResult>) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = ResultItemBinding.inflate(inflater)
        val userResult = dataSource[position]
        rowView.teamTextView.text = userResult.username
        rowView.numberTextView.text = userResult.index.toString()
        rowView.scoreTextView.text = context.resources.getString(R.string.score) + " " + userResult.score
        rowView.root.setBackgroundColor(Color.WHITE)

        if (userResult.isMe) {
            rowView.root.setBackgroundColor(ContextCompat.getColor(context, R.color.alias_green))
        } else {
            rowView.root.setBackgroundColor(ContextCompat.getColor(context, R.color.alias_blue))
        }

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