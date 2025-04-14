package org.example.projectchat.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateGroupChatRequest(
        @NotBlank(message = "Group name cant be empty")
        @Size(min = 2, max = 50, message = "Name of group will will 2 <= x < 50")
        String groupName,
        Set<String> participantUsernames
) {
}
