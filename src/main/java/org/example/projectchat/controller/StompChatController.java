package org.example.projectchat.controller;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.chat.ChatMessageDto;
import org.example.projectchat.DTO.chat.MessageDto;
import org.example.projectchat.model.ChatRoom;
import org.example.projectchat.model.Message;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.ChatRoomRepository;
import org.example.projectchat.repository.MessageRepository;
import org.example.projectchat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@Slf4j
@RequiredArgsConstructor
public class StompChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserService userService;

    @MessageMapping("/chat.sendMessage/{roomId}")
    @Transactional
    public void sendMessage(@DestinationVariable Long roomId, @Payload ChatMessageDto chatMessageDto, Principal principal){
        String username = principal.getName();
        log.info("Сообщение получено для комнаты {}: от {}: {} (ClientMsgID: {})",
                roomId, username, chatMessageDto.content(), chatMessageDto.clientMessageId()); // Log it

        User sender = userService.findByUsername(username).orElseThrow(() -> {
            log.error("STOMP CHAT Error: User {} not found", username);
            messagingTemplate.convertAndSendToUser(username, "/queue/errors", "User not found for sending message.");
            return new IllegalArgumentException("Sender not found");
        });

        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> {
            log.error("STOMP CHAT Error: Room with ID {} not found", roomId);
            messagingTemplate.convertAndSendToUser(username, "/queue/errors", "Chat room not found.");
            return new IllegalArgumentException("Room chat not found!");
        });

        if (chatRoom.getParticipants().stream().noneMatch(p -> p.getId().equals(sender.getId()))) {
            log.warn("STOMP CHAT: User {} is not a participant of room {}", username, roomId);
            messagingTemplate.convertAndSendToUser(username, "/queue/errors", "You are not a participant of this chat room.");
            throw new AccessDeniedException("User is not a participant of this chat room.");
        }

        Message message = new Message();
        message.setContent(chatMessageDto.content());
        message.setSender(sender);
        message.setChatRoom(chatRoom);

        Message savedMessage = messageRepository.save(message);
        log.info("Сообщение сохранено с ID: {}", savedMessage.getId());

        MessageDto messageToSendToClients = new MessageDto(
                savedMessage.getId(),
                savedMessage.getContent(),
                savedMessage.getCreatedAt(),
                sender.getUsername(),
                chatMessageDto.clientMessageId()
        );

        String destination = "/topic/rooms/" + roomId;
        messagingTemplate.convertAndSend(destination, messageToSendToClients);
        log.info("STOMP CHAT: Message broadcast to topic: {} with payload: {}", destination, messageToSendToClients);
    }
}
