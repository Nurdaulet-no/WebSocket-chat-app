package org.example.projectchat.controller;

import jakarta.validation.Valid;
import org.example.projectchat.DTO.MessageRequest;
import org.example.projectchat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MessageController {
    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService){
        this.messageService = messageService;
    }

    @PostMapping("/messages")
    public ResponseEntity<Void> sendMessage(@Valid @RequestBody MessageRequest messageRequest){
        messageService.saveMessage(messageRequest);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
