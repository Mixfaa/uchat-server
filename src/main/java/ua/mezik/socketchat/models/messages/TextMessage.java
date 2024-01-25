package ua.mezik.socketchat.models.messages;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ua.mezik.socketchat.models.Account;
import ua.mezik.socketchat.models.Chat;
import ua.mezik.socketchat.models.ChatMessage;
import ua.mezik.socketchat.models.MessageType;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
public class TextMessage extends ChatMessage {
    private String text;
    private boolean edited = false;

    public TextMessage(Account owner, Chat chat, MessageType type, String text) {
        super(owner, chat, type);
        this.text = text;
    }
}
