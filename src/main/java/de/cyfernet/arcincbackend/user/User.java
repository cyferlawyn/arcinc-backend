package de.cyfernet.arcincbackend.user;

import org.springframework.data.annotation.Id;

public class User {
    @Id
    public String id;

    public String name;

    public String passwordHash;

    public String authToken;

    public Long created;

    public Long lastSeen;

    public String savegame;
}
