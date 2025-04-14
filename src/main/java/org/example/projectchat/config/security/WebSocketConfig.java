package org.example.projectchat.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.component.JWTUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE+99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JWTUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    private final String AUTHORIZATION_HEADER = "Authorization";
    private final String BEARER_PREFIX = "Bearer ";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry messageBrokerRegistry){
        messageBrokerRegistry.enableSimpleBroker("/topic", "/queue"); // /topic for public, /queue for private chat
        messageBrokerRegistry.setApplicationDestinationPrefixes("/app");
        messageBrokerRegistry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:5500")
                .withSockJS();

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration channelRegistration){
        channelRegistration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if(StompCommand.CONNECT.equals(accessor.getCommand()) && accessor.getUser() == null){
                    log.debug("Перехвачен CONNECT фрейм");

                    String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
                    log.debug("Заголовок Authorization в WebSocket: {}", authHeader);

                    if(StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)){
                        String jwt = authHeader.substring(BEARER_PREFIX.length());
                        log.debug("Извлечен JWT: {}", jwt);

                        try {
                            String username = jwtUtil.extractUsername(jwt);
                            if(StringUtils.hasText(username)){
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                                if(jwtUtil.validateToken(jwt, userDetails)){
                                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );

                                    accessor.setUser(authentication);
                                    log.info("Пользователь '{}' аутентифицирован для WebSocket сессии.", username);
                                }else {
                                    log.warn("Невалидный JWT токен для WebSocket (проверка validateToken не прошла).");
                                    throw new AuthenticationException("Invalid JWT token") {};
                                }
                            }
                        }catch (Exception e){
                            log.error("Ошибка аутентификации WebSocket: {}", e.getMessage());
                            throw new AuthenticationException("Ошибка аутентификации WebSocket: " + e.getMessage()) {};
                        }
                    }
                }else if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.info(">>> [WS Interceptor] Повторный перехват CONNECT фрейма (пользователь уже аутентифицирован: {}). Пропускаем.", accessor.getUser().getName());
                }

                log.debug(">>> [WS Interceptor] Возвращаем сообщение дальше.");
                return message;
            }
        });
    }
}
