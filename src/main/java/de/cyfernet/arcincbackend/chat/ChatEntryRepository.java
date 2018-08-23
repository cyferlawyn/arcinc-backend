package de.cyfernet.arcincbackend.chat;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatEntryRepository extends MongoRepository<ChatEntry, String> {
    List<ChatEntry> findTop50ByOrderByTimestampDesc();
}
