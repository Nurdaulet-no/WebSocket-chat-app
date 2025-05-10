package org.example.projectchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.chat.ChatRoomDto;
import org.example.projectchat.DTO.chat.ChatRoomListItemDto;
import org.example.projectchat.DTO.chat.CreateGroupChatRequest;
import org.example.projectchat.model.User;
import org.example.projectchat.service.ChatRoomService;
import org.example.projectchat.service.UserService;
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

    // Injection services
    private final UserService userService;
    private final ChatRoomService chatRoomService;

    // Get current user's chat
    @GetMapping
    public ResponseEntity<List<ChatRoomListItemDto>> getUserChatRooms(Principal principal){
        String username = principal.getName();
        log.info("Response chat rooms for user {}", username);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<ChatRoomListItemDto> chatRoomDtos = chatRoomService.findUserChatListItems(user);

        return ResponseEntity.ok(chatRoomDtos);
    }

    // Get details of specific room
    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomDto> getChatRoomById(
            @PathVariable Long roomId,
            Principal principal) {

        String username = principal.getName();
        log.info("Запрос деталей для комнаты {} от пользователя {}", roomId, username);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        ChatRoomDto chatRoomDto = chatRoomService.getChatRoomDetails(roomId, user);

        return ResponseEntity.ok(chatRoomDto);
    }

    // Create group chat
    @PostMapping("/group")
    public ResponseEntity<ChatRoomDto> createGroupChat(
            @Valid @RequestBody CreateGroupChatRequest createGroupChatRequest,
            Principal principal)
    {
        String username = principal.getName();
        log.info("Create chat rooms for user {}", username);

        User user = userService.findByUsername(username)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not FOUND"));

        ChatRoomDto newGroup = chatRoomService.createGroupChat(
                createGroupChatRequest.groupName(),
                user,
                createGroupChatRequest.participantUsernames());

        return ResponseEntity.status(HttpStatus.CREATED).body(newGroup);
    }

    // Get/create private chat(search bar if user messages it finds if not then creates new chat with that user)
    @PostMapping("/private/{username}")
    public ResponseEntity<ChatRoomDto> getOrCreatePrivateChat(
            @PathVariable String username,
            Principal principal
    ){
        String usernameA = principal.getName();
        log.info("Запрос на приватный чат между {} и пользователем {}", usernameA, username);

        User userA = userService.findByUsername(usernameA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current User not found"));

        ChatRoomDto chatRoomDto =chatRoomService.getOrCreateChatRoomService(userA, username);

        return ResponseEntity.status(HttpStatus.CREATED).body(chatRoomDto);
    }

    // Add participants in group chat
    @PutMapping("/{roomId}/participants/{usernameToAdd}")
    public ResponseEntity<ChatRoomDto> addParticipantToRoom(
            @PathVariable Long roomId,
            @PathVariable String usernameToAdd,
            Principal principal
    ){
        String username = principal.getName();
        log.info("Response to add user {} in group {} from user {}",usernameToAdd, roomId, username);

        User userInitiator = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User for adding member to group NOT FOUND"));

        ChatRoomDto chatRoomDto = chatRoomService.addParticipantsToGroup(roomId, usernameToAdd, userInitiator);
        return ResponseEntity.ok(chatRoomDto);
    }

    // Remove participants in group
    @DeleteMapping("/{roomId}/participants/{usernameToDelete}")
    public ResponseEntity<ChatRoomDto> deleteParticipantFromRoom(
            @PathVariable Long roomId,
            @PathVariable String usernameToDelete,
            Principal principal
    ){
        String username = principal.getName();
        log.info("Response to delete user {} in group {} from user {}",usernameToDelete, roomId, username);

        User userInitiator = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User for removing member from group NOT FOUND"));

        chatRoomService.deleteParticipantsFromGroup(roomId, usernameToDelete, userInitiator);

        return ResponseEntity.noContent().build();
    }

}
