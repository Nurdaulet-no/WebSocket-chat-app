package org.example.projectchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.chat.MessageDto;
import org.example.projectchat.DTO.chat.MessageRequest;
import org.example.projectchat.model.User;
import org.example.projectchat.service.MessageService;
import org.example.projectchat.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {
    private final MessageService messageService;
    private final UserService userService;

    // Send Message
    @PostMapping
    public ResponseEntity<Void> sendMessage(@Valid @RequestBody MessageRequest messageRequest){
        log.info("Received REST request to send message: {}", messageRequest);
        messageService.saveMessage(messageRequest);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // Get message history for room
    @GetMapping("/history/{roomId}")
    public ResponseEntity<Page<MessageDto>> getMessageHistory(
            @PathVariable Long roomId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            Principal principal
            )
    {
        String username = principal.getName();
        log.info("Запрос истории сообщений для комнаты {} от пользователя {}", roomId, username);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        Page<MessageDto> messageDtoPage = messageService.findMessageHistory(roomId, user, pageable);
        return ResponseEntity.ok(messageDtoPage);
    }
}
