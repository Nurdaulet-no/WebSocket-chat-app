package org.example.projectchat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.MessageDto;
import org.example.projectchat.DTO.MessageRequest;
import org.example.projectchat.model.Message;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.ChatRoomRepository;
import org.example.projectchat.repository.MessageRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    private final MessageRepository messageRepository;
    private final ModelMapper modelMapper;
    private final ChatRoomRepository chatRoomRepository;

    public void saveMessage(MessageRequest messageRequest){
        Message message = modelMapper.map(messageRequest, Message.class);

        messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Page<MessageDto> findMessageHistory(Long chatRoomId, User user, Pageable pageable){
        if(!chatRoomRepository.existsByIdAndParticipants_Id(chatRoomId, user.getId())){
            log.warn("Access denied: User {} not contain in group {}", user.getUsername(), chatRoomId);
            throw new AccessDeniedException("Access denied for history of this group");
        }

        Page<Message> messagePage = messageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId, pageable);
        log.info("Найдено {} сообщений на странице {} для комнаты {}", messagePage.getNumberOfElements(), pageable.getPageNumber(), chatRoomId);

        return messagePage.map(message -> new MessageDto(
                message.getId(),
                message.getContent(),
                message.getCreatedAt(),
                message.getSender() != null ? message.getSender().getUsername() : "Unknown"
        ));
    }
}
