package de.cyfernet.arcincbackend.user;

import de.cyfernet.arcincbackend.Version;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.logstash.logback.marker.Markers.append;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {
    @Autowired
    UserRepository userRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public ResponseEntity<CreateResDto> create(@RequestBody CreateReqDto createReqDto) {
        if (createReqDto != null && createReqDto.name != null && createReqDto.passwordHash != null) {
            User user = new User();
            user.name = createReqDto.name;
            user.passwordHash = createReqDto.passwordHash;
            user.authToken = UUID.randomUUID().toString();
            user.created = OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
            user.lastSeen = user.created;

            if (userRepository.countByName(user.name) == 0 && user.name.length() < 16) {
                userRepository.save(user);

                logger.debug(append("user", user), "Created user");

                CreateResDto createResDto = new CreateResDto();
                createResDto.authToken = user.authToken;

                return new ResponseEntity<>(createResDto, HttpStatus.CREATED);
            }
        }

        logger.error(append("createReqDto", createReqDto), "Failed to create user");
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    @RequestMapping(value = "login", method = RequestMethod.POST)
    public ResponseEntity<LoginResDto> login(@RequestBody LoginReqDto loginReqDto) {
        if (loginReqDto != null && loginReqDto.name != null && loginReqDto.passwordHash != null) {
            User user = userRepository.findByName(loginReqDto.name);
            if (user != null && user.passwordHash.equals(loginReqDto.passwordHash) && !user.banned) {
                user.lastSeen = OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
                userRepository.save(user);

                logger.debug(append("user", user), "Logged in user");

                LoginResDto loginResDto = new LoginResDto();
                loginResDto.authToken = user.authToken;

                return new ResponseEntity<>(loginResDto, HttpStatus.OK);
            }
        }

        logger.error(append("loginReqDto", loginReqDto), "Failed to log in user");
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @RequestMapping(value = "save", method = RequestMethod.POST)
    public ResponseEntity save(@RequestBody SaveReqDto saveReqDto) {
        if (saveReqDto != null && saveReqDto.authToken != null && saveReqDto.savegame != null) {
            User user = userRepository.findByAuthToken(saveReqDto.authToken);

            if (user != null) {
                Document newSavegame = Document.parse(saveReqDto.savegame);

                /*
                if (user.savegame != null && user.savegame.getString("version") != null && newSavegame.getString("version") != null) {
                    String oldVersion = user.savegame.getString("version");
                    String newVersion = newSavegame.getString("version");

                    if (oldVersion != null && newVersion != null && oldVersion.equals(Version.CURRENT) && newVersion.equals(Version.CURRENT)) {
                        Integer oldHighestWave = user.savegame.getInteger("highestWave");
                        Integer newHighestWave = newSavegame.getInteger("highestWave");

                        if (oldHighestWave != null && newHighestWave != null && newHighestWave < oldHighestWave) {
                            // If all conditions above apply, the savegame is outdated and we will not save it.
                            user.lastSeen = OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
                            userRepository.save(user);
                            logger.warn(append("user", user).and(append("saveReqDto", saveReqDto)), "Skipped saving user");
                        }
                    }
                }
                */

                user.savegame = newSavegame;
                user.lastSeen = OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
                userRepository.save(user);
                logger.debug(append("user", user), "Saved user");

                return new ResponseEntity(HttpStatus.OK);
            }
        }

        logger.error(append("saveReqDto", saveReqDto), "Failed to save user");
        return new ResponseEntity(HttpStatus.FORBIDDEN);
    }

    @RequestMapping(value = "load", method = RequestMethod.POST)
    public ResponseEntity<LoadResDto> load(@RequestBody LoadReqDto loadReqDto) {
        if (loadReqDto != null && loadReqDto.authToken != null) {
            User user = userRepository.findByAuthToken(loadReqDto.authToken);
            LoadResDto loadResDto = new LoadResDto();
            loadResDto.savegame = user.savegame;

            logger.debug(append("user", user), "Loaded user");

            return new ResponseEntity<>(loadResDto, HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }


    @RequestMapping(value = "leaderboard", method = RequestMethod.GET)
    public ResponseEntity<LeaderboardResDto[]> leaderboard() {
        BasicQuery query = new BasicQuery("{'flagged': false, 'savegame.version': '" + Version.CURRENT + "'}");
        query.with(new Sort(Sort.Direction.DESC, "savegame.highestWave"));
        query.limit(100);
        List<User> users = mongoTemplate.find(query, User.class);

        List<LeaderboardResDto> leaderboardResDtos = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            try {
                LeaderboardResDto leaderboardResDto = new LeaderboardResDto();
                leaderboardResDto.rank = i + 1l;
                leaderboardResDto.name = users.get(i).name;
                if (users.get(i).savegame != null) {
                    leaderboardResDto.highestWave = users.get(i).savegame.getInteger("highestWave").longValue();
                    leaderboardResDto.activeAntimatter = users.get(i).savegame.getInteger("activeAntimatter").longValue();
                } else {
                    leaderboardResDto.highestWave = 0l;
                    leaderboardResDto.activeAntimatter = 0l;
                }
                leaderboardResDtos.add(leaderboardResDto);
            } catch (Throwable t) {
                logger.warn(append("user", users.get(i)), "Invalid data structure found");
            }
        }
        LeaderboardResDto[] leaderboardResDtoArray = new LeaderboardResDto[leaderboardResDtos.size()];
        leaderboardResDtos.toArray(leaderboardResDtoArray);
        return new ResponseEntity<>(leaderboardResDtoArray, HttpStatus.OK);
    }
}
