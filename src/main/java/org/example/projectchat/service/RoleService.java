package org.example.projectchat.service;

import lombok.RequiredArgsConstructor;
import org.example.projectchat.model.Role;
import org.example.projectchat.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Optional<Role> findUserByName(String name){
        return roleRepository.findByName(name);
    }
}
