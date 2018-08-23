package de.cyfernet.arcincbackend.chat;

import org.springframework.data.annotation.Id;

public class ChatEntry {
    @Id
    public String id;

    public String userId;

    public String userName;

    public Long timestamp;

    public String time;

    public String text;
}
