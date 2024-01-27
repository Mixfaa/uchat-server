package ua.mezik.socketchat.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Calendar;

@Entity
@Data
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class ChatMessage {
    @Id
    @GeneratedValue
    private long id;
    @ManyToOne
    private Account owner;
    @ManyToOne
    @JsonIgnore
    private Chat chat;
    private MessageType type;
    private Timestamp timestamp;

    public ChatMessage(Account owner, Chat chat, MessageType type) {
        this.owner = owner;
        this.chat = chat;
        this.type = type;
        this.timestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
    }

    public ChatMessage() {
    }
}
