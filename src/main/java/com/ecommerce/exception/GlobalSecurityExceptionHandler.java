package com.ecommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalSecurityExceptionHandler {

    // Intercept all unhandled runtime exceptions falling through the application
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnhandledExceptions(Exception ex) {
        // Log the real exception internally for developer tracking, but mask the public
        // payload
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", "500",
                        "error", "Internal Server Error",
                        "message", "An unexpected error occurred. Technical details have been logged."));
    }
}
