package hr.azzi.socialgames.alias.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.databinding.BoardGameViewHolderBinding

interface BoardGameAdapterDelegate {
    fun didClick(position: Int)
}
interface BoardGameHolderListener {
    fun didClick(position: Int)
}
class BoardGameAdapter(private val items: List<BoardGame>) : RecyclerView.Adapter<BoardGameViewHolder>(), BoardGameHolderListener {

    var delegate: BoardGameAdapterDelegate? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardGameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.board_game_view_holder, parent, false)
        return BoardGameViewHolder(view)
    }

    override fun onBindViewHolder(holder: BoardGameViewHolder, position: Int) {
        holder.bind(items[position])
        holder.listener = this
    }

    override fun didClick(position: Int) {
        delegate?.didClick(position)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
class BoardGameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private val binding = BoardGameViewHolderBinding.bind(itemView)
    var listener: BoardGameHolderListener? = null

    init {
        itemView.setOnClickListener(this)
    }
    fun bind(item: BoardGame) {
        val context = binding.imageView.context
        binding.imageView.setImageResource(
            context.resources.getIdentifier(
                item.image,
                "drawable",
                context.packageName
            )
        )
        binding.titleTextView.text = context.getString(
            context.resources.getIdentifier(
                item.name,
                "string",
                context.packageName
            )
        )
    }

    override fun onClick(p0: View?) {
        listener?.didClick(this.adapterPosition)
    }
}
