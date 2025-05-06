package org.example.projectchat.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.projectchat.exception.TokenRefreshException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Обработчик для вашего TokenRefreshException
    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<Object> handleTokenRefreshException(TokenRefreshException ex, WebRequest request) {
        log.warn("Token Refresh Error: {} (Path: {})", ex.getMessage(), getPath(request));
        return buildErrorResponse(ex, ex.getMessage(), HttpStatus.FORBIDDEN, request);
    }

    // Обработчик для ошибок аутентификации (неверный логин/пароль)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.warn("Authentication failed: {} (Path: {})", ex.getMessage(), getPath(request));
        return buildErrorResponse(ex, "Incorrect username or password.", HttpStatus.UNAUTHORIZED, request);
    }

    // Обработчик для ошибок авторизации (нет прав доступа)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {} (Path: {})", ex.getMessage(), getPath(request));
        return buildErrorResponse(ex, "You do not have permission to access this resource.", HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        log.warn("Validation error: {} (Path: {})", errors, getPath(request));

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Error");
        body.put("messages", errors);
        body.put("path", getPath(request));

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleGenericRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("Unexpected runtime error: {} (Path: {})", ex.getMessage(), getPath(request), ex);
        return buildErrorResponse(ex, "An unexpected internal server error occurred.", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error: {} (Path: {})", ex.getMessage(), getPath(request), ex);
        return buildErrorResponse(ex, "An unexpected server error occurred.", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }


    private ResponseEntity<Object> buildErrorResponse(Exception ex, String message, HttpStatus httpStatus, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", httpStatus.value());
        body.put("error", httpStatus.getReasonPhrase());
        body.put("message", message);
        body.put("path", getPath(request));

        return new ResponseEntity<>(body, httpStatus);
    }

    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return "N/A";
    }
}
