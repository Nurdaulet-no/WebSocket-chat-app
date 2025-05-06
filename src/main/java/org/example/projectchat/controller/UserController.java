package org.example.projectchat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.user.UserDto;
import org.example.projectchat.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    // Search user based their usernames
    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String usernameQuery){
        log.info("Received user search request for query: '{}'", usernameQuery);

        List<UserDto> foundUsers = userService.searchUserByUsernameContaining(usernameQuery);
        log.info("Found {} users for query: '{}'", foundUsers.size(), usernameQuery);

        return ResponseEntity.ok(foundUsers);
    }

}
