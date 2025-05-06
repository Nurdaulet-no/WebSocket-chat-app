package org.example.projectchat.repository;

import org.example.projectchat.model.RefreshToken;
import org.example.projectchat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findAllByUser(User user);
    Optional<RefreshToken> findByJti(String jti);
    Optional<RefreshToken> findByUserAndRevokedFalse(User user);
    List<RefreshToken> findAllByUserAndRevokedFalseAndExpiryDateAfter(User user, Instant now);

    @Modifying
    int deleteAllByExpiryDateBefore(Instant now);

    @Modifying
    int deleteAllByUser(User user);

    @Modifying
    int deleteAllByUserAndRevokedTrue(User user);
}
