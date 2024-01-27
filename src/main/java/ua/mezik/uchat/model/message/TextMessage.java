package ua.mezik.uchat.model.message;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ua.mezik.uchat.model.Account;
import ua.mezik.uchat.model.Chat;
import ua.mezik.uchat.model.ChatMessage;
import ua.mezik.uchat.model.MessageType;

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
