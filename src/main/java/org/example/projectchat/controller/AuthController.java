package org.example.projectchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.projectchat.DTO.LoginRequest;
import org.example.projectchat.DTO.LoginResponse;
import org.example.projectchat.DTO.RegisterRequest;
import org.example.projectchat.component.JWTUtil;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginRequest loginRequest){
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());

            // we give token to authentication and authentication use it to loadUser and PasswordEncoder
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // if authentication will success
            // get user details from Authentication
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // generate token to this user
            final String jwt = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(new LoginResponse(jwt));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(401).body("Error authentication: Incorrect login or password");
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error in server: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest){
        if(userRepository.existsUserByUsername(registerRequest.username())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User by this username are exist");
        }

        User newUser = new User();
        newUser.setUsername(registerRequest.username());
        newUser.setUserFirstName(registerRequest.userFirstName());
        newUser.setEmail(registerRequest.email());
        newUser.setPassword(passwordEncoder.encode(registerRequest.password()));

        try {
            userRepository.save(newUser);
            return ResponseEntity.status(HttpStatus.CREATED).body("User are successfully created");
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error during registration: " + e.getMessage());
        }

    }
}
