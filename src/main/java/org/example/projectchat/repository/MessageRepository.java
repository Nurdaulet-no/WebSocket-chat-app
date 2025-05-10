package org.example.projectchat.repository;

import org.example.projectchat.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId, Pageable pageable);
    Optional<Message> findFirstByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);
}
