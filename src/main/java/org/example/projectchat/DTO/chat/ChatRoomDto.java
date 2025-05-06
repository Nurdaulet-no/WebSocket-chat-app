package org.example.projectchat.DTO;

import org.example.projectchat.model.ChatRoomType;

import java.util.List;

public record ChatRoomDto(
        Long id,
        String name,
        ChatRoomType type,
        List<String> participantUsernames
) {
}
