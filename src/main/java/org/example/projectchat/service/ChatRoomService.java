package org.example.projectchat.service;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.ChatRoomDto;
import org.example.projectchat.model.ChatRoom;
import org.example.projectchat.model.ChatRoomType;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.ChatRoomRepository;
import org.example.projectchat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    public List<ChatRoomDto> findUserChatRooms(User user){
        Set<ChatRoom> userRooms = chatRoomRepository.findByParticipantsContaining(user);
        log.info("Найдено {} чатов для пользователя {}", userRooms.size(), user.getUsername());

        List<ChatRoomDto> roomDtoList = userRooms.stream().map(chatRoom -> {
            List<String> participantUsernames = chatRoom
                    .getParticipants().stream().map(User::getUsername).toList();

            return new ChatRoomDto(
                    chatRoom.getId(),
                    chatRoom.getName(),
                    chatRoom.getType(),
                    participantUsernames
            );
        }).toList();

        return roomDtoList;
    }

    @Transactional
    public ChatRoomDto createGroupChat(String groupName, User creator, Set<String> initParticipantUsernames){
        // Check: Is group unique
        if(chatRoomRepository.existsByNameAndType(groupName, ChatRoomType.GROUP)){
            log.warn("You cant creat group with name {}", groupName);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group with this name exist");
        }

        // Create new group
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(groupName);
        chatRoom.setType(ChatRoomType.GROUP);

        // Add user
        Set<User> participants = new HashSet<>();
        participants.add(creator);

        if(initParticipantUsernames != null && !initParticipantUsernames.isEmpty()){
            List<User> foundUsers = userRepository.findByUsernameIn(initParticipantUsernames);
            participants.addAll(foundUsers);
        }
        chatRoom.setParticipants(participants);

        ChatRoom saveChatRoom = chatRoomRepository.save(chatRoom);
        log.info("New group with name {} created", groupName);

        List<String> participantNames = saveChatRoom.getParticipants()
                .stream().map(User::getUsername).toList();

        return new ChatRoomDto(
                saveChatRoom.getId(),
                saveChatRoom.getName(),
                saveChatRoom.getType(),
                participantNames
        );
    }

    @Transactional
    public ChatRoomDto getOrCreateChatRoomService(User userA, String userBUsername){
        // find userB
        User userB = userRepository.findByUsername(userBUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with username:"+userBUsername+" NOT FOUND"));

        // check its not same user
        if(userA.getId().equals(userB.getId())){
            log.warn("Попытка создать чат с самим собой через сервис: {}", userA.getUsername());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cant open private chat by yourself");
        }

        Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateChatRoomByUsers(userA, userB, ChatRoomType.PRIVATE);
        ChatRoom chatRoom;

        // if have chat between two users
        if(existingRoom.isPresent()){
            chatRoom = existingRoom.get();
            log.info("Найден существующий приватный чат (ID: {}) между {} и {}", chatRoom.getId(), userA.getUsername(), userB.getUsername());

        }else {
            log.info("Создание нового приватного чата между {} и {}", userA.getUsername(), userB.getUsername());
            chatRoom = new ChatRoom();
            chatRoom.setType(ChatRoomType.PRIVATE);
            // currentUser send message to UserB then the name of room should be userB's username
            chatRoom.setName(userB.getUsername());

            // Add two user in chat
            Set<User> participants = new HashSet<>();
            participants.add(userA);
            participants.add(userB);
            chatRoom.setParticipants(participants);

            chatRoom  = chatRoomRepository.save(chatRoom);
        }

        // Map to Dto
        List<String> participantUsernames = chatRoom.getParticipants().stream()
                .map(User::getUsername).toList();

        return new ChatRoomDto(
                chatRoom.getId(),
                userBUsername,
                chatRoom.getType(),
                participantUsernames
        );
    }

    @Transactional
    public ChatRoomDto addParticipantsToGroup(Long roomId, String usernameToAdd, User initiator){
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if(chatRoom.getType() != ChatRoomType.GROUP){
            log.warn("Попытка добавить участника в не-групповой чат (ID: {}, Type: {})", roomId, chatRoom.getType());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to add participant to private chat");
        }

        if(!chatRoomRepository.existsByIdAndParticipants_Id(roomId, initiator.getId())){
            throw new AccessDeniedException("You are not contain and cant add user in this group");
        }

        User userToAdd = userRepository.findByUsername(usernameToAdd)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User to add participant not found"));

        boolean added = chatRoom.getParticipants().add(userToAdd);

        if(!added){
            log.warn("Пользователь {} уже является участником группы {}", usernameToAdd, roomId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User " + usernameToAdd + " is already participant of this group");
        }



        chatRoom = chatRoomRepository.save(chatRoom);
        log.info("Пользователь {} успешно добавлен в группу '{}' (ID: {}) пользователем {}",
                userToAdd.getUsername(), chatRoom.getName(), chatRoom.getId(), initiator.getUsername());

        return mapToChatRoomDto(chatRoom, initiator.getUsername());
    }

    @Transactional
    public ChatRoomDto deleteParticipantsFromGroup(Long roomId, String usernameToDelete, User initiator){
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if(chatRoom.getType() != ChatRoomType.GROUP){
            log.warn("Попытка удалить участника в не-групповой чат (ID: {}, Type: {})", roomId, chatRoom.getType());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to delete participant to private chat");
        }

        if(!chatRoomRepository.existsByIdAndParticipants_Id(roomId, initiator.getId())){
            log.warn("Попытка удалить участника не находясь в групповой чат (ID: {}, Type: {})", roomId, chatRoom.getType());
            throw new AccessDeniedException("You are not contain and cant add user in this group");
        }

        User userToDelete = userRepository.findByUsername(usernameToDelete)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User" + usernameToDelete + " for delete is not found"));

        if(initiator.getId().equals(userToDelete.getId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вы не можете удалить себя из группы этим методом. Используйте функцию 'Покинуть группу'.");
        }

        boolean deleted = chatRoom.getParticipants().remove(userToDelete);

        if(deleted){
            log.warn("Пользователь {} не найден в участниках группы {}", usernameToDelete, roomId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь '" + usernameToDelete + "' не является участником этой группы");
        }

        chatRoom = chatRoomRepository.save(chatRoom);
        log.info("Пользователь {} успешно удален из группу '{}' (ID: {}) пользователем {}",
                userToDelete.getUsername(), chatRoom.getName(), chatRoom.getId(), initiator.getUsername());

        return mapToChatRoomDto(chatRoom, initiator.getUsername());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ChatRoomDto getChatRoomDetails(Long roomId, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комната с ID " + roomId + " не найдена"));

        if (!chatRoomRepository.existsByIdAndParticipants_Id(roomId, user.getId())) {
            log.warn("Доступ запрещен: Пользователь {} попытался получить детали комнаты {}, в которой не состоит", user.getUsername(), roomId);
            throw new AccessDeniedException("Доступ к этой комнате запрещен");
        }

        log.info("Загружены детали для комнаты ID {} для пользователя {}", roomId, user.getUsername());
        return mapToChatRoomDto(chatRoom, user.getUsername());
    }

    private ChatRoomDto mapToChatRoomDto(ChatRoom chatRoom, String currentUsername){
        List<String> participantUsernames = chatRoom.getParticipants().stream()
                .map(User::getUsername).toList();

        String displayName = chatRoom.getName();
        if(chatRoom.getType() == ChatRoomType.PRIVATE && chatRoom.getParticipants().size() == 2){
            displayName = participantUsernames.stream()
                    .filter(name -> !name.equals(currentUsername))
                    .findFirst().orElse("Private Chat");
        }else if (displayName == null && chatRoom.getType() == ChatRoomType.GROUP){
            displayName = "Group " + chatRoom.getId();
        }

        return new ChatRoomDto(
                chatRoom.getId(),
                displayName,
                chatRoom.getType(),
                participantUsernames
        );
    }
}
