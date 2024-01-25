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
public class FileMessage extends ChatMessage {
    private String link; // oh fuck you

    public FileMessage(Account owner, Chat chat, MessageType type, String link) {
        super(owner, chat, type);
        this.link = link;
    }
}
