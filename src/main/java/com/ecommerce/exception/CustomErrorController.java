package com.ecommerce.exception;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request, HttpServletResponse response) {
        int statusCode = response.getStatus();
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // FIX 10023 & 90022: Eradicate server error disclosures by returning a fully
        // sterile JSON map
        return ResponseEntity.status(status).body(Map.of(
                "status", String.valueOf(statusCode),
                "error", status.getReasonPhrase(),
                "message",
                "The requested operation could not be processed cleanly. Access anomalies have been logged."));
    }
}
