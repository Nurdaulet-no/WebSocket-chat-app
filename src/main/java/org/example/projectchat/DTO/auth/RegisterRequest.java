package org.example.projectchat.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username cant be empty")
        @Size(min = 3, max = 30, message = "Username length should be from 3 to 30")
        String username,

        @NotBlank(message = "User's first name cant be empty")
        String userFirstName,
        @NotBlank(message = "Email cant be empty")
        @Email(message = "Incorrect format of email")
        String email,

        @NotBlank(message = "Password cant be empty")
        @Size(min = 8, max = 25, message = "Password length should be from 8 to 25")
        String password
) {

}
