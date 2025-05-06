package org.example.projectchat.DTO.chat;

import org.example.projectchat.model.ChatRoomType;

import java.util.List;

public record ChatRoomDto(
        Long id,
        String name,
        ChatRoomType type,
        List<String> participantUsernames
) {
}
