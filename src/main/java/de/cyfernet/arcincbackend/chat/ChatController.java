package de.cyfernet.arcincbackend.chat;

import de.cyfernet.arcincbackend.user.User;
import de.cyfernet.arcincbackend.user.UserRepository;
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
import java.util.List;

import static net.logstash.logback.marker.Markers.append;
@RestController
public class ChatController {
    @Autowired
    UserRepository userRepository;

    @Autowired
    ChatEntryRepository chatEntryRepository;

    private final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @RequestMapping(value="/chat/send", method = RequestMethod.POST)
    public ResponseEntity send(@RequestParam(value="authToken") String authToken, @RequestParam(value="text") String text) {
        User user = userRepository.findByAuthToken(authToken);

        if (user != null) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            ChatEntry chatEntry = new ChatEntry();
            chatEntry.userId = user.id;
            chatEntry.userName = user.name;
            chatEntry.timestamp = now.toInstant().toEpochMilli();
            chatEntry.time = String.format("%02d" , now.getHour()) + ":" + String.format("%02d" , now.getMinute()) + ":" + String.format("%02d" , now.getSecond());
            chatEntry.text = text;
            chatEntryRepository.save(chatEntry);

            logger.debug(append("user", user).and(append("chatEntry", chatEntry)), "Sent to chat");

            return new ResponseEntity(HttpStatus.CREATED);
        } else {
            logger.error(append("authToken", authToken).and(append("text", text)), "Failed to send to chat");
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value="/chat/receive", method = RequestMethod.GET)
    public ResponseEntity<ChatEntryDto[]> receive(@RequestParam(value="authToken") String authToken) {
        User user = userRepository.findByAuthToken(authToken);

        if (user != null) {
            List<ChatEntry> chatEntries = chatEntryRepository.findTop50ByOrderByTimestampDesc();
            ChatEntryDto[] chatEntryDtos = new ChatEntryDto[chatEntries.size()];

            for (int i = 0; i < chatEntries.size(); i++) {
                ChatEntryDto chatEntryDto = new ChatEntryDto();
                chatEntryDto.time = chatEntries.get(i).time;
                chatEntryDto.name = chatEntries.get(i).userName;
                chatEntryDto.text = chatEntries.get(i).text;
                chatEntryDtos[i] = chatEntryDto;
            }

            return new ResponseEntity<>(chatEntryDtos, HttpStatus.OK);
        } else {
            logger.error(append("authToken", authToken), "Failed to receive chat");
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
    }
}
