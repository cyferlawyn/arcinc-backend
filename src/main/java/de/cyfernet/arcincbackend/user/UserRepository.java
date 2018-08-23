package de.cyfernet.arcincbackend.user;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    User findByName(String name);
    User findByAuthToken(String authToken);
    long countByName(String name);
}
