package org.example.projectchat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenRefreshException extends RuntimeException
{
    public TokenRefreshException(String message){
        super(message);
    }

    public TokenRefreshException(String message, Throwable cause){
        super(message, cause);
    }
}
