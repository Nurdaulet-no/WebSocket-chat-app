package org.example.projectchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.ChatRoomDto;
import org.example.projectchat.DTO.CreateGroupChatRequest;
import org.example.projectchat.DTO.MessageDto;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.UserRepository;
import org.example.projectchat.service.ChatRoomService;
import org.example.projectchat.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {
    private final UserRepository userRepository;

    // Injection services
    private final ChatRoomService chatRoomService;
    private final MessageService messageService;

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<Page<MessageDto>> getMessageHistory(
            @PathVariable Long roomId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            Principal principal)
    {

        String username = principal.getName();
        log.info("Запрос истории сообщений для комнаты {} от пользователя {}", roomId, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        Page<MessageDto> messageDtoPage = messageService.findMessageHistory(roomId, user, pageable);

        return ResponseEntity.ok(messageDtoPage);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomDto> getChatRoomById(
            @PathVariable Long roomId,
            Principal principal) {

        String username = principal.getName();
        log.info("Запрос деталей для комнаты {} от пользователя {}", roomId, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        ChatRoomDto chatRoomDto = chatRoomService.getChatRoomDetails(roomId, user);

        return ResponseEntity.ok(chatRoomDto);
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomDto>> getUserChatRooms(Principal principal){
        String username = principal.getName();
        log.info("Response chat rooms for user {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<ChatRoomDto> chatRoomDtos = chatRoomService.findUserChatRooms(user);

        return ResponseEntity.ok(chatRoomDtos);
    }

    @PostMapping("/group")
    public ResponseEntity<ChatRoomDto> createGroupChat(
            @Valid @RequestBody CreateGroupChatRequest createGroupChatRequest,
            Principal principal)
    {
        String username = principal.getName();
        log.info("Create chat rooms for user {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not FOUND"));

        ChatRoomDto newGroup = chatRoomService.createGroupChat(
                createGroupChatRequest.groupName(),
                user,
                createGroupChatRequest.participantUsernames());

        return ResponseEntity.status(HttpStatus.CREATED).body(newGroup);
    }

    @PostMapping("/private/{username}")
    public ResponseEntity<ChatRoomDto> getOrCreatePrivateChat(
            @PathVariable String username,
            Principal principal
    ){
        String usernameA = principal.getName();
        log.info("Запрос на приватный чат между {} и пользователем {}", usernameA, username);

        User userA = userRepository.findByUsername(usernameA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current User not found"));

        ChatRoomDto chatRoomDto =chatRoomService.getOrCreateChatRoomService(userA, username);

        return ResponseEntity.status(HttpStatus.CREATED).body(chatRoomDto);
    }

    @PutMapping("/{roomId}/participants/{usernameToAdd}")
    public ResponseEntity<ChatRoomDto> addParticipantToRoom(
            @PathVariable Long roomId,
            @PathVariable String usernameToAdd,
            Principal principal
    ){
        String username = principal.getName();
        log.info("Response to add user {} in group {} from user {}",usernameToAdd, roomId, username);

        User userInitiator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User for adding member to group NOT FOUND"));

        ChatRoomDto chatRoomDto = chatRoomService.addParticipantsToGroup(roomId, usernameToAdd, userInitiator);
        return ResponseEntity.ok(chatRoomDto);
    }

    @DeleteMapping("/{roomId}/participants/{usernameToDelete}")
    public ResponseEntity<ChatRoomDto> deleteParticipantFromRoom(
            @PathVariable Long roomId,
            @PathVariable String usernameToDelete,
            Principal principal
    ){
        String username = principal.getName();
        log.info("Response to delete user {} in group {} from user {}",usernameToDelete, roomId, username);

        User userInitiator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User for removing member from group NOT FOUND"));

        chatRoomService.deleteParticipantsFromGroup(roomId, usernameToDelete, userInitiator);

        return ResponseEntity.noContent().build();
    }


}
