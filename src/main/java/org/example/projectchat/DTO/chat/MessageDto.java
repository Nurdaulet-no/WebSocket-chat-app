package org.example.projectchat.DTO.chat;

import java.time.LocalDateTime;

public record MessageDto(
        Long id,
        String content,
        LocalDateTime createdAt,
        String senderUsername,
        String clientMessageId
){
}
