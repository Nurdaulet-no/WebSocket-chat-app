package org.example.projectchat.component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@RequiredArgsConstructor
@Slf4j
public class JWTRequestFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String jwt = extractJwtFromRequest(request);

        if(!StringUtils.hasText(jwt) || SecurityContextHolder.getContext().getAuthentication() != null){
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtUtil.extractUsername(jwt);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if(jwtUtil.validateToken(jwt, userDetails)){

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authenticationToken);
                SecurityContextHolder.setContext(context);

                log.debug("User '{}' successful authenticated with JWT", username);
            }
        }catch (ExpiredJwtException e){
            log.warn("Токен истек: {}", e.getMessage());
        } catch (MalformedJwtException e){
            log.warn("Некорректный формат JWT: {}", e.getMessage());
        }catch (UnsupportedJwtException e){
            log.warn("Неподдерживаемый JWT: {}", e.getMessage());
        }catch (IllegalArgumentException e){
            log.warn("Некорректные аргументы JWT: {}", e.getMessage());
        }catch (Exception e){
            log.error("Ошибка аутентификации по JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request){
        final String header = request.getHeader(AUTHORIZATION_HEADER);
        if(StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)){
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
