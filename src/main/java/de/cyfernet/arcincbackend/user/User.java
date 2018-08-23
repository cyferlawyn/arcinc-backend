package de.cyfernet.arcincbackend.user;

import org.bson.Document;
import org.springframework.data.annotation.Id;

public class User {
    @Id
    public String id;

    public String name;

    public String passwordHash;

    public String authToken;

    public Long created;

    public Long lastSeen;

    public boolean muted = false;

    public boolean flagged = false;

    public boolean banned = false;

    public Document savegame;
}
