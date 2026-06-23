package com.ecommerce.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class BaseUtilityController {

    // Clean JSON response for root directory scans
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> getApiStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "ONLINE",
                "engine", "Headless E-Commerce REST API Engine v1.0"));
    }

    // Clean text response for standard crawler mappings
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getRobotsTxt() {
        return "User-agent: *\nDisallow: /actuator/\nDisallow: /api/v1/admin/";
    }

    // Graceful response for sitemap requests
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap() {
        return ResponseEntity.noContent().build(); // Standard clean 204 response
    }

    /**
     * Explicit handler for the Actuator root path.
     *
     * <p>Only /actuator/health is exposed (see application.properties).
     * Returning 404 here avoids having Spring generate a 500 error page that
     * leaks internal framework details — fixes ZAP [10023] and [90022].
     */
    @GetMapping(value = "/actuator/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getActuatorRoot() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Not Found"));
    }

    /**
     * Explicit handler for the admin API prefix.
     *
     * <p>This path is reserved but not publicly accessible. A clean 403 JSON
     * response prevents Spring from generating a 500 error page that exposes
     * implementation details — fixes ZAP [10023] and [90022].
     */
    @GetMapping(value = "/api/v1/admin/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getAdminRoot() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Forbidden"));
    }
}
