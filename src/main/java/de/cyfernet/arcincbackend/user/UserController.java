package de.cyfernet.arcincbackend.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static net.logstash.logback.marker.Markers.append;

@RestController
public class UserController {
    @Autowired
    UserRepository userRepository;

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    @RequestMapping(value="/user/create", method = RequestMethod.POST)
    public ResponseEntity<String> create(@RequestParam(value="name") String name, @RequestParam(value="passwordHash") String passwordHash) {
        User user = new User();
        user.name = name;
        user.passwordHash = passwordHash;
        user.authToken = UUID.randomUUID().toString();
        user.created = OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
        user.lastSeen = user.created;

        if (userRepository.countByName(user.name) == 0 && user.name.length() < 16) {
            userRepository.save(user);
            logger.debug(append("user", user), "Created user");
            return new ResponseEntity<>(user.authToken, HttpStatus.CREATED);
        } else {
            logger.error(append("name", name).and(append("passwordHash", passwordHash)), "Failed to create user");
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @RequestMapping(value="/user/login", method = RequestMethod.POST)
    public ResponseEntity<String> login(@RequestParam(value="name") String name, @RequestParam(value="passwordHash") String passwordHash) {
        User user = userRepository.findByName(name);
        if (user != null && user.passwordHash.equals(passwordHash)) {
            user.lastSeen = OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
            userRepository.save(user);
            logger.debug(append("user", user), "Logged in user");
            return new ResponseEntity<>(user.authToken, HttpStatus.OK);
        } else {
            logger.error(append("name", name).and(append("passwordHash", passwordHash)), "Failed to log in user");
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value="/user/save", method = RequestMethod.POST)
    public ResponseEntity<String> save(@RequestParam(value="authToken") String authToken, @RequestParam(value="savegame") String savegame) {
        User user = userRepository.findByAuthToken(authToken);

        if (user != null) {
            user.lastSeen = OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
            user.savegame = savegame;
            userRepository.save(user);
            logger.debug(append("user", user), "Saved user");
            return new ResponseEntity<>(user.authToken, HttpStatus.OK);
        } else {
            logger.error(append("authToken", authToken).and(append("savegame", savegame)), "Failed to log in user");
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }
}
