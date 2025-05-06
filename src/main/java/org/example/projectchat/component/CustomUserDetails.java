package org.example.projectchat.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomUserDetails implements UserDetails {
    private Long id;
    private String username;
    private String password;
    private List<String> adminGroupIds;
    private Collection<? extends GrantedAuthority> authorities;
}
