package org.example.projectchat.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.ChatMessageDto;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Map<String, ActiveWebSocketUser> activeUsers = new ConcurrentHashMap<>();
    private static final Pattern chatTopicPattern = Pattern.compile("/topic/chats/(\\d+)");

    @EventListener
    public void handleWebSocketEventListener(SessionConnectEvent sessionConnectEvent){
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(sessionConnectEvent.getMessage());
        Principal userPrincipal = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (userPrincipal != null && sessionId != null) {
            log.info(">>> WebSocket CONNECTED. SessionId: {}, User: {}", sessionId, userPrincipal.getName());
        } else {
            log.warn(">>> WebSocket CONNECTED. SessionId: {}. User is NULL (maybe handshake phase?)", sessionId);
        }
    }

    @EventListener
    public void handleWebSocketSubscriberListener(SessionSubscribeEvent subscribeEvent){
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(subscribeEvent.getMessage());
        String destination = headerAccessor.getDestination();
        Principal principal = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        if(principal == null || sessionId == null){
            log.warn("Попытка подписки на {} без аутентифицированного пользователя (SessionId: {})", destination, sessionId);
            return;
        }

        String username = principal.getName();
        log.info("Пользователь '{}' (SessionId: {}) подписался на: {}", username, sessionId, destination);

        if(destination != null){
            Matcher matcher = chatTopicPattern.matcher(destination);
            if(matcher.matches()){
                Long roomId = Long.parseLong(matcher.group(1));
                log.info("Пользователь '{}' присоединился к комнате {}", username, roomId);

                ActiveWebSocketUser activeUser = activeUsers.computeIfAbsent(sessionId, id -> new ActiveWebSocketUser(username));
                activeUser.addSubscribedRoomId(roomId);
                log.info("Пользователь '{}' теперь активен в комнате {}. Активные сессии: {}", username, roomId, activeUsers.size());

                ChatMessageDto systemMessage = new ChatMessageDto(
                        "System",
                        username + " joined the chat!"
                );

                simpMessagingTemplate.convertAndSend(destination, systemMessage);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent disconnectEvent){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(disconnectEvent.getMessage());
        Principal principal = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        if (sessionId == null) {
            log.error("<<< Disconnect event received without sessionId!");
            return;
        }

        ActiveWebSocketUser disconnectedUser = activeUsers.remove(sessionId);
        if(disconnectedUser != null){
            String username = disconnectedUser.getUsername();
            Set<Long> roomIds = disconnectedUser.getSubscribedRoomsId();
            log.info("<<< WebSocket DISCONNECTED. SessionId: {}, User: {}. Был в комнатах: {}", sessionId, username, roomIds);

            ChatMessageDto systemMessage = new ChatMessageDto("System", username + " left the chat.");

            if(roomIds != null){
                roomIds.forEach(roomId -> {
                    String destination = "/topic/chats/" + roomId;
                    simpMessagingTemplate.convertAndSend(destination, systemMessage);
                    log.info("Отправлено 'left' уведомление для {} в {}", username, destination);
                });
            }

        }else{
            log.warn("<<< WebSocket DISCONNECTED. SessionId: {}. User information not available.", sessionId);
            log.info("Активные сессии после дисконнекта: {}", activeUsers.size());
        }
    }
}
