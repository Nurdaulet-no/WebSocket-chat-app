package org.example.projectchat.config.security;

import lombok.RequiredArgsConstructor;
import org.example.projectchat.component.CustomUserDetails;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User appUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User with username: " + username + "not found"));

        List<GrantedAuthority> authorities = appUser.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .map(authority -> (GrantedAuthority) authority)
                .toList();

        // TODO:
        List<String> adminGroupIds = new ArrayList<>();

        return new CustomUserDetails(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getPassword(),
                adminGroupIds,
                authorities
        );
    }
}
