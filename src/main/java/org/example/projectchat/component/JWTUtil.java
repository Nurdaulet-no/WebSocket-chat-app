package org.example.projectchat.component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JWTUtil {
    @Value("${jwt.secret.key}")
    private String SUPER_SECRET_KEY ;

    @Value("${jwt.expiration.ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey(){
        byte[] keyBytes = Decoders.BASE64.decode(SUPER_SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Date extractExpiration(String token){
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload().getExpiration();
    }
    public String extractUsername(String token){
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public String generateToken(UserDetails userDetails){
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails){
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject().equals(userDetails.getUsername()) && !claims.getExpiration().before(new java.util.Date());
    }
}
