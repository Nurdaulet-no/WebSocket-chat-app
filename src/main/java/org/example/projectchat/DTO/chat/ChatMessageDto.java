package org.example.projectchat.DTO.chat;

public record ChatMessageDto(String sender, String content) {
    public ChatMessageDto(String content){
        this(null, content);
    }
}
