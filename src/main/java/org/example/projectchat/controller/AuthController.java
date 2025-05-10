package org.example.projectchat.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.DTO.auth.LoginRequest;
import org.example.projectchat.DTO.auth.LoginResponse;
import org.example.projectchat.DTO.auth.RefreshTokenResponseDto;
import org.example.projectchat.DTO.auth.RegisterRequest;
import org.example.projectchat.component.CustomUserDetails;
import org.example.projectchat.component.JWTUtil;
import org.example.projectchat.config.security.MyUserDetailService;
import org.example.projectchat.exception.TokenRefreshException;
import org.example.projectchat.model.RefreshToken;
import org.example.projectchat.model.Role;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.UserRepository;
import org.example.projectchat.service.RoleService;
import org.example.projectchat.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final RoleService roleService;
    private final RefreshTokenService refreshTokenService;
    private final MyUserDetailService myUserDetailService;

    @Value("${jwt.expiration.refresh-token}")
    private long jwtExpirationRefreshToken;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginRequest loginRequest, HttpServletResponse response){
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());

            // we give token to authentication and authentication use it to loadUser and PasswordEncoder
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // if authentication will success
            // get user details from Authentication
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername()).getToken();

            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false) // TODO: In production it will be true for https
                    .path("/api/auth")
                    .maxAge(jwtExpirationRefreshToken)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return ResponseEntity.ok(new LoginResponse(accessToken));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(401).body("Error authentication: Incorrect login or password");
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error in server: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                          HttpServletResponse response
    ){
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token is missing.");
        }

        try {
            RefreshToken foundRefreshToken = refreshTokenService.findByToken(refreshToken)
                    .orElseThrow(() -> new TokenRefreshException("Refresh token not found in DB."));

            refreshTokenService.verifyExpirationAndRevocation(foundRefreshToken);

            User user = foundRefreshToken.getUser();
            String newAccessToken = jwtUtil.generateAccessToken(createCustomUserDetailsFromUser(user));

            String newRefreshTokenString = refreshTokenService.rotateRefreshToken(foundRefreshToken);

            ResponseCookie newRefreshTokenCookie = ResponseCookie.from("refreshToken", newRefreshTokenString)
                    .httpOnly(true)
                    .secure(false) // TODO: In production it will be true
                    .path("/api/auth")
                    .maxAge(jwtExpirationRefreshToken)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, newRefreshTokenCookie.toString());

            return ResponseEntity.ok(new RefreshTokenResponseDto(newAccessToken));

        } catch (TokenRefreshException ex) {
            log.warn("Token refresh failed: {}", ex.getMessage());
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error during token refresh: {}", ex.getMessage(), ex);
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error during token refresh.");
        }
    }

    private CustomUserDetails createCustomUserDetailsFromUser(User user){
        return (CustomUserDetails) myUserDetailService.loadUserByUsername(user.getUsername());
    }
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie deleteRefreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false) // TODO: In production it will be true
                .path("/api/auth")
                .maxAge(0) // delete cookie
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie.toString());
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

        Role userRole = roleService.findUserByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found"));
        newUser.getRoles().add(userRole);

        try {
            userRepository.save(newUser);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error during registration: " + e.getMessage());
        }

    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refreshToken", required = false) String refreshTokenString, HttpServletResponse response){
        log.info("Logout attempt received.");

        if (refreshTokenString == null || refreshTokenString.isEmpty()) {
            log.info("No refresh token found in cookie. Assuming already logged out or cookie cleared.");
            return ResponseEntity.ok("Successfully logged out (no active refresh token found).");
        }

        try {
            refreshTokenService.invalidateRefreshToken(refreshTokenString);
            log.info("Refresh token invalidated successfully on the server.");

            clearRefreshTokenCookie(response);
            log.info("Refresh token cookie cleared successfully.");
            Map<String, String> responseLogout = new HashMap<>();
            responseLogout.put("message", "Logout successful.");
            return ResponseEntity.ok(responseLogout);
        }catch (TokenRefreshException exception){
            log.warn("Error during logout due to token issue: {}", exception.getMessage());
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Logout failed due to token issue: " + exception.getMessage());
        }catch (DataAccessException dae){
            log.error("Database access error during logout: {}", dae.getMessage(), dae);
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error during logout (database access).");
        }catch (Exception ex){
            log.error("Unexpected error during logout: {}", ex.getMessage(), ex);
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected server error occurred during logout.");
        }

    }
}
