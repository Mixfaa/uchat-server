package ua.mezik.uchat.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue
    private long id;

    private String username;
    @JsonIgnore
    private String password;

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
