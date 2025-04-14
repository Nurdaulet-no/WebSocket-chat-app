package org.example.projectchat.repository;

import org.example.projectchat.model.ChatRoom;
import org.example.projectchat.model.ChatRoomType;
import org.example.projectchat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr from ChatRoom cr join cr.participants p1 join cr.participants p2 " +
    "WHERE cr.type = ?3 AND p1 = ?1 AND p2 = ?2 and size(cr.participants) = 2")
    Optional<ChatRoom> findPrivateChatRoomByUsers(
            User user1,
            User user2,
            ChatRoomType type
            );

    Optional<ChatRoom> findByNameAndType(String name, ChatRoomType type);

    @Query("select distinct cr from ChatRoom cr join fetch cr.participants p where :user member of cr.participants")
    Set<ChatRoom> findByParticipantsContaining(@Param("user") User user);

    boolean existsByIdAndParticipants_Id(Long chatRoomId, Long userId);
    boolean existsByNameAndType(String name, ChatRoomType type);

}
