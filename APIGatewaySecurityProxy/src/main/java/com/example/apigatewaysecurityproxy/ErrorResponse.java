package com.example.apigatewaysecurityproxy;

import lombok.Getter;

import java.time.Instant;

@Getter
public class ErrorResponse {

    private  int status;
    private  String error;
    private String message;
    private long timestamp;

    public ErrorResponse(
            int status,
            String error,
            String message,
            long timestamp
    ) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
    }

    public static ErrorResponse of(
            int status,
            String message
    ) {
        return new ErrorResponse(
                status,
                "Security Error",
                message,
                Instant.now().toEpochMilli()
        );
    }

    public String toJson() {
        return String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
                status,
                error,
                message,
                timestamp
        );
    }
}
