package org.example.projectchat.DTO.chat;

import org.example.projectchat.model.ChatRoomType;
import java.util.List;

public record ChatRoomListItemDto(
        Long id,
        String name,
        ChatRoomType type,
        List<String> participantUsernames,
        MessageDto lastMessage
) {
}
