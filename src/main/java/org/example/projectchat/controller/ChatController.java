package org.example.projectchat.controller;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.ChatMessageDto;
import org.example.projectchat.model.ChatRoom;
import org.example.projectchat.model.Message;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.ChatRoomRepository;
import org.example.projectchat.repository.MessageRepository;
import org.example.projectchat.repository.UserRepository;
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
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    @MessageMapping("/chat/{roomId}/sendMessage")
    @Transactional
    public void sendMessage(@DestinationVariable Long roomId, @Payload ChatMessageDto chatMessageDto, Principal principal){
        String username = principal.getName();
        log.info("Сообщение получено для комнаты {}: от {}: {}", roomId, username, chatMessageDto.content());

        User sender = userRepository.findByUsername(username).orElseThrow(()->{
            log.error("Ошибка: Пользователь {} не найден", username);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender not found");
        });

        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() ->{
            log.error("Ошибка: Комната с ID {} не найдена", roomId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Room chat not found!");
        });

        if(!chatRoom.getParticipants().contains(sender)){
            log.warn("Пользователь {} не является участником комнаты {}", username, roomId);
            messagingTemplate.convertAndSendToUser(username, "queue/errors", "Not a participant");
            throw new AccessDeniedException("Не участник комнаты");
        }

        Message message = new Message();
        message.setContent(chatMessageDto.content());
        message.setSender(sender);
        message.setChatRoom(chatRoom);

        Message savedMessage = messageRepository.save(message);
        log.info("Сообщение сохранено с ID: {}", savedMessage.getId());

        ChatMessageDto messageToSend = new ChatMessageDto(
                sender.getUsername(),
                savedMessage.getContent()
        );

        String destination = "/topic/chats/" + roomId;
        messagingTemplate.convertAndSend(destination, messageToSend);
        log.info("Сообщение отправлено в топик: {}", destination);
    }
}
