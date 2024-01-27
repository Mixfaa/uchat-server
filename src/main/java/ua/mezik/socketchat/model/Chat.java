package ua.mezik.socketchat.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Chat {
    @Id
    @GeneratedValue
    private long id;

    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    private Account owner;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Account> participants;

    private Long firstMessageId = -1L;

    public Chat(String name, Account owner, List<Account> participants) {
        this.name = name;
        this.owner = owner;
        this.participants = new ArrayList<>(participants);
    }
}
