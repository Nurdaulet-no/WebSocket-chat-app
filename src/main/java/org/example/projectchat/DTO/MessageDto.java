package org.example.projectchat.DTO;

import java.time.LocalDateTime;

public record MessageDto(
        Long id,
        String content,
        LocalDateTime createdAt,
        String senderUsername
){
}
