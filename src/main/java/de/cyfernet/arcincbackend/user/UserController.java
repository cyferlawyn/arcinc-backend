package de.cyfernet.arcincbackend.user;

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

    @RequestMapping(value="create", method = RequestMethod.POST)
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

    @RequestMapping(value="login", method = RequestMethod.POST)
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

    @RequestMapping(value="save", method = RequestMethod.POST)
    public ResponseEntity save(@RequestBody SaveReqDto saveReqDto) {
        if (saveReqDto != null && saveReqDto.authToken != null && saveReqDto.savegame != null) {
            User user = userRepository.findByAuthToken(saveReqDto.authToken);

            if (user != null) {
                user.lastSeen = OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
                user.savegame = Document.parse(saveReqDto.savegame);
                userRepository.save(user);
                logger.debug(append("user", user), "Saved user");

                return new ResponseEntity(HttpStatus.OK);
            }
        }

        logger.error(append("saveReqDto", saveReqDto), "Failed to save user");
        return new ResponseEntity(HttpStatus.FORBIDDEN);
    }

    @RequestMapping(value="leaderboard", method = RequestMethod.GET)
    public ResponseEntity<LeaderboardResDto[]>leaderboard() {
        BasicQuery query = new BasicQuery("{}");
        query.with(new Sort(Sort.Direction.DESC, "savegame.highestWave"));
        query.limit(50);
        List<User> users = mongoTemplate.find(query, User.class);

        LeaderboardResDto[] leaderboardResDtos = new LeaderboardResDto[users.size()];

        for (int i = 0; i < users.size(); i++) {
            LeaderboardResDto leaderboardResDto = new LeaderboardResDto();
            leaderboardResDto.rank = i + 1;
            leaderboardResDto.name = users.get(i).name;
            leaderboardResDto.highestWave = users.get(i).savegame.getInteger("highestWave");
            leaderboardResDtos[i] = leaderboardResDto;
        }

        return new ResponseEntity<>(leaderboardResDtos, HttpStatus.OK);
    }
}
