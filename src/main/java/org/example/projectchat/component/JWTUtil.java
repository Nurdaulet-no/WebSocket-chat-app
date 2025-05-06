package org.example.projectchat.component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JWTUtil {
    @Value("${jwt.secret.key}")
    private String SUPER_SECRET_KEY ;

    @Value("${jwt.expiration.access-token}")
    private long jwtExpirationAccessToken;

    @Value("${jwt.expiration.refresh-token}")
    private long jwtExpirationRefreshToken;

    private SecretKey getSigningKey(){
        byte[] keyBytes = Decoders.BASE64.decode(SUPER_SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token){
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimResolver){
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    public Instant extractExpiration(String token){
        return extractClaim(token, Claims :: getExpiration).toInstant();
    }

    public String extractJti(String token){
        return extractClaim(token, claims -> claims.get("jti", String.class));
    }

    public String extractUsername(String token){
        return extractClaim(token, Claims :: getSubject);
    }

    public String generateAccessToken(CustomUserDetails userDetails){
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpirationAccessToken);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(expirationDate)
                .claim("userId", userDetails.getId())
                .claim("roles", roles)
                .claim("adminOf", userDetails.getAdminGroupIds())
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String username){
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpirationRefreshToken);

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(expirationDate)
                .claim("jti", UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token, CustomUserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        }catch (Exception e){
            return false;
        }
    }
    private boolean isTokenExpired(String token){
        return extractExpiration(token).isBefore(Instant.now());
    }
}
