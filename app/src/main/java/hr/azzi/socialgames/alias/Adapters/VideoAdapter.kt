package hr.azzi.socialgames.alias.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import hr.azzi.socialgames.alias.Models.Video
import hr.azzi.socialgames.alias.R
import kotlinx.android.synthetic.main.video_item.view.*

interface VideoAdapterDelegate {
    fun didTapShare(position: Int)
    fun didTapPlay(position: Int)
}

class VideoAdapter(private val context: Context,
                  private val dataSource: ArrayList<Video>) : BaseAdapter() {

    var delegate: VideoAdapterDelegate? = null
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.video_item, parent, false)
        val video = dataSource[position]
        rowView.videoTextView.text = video.name

        rowView.playButton.setOnClickListener {
            this.delegate?.didTapPlay(position)
        }

        rowView.shareButton.setOnClickListener {
            this.delegate?.didTapShare(position)
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