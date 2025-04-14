package org.example.projectchat.repository;

import org.example.projectchat.model.ChatRoom;
import org.example.projectchat.model.ChatRoomType;
import org.example.projectchat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsUserByUsername(String username);
    List<User> findByUsernameIn(Set<String> participantUsernames);
}
