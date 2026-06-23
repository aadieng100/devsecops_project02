package com.ecommerce.api.controller;

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
}
