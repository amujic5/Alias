package hr.azzi.socialgames.alias.Online.Models

import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.IUser
import java.util.*

class Message(var messageId: String, var date: Date, var author: IUser, var messageText: String, var messageType: MessageType):
    IMessage {

    override fun getId(): String {
        return messageId
    }

    override fun getCreatedAt(): Date {
        return date
    }

    override fun getUser(): IUser {
        return author
    }

    override fun getText(): String {
        return messageText
    }

}