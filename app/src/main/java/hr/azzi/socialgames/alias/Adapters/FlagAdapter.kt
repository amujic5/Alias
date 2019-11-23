package hr.azzi.socialgames.alias.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import hr.azzi.socialgames.alias.Models.FlagModel
import hr.azzi.socialgames.alias.R
import kotlinx.android.synthetic.main.flag.view.*


interface FlagAdapterDelegate {
    fun didClick(position: Int)
}
interface FlagHolderListener {
    fun didClick(position: Int)
}

class FlagAdapter(private val flags: ArrayList<FlagModel>): RecyclerView.Adapter<FlagAdapter.FlagHolder>(), FlagHolderListener{

    var delegate: FlagAdapterDelegate? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlagHolder {
        val inflatedView = LayoutInflater.from(parent.context).inflate(R.layout.flag, parent, false)
        return FlagHolder(inflatedView)
    }

    override fun getItemCount(): Int = flags.size

    override fun onBindViewHolder(holder: FlagHolder, position: Int) {
        val flagModel = flags[position]
        holder.updateUI(flagModel)
        holder.listener = this
    }

    override fun didClick(position: Int) {
        delegate?.didClick(position)
    }

    //1
    class FlagHolder(private val view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        var listener: FlagHolderListener? = null

        //3
        init {
            view.setOnClickListener(this)
        }

        fun updateUI(flagModel: FlagModel) {
            if (flagModel.selected) {
                view.alpha = 1F
            } else {
                view.alpha = 0.5F
            }
            Picasso.get()
                .load(flagModel.url)
                .into(view.flagImageView)

            view.textView.text = flagModel.code

        }

        override fun onClick(v: View) {
            listener?.didClick(this.adapterPosition)
        }

    }

}