package org.example.projectchat;

import org.example.projectchat.DTO.ChatRoomDto;
import org.example.projectchat.model.ChatRoom;
import org.example.projectchat.model.ChatRoomType;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.ChatRoomRepository;
import org.example.projectchat.repository.UserRepository;
import org.example.projectchat.service.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatRoomServiceTest {
    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private User testCreator;
    private String groupName;

    @BeforeEach
    void setUp(){
        testCreator = new User();
        testCreator.setId(1L);
        testCreator.setUsername("testCreator");

        groupName = "Test group";
    }

    // Test 1: Should create group when name is unique and no participants
    @Test
    void testCreateGroupChat(){
        when(chatRoomRepository.existsByNameAndType(groupName, ChatRoomType.GROUP)).thenReturn(false);

        ChatRoom savedChatRoom = new ChatRoom();
        savedChatRoom.setId(100L);
        savedChatRoom.setName(groupName);
        savedChatRoom.setType(ChatRoomType.GROUP);
        savedChatRoom.setParticipants(new HashSet<>(Set.of(testCreator)));

        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(savedChatRoom);

        ChatRoomDto resultDto = chatRoomService.createGroupChat(groupName, testCreator, null);

        assertNotNull(resultDto);
        assertEquals(100L, resultDto.id());
        assertEquals(groupName, resultDto.name());
        assertEquals(ChatRoomType.GROUP, resultDto.type());
        assertNotNull(resultDto.participantUsernames());
        assertEquals(1, resultDto.participantUsernames().size());
        assertTrue(resultDto.participantUsernames().contains(testCreator.getUsername()));

        verify(chatRoomRepository, times(1)).existsByNameAndType(groupName, ChatRoomType.GROUP);
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        verify(userRepository, never()).findByUsername(any());
    }

    // Test 2: Should throw exception when group name exists
    @Test
    void createGroupChat1(){
        when(chatRoomRepository.existsByNameAndType(groupName, ChatRoomType.GROUP)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatRoomService.createGroupChat(groupName, testCreator, null);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Group with this name exist"));

        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }
}
