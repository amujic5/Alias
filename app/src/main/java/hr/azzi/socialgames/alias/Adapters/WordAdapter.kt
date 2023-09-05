package hr.azzi.socialgames.alias.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.databinding.EditAnswerItemBinding

interface WordAdapterDelegate {
    fun didChangeSwitchValue(isChecked: Boolean, position: Int)
}

class WordAdapter(private val context: Context,
                       private val dataSource: ArrayList<MarkedWord>) : BaseAdapter() {

    var delegate: WordAdapterDelegate? = null
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = EditAnswerItemBinding.inflate(inflater)
        val markedWord = dataSource[position]

        rowView.wordTextView.text = markedWord.word
        rowView.switch1.isChecked = markedWord.isCorrect

        if (markedWord.isCorrect) {
            rowView.wordTextView.setTextColor(Color.parseColor("#000000"))
        } else {
            rowView.wordTextView.setTextColor(Color.parseColor("#FF0000"))
        }

        rowView.switch1.setOnCheckedChangeListener { buttonView, isChecked ->
            this.delegate?.didChangeSwitchValue(isChecked, position)

            if (isChecked) {
                rowView.wordTextView.setTextColor(Color.parseColor("#000000"))
            } else {
                rowView.wordTextView.setTextColor(Color.parseColor("#FF0000"))
            }

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