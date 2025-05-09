package org.example.projectchat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.component.JWTUtil;
import org.example.projectchat.exception.TokenRefreshException;
import org.example.projectchat.model.RefreshToken;
import org.example.projectchat.model.User;
import org.example.projectchat.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository tokenRepository;
    private final UserService userService;
    private final JWTUtil jwtUtil;

    @Transactional
    public RefreshToken createRefreshToken(String username){
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with username: " +username+"NOT_FOUND"));

        String newRefreshToken = jwtUtil.generateRefreshToken(username);
        String jti = jwtUtil.extractJti(newRefreshToken);
        Instant dateExpiration = jwtUtil.extractExpiration(newRefreshToken);

        tokenRepository.findByUserAndRevokedFalse(user).ifPresent(oldtoken ->{
            oldtoken.setRevoked(true);
            oldtoken.setReplacedByTokenJti(jti);
            tokenRepository.save(oldtoken);
        });

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(newRefreshToken);
        refreshToken.setJti(jti);
        refreshToken.setExpiryDate(dateExpiration);
        refreshToken.setUser(user);

        tokenRepository.save(refreshToken);
        return refreshToken;
    }

    public Optional<RefreshToken> findByToken(String refreshToken){
        return tokenRepository.findByToken(refreshToken);
    }

    @Transactional
    public RefreshToken verifyExpirationAndRevocation(RefreshToken refreshToken){
        if(refreshToken.isRevoked()){
            if(refreshToken.getReplacedByTokenJti() != null){
                tokenRepository.findByJti(refreshToken.getReplacedByTokenJti()).ifPresent(newToken -> {
                    newToken.setRevoked(true);
                    tokenRepository.save(newToken);
                    log.warn("Potential refresh token reuse detected! Token JTI: {}, replaced by JTI: {}. New token revoked.",
                            refreshToken.getJti(), newToken.getJti());
                });
            }

            throw new TokenRefreshException("Refresh token was revoked");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(String refreshToken){
        RefreshToken find = tokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new TokenRefreshException("Token not found"));

        find.setRevoked(true);
        tokenRepository.save(find);
    }

    @Transactional
    public void revokeAllUserTokens(User user){
        List<RefreshToken> tokens = tokenRepository.findAllByUser(user)
                .stream().peek(token -> token.setRevoked(true)).toList();

        tokenRepository.saveAll(tokens);
    }

    @Transactional
    public String rotateRefreshToken(RefreshToken oldRefreshToken){
        oldRefreshToken.setRevoked(true);
        tokenRepository.save(oldRefreshToken);

        String newToken = jwtUtil.generateRefreshToken(oldRefreshToken.getUser().getUsername());
        String jti = jwtUtil.extractJti(newToken);
        Instant expirationDate = jwtUtil.extractExpiration(newToken);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken(newToken);
        newRefreshToken.setJti(jti);
        newRefreshToken.setUser(oldRefreshToken.getUser());
        newRefreshToken.setExpiryDate(expirationDate);

        RefreshToken savedToken = tokenRepository.save(newRefreshToken);

        oldRefreshToken.setReplacedByTokenJti(savedToken.getJti());

        tokenRepository.save(oldRefreshToken);
        return newToken;
    }

    @Transactional
    public int deleteExpiredTokens(){
        return tokenRepository.deleteAllByExpiryDateBefore(Instant.now());
    }

    @Transactional
    public void invalidateRefreshToken(String tokenString){
        if(tokenString.isEmpty() || tokenString == null){
            log.debug("Attempted to invalidate null or empty refresh token");
            return;
        }

        Optional<RefreshToken> refreshToken = tokenRepository.findByToken(tokenString);

        if(refreshToken.isPresent()){
            RefreshToken token = refreshToken.get();
            if(token.isRevoked()){
                log.debug("Refresh token (token string: {}) is already revoked. No action needed.", tokenString);
                return;
            }

            token.setRevoked(true);
            tokenRepository.save(token);
            log.info("Refresh token (token string: {}) successfully marked as revoked.", tokenString);
        }else {
            log.info("Attempted to invalidate a refresh token that was not found in the database (token string: {}). It might have been already deleted or never existed.", tokenString);
        }
    }
}
