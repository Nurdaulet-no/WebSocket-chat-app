package org.example.projectchat.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequest {
    @NotBlank(message = "message cant be null")
    String content;
}
