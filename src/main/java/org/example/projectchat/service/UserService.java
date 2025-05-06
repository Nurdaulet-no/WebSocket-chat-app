package org.example.projectchat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.user.UserDto;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserDto> searchUserByUsernameContaining(String query){
        log.debug("Searching users by username containing: {}", query);
        if(query == null || query.trim().isEmpty()){
            log.debug("Empty query, returning empty list.");
            return List.of();
        }

        List<User> foundUsers = userRepository.findByUsernameContainingIgnoreCase(query.trim());
        List<UserDto> userDtos = foundUsers.stream()
                .map(user -> new UserDto(user.getId(), user.getUsername()))
                .toList();
        log.debug("Found {} users for query: {}", userDtos.size(), query);
        return userDtos;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username){
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<User> findByUsernameInSet(Set<String> participantUsername){
        log.debug("Finding users by username in set: {}", participantUsername);
        return userRepository.findByUsernameIn(participantUsername);
    }


}

