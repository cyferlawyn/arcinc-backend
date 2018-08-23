package de.cyfernet.arcincbackend.chat;

import de.cyfernet.arcincbackend.user.User;
import de.cyfernet.arcincbackend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.logstash.logback.marker.Markers.append;

@RestController
@RequestMapping("/chat")
@CrossOrigin
public class ChatController {
    private final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ChatEntryRepository chatEntryRepository;

    @EventListener
    public void onChatSend(ChatEntry chatEntry) {
        List<SseEmitter> deadEmitters = new ArrayList<>();

        ChatEntryDto chatEntryDto = new ChatEntryDto();
        chatEntryDto.time = chatEntry.time;
        chatEntryDto.name = chatEntry.userName;
        chatEntryDto.text = chatEntry.text;

        this.emitters.forEach(emitter -> {
            try {
                emitter.send(chatEntryDto);
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        });

        this.emitters.removeAll(deadEmitters);
    }

    @RequestMapping(value = "send", method = RequestMethod.POST)
    public ResponseEntity send(@RequestBody SendReqDto sendReqDto) {
        if (sendReqDto != null && sendReqDto.authToken != null && sendReqDto.text != null) {
            User user = userRepository.findByAuthToken(sendReqDto.authToken);

            if (user != null && !user.muted && !user.banned) {
                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                ChatEntry chatEntry = new ChatEntry();
                chatEntry.userId = user.id;
                chatEntry.userName = user.name;
                chatEntry.timestamp = now.toInstant().toEpochMilli();
                chatEntry.time = String.format("%02d", now.getHour()) + ":" + String.format("%02d", now.getMinute()) + ":" + String.format("%02d", now.getSecond());
                chatEntry.text = sendReqDto.text;
                chatEntryRepository.save(chatEntry);

                this.applicationEventPublisher.publishEvent(chatEntry);

                logger.debug(append("user", user).and(append("chatEntry", chatEntry)), "Sent to chat");

                return new ResponseEntity(HttpStatus.CREATED);
            }
        }

        logger.error(append("sendReqDto", sendReqDto), "Failed to send to chat");
        return new ResponseEntity(HttpStatus.FORBIDDEN);
    }

    @RequestMapping(value = "receive", method = RequestMethod.GET)
    public ResponseEntity<SseEmitter> receive() {
        SseEmitter emitter = new SseEmitter(900_000L);
        this.emitters.add(emitter);

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));

        return new ResponseEntity<>(emitter, HttpStatus.OK);
    }
}
