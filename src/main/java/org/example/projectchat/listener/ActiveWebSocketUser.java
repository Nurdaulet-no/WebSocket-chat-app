package org.example.projectchat.listener;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ActiveWebSocketUser {
    @NotNull
    private String username;
    private Set<Long> subscribedRoomsId = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ActiveWebSocketUser(String username){
        this.username = username;
    }

    public void addSubscribedRoomId(Long roomId){
        subscribedRoomsId.add(roomId);
    }
}

