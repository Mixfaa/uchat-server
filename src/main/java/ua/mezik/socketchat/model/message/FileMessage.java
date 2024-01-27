package ua.mezik.socketchat.model.message;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ua.mezik.socketchat.model.Account;
import ua.mezik.socketchat.model.Chat;
import ua.mezik.socketchat.model.ChatMessage;
import ua.mezik.socketchat.model.MessageType;

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
