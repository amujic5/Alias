package hr.azzi.socialgames.alias.Online.Adapters

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import com.stfalcon.chatkit.messages.MessagesListAdapter
import hr.azzi.socialgames.alias.Online.Models.Message
import hr.azzi.socialgames.alias.Online.Models.MessageType

class OutMessageViewHolder(itemView: View) :
    MessagesListAdapter.OutcomingMessageViewHolder<Message>(itemView) {

    override fun onBind(message: Message) {
        super.onBind(message)

        text.setTextColor(Color.BLACK)

        val string = message.author.name + ": " + message.messageText
        val boldSpan = StyleSpan(Typeface.BOLD)
        val spannable = SpannableString(string)
        spannable.setSpan(boldSpan, 0, message.author.name.length + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(Color.BLACK), 0, message.author.name.length + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        when(message.messageType) {
            MessageType.WRONG -> {
                time.setTextColor(Color.RED)
                time.text = "\u0078"
            }
            MessageType.CORRECT -> {
                time.text = "\u2713"
                time.setTextColor(Color.GREEN)
            }
            MessageType.EXPLAIN -> {
                time.text = "\u2824"
                time.setTextColor(Color.BLACK)
                spannable.setSpan(
                    BackgroundColorSpan(Color.YELLOW), message.author.name.length + 2 , string.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        text.setText(spannable, TextView.BufferType.SPANNABLE)

    }
}