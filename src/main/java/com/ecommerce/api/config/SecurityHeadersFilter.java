package com.ecommerce.api.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SecurityHeadersFilter injects OWASP-recommended HTTP security headers on every
 * response, regardless of HTTP status code or controller path.
 *
 * <p>Headers applied:
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff}   — fixes ZAP [10021]</li>
 *   <li>{@code Cross-Origin-Resource-Policy: same-origin} — fixes ZAP [90004]</li>
 *   <li>{@code Cache-Control: no-store}             — fixes ZAP [10049]</li>
 *   <li>{@code X-Frame-Options: DENY}               — defence-in-depth (clickjacking)</li>
 *   <li>{@code X-XSS-Protection: 0}                 — disables legacy broken XSS filter per OWASP</li>
 * </ul>
 */
@Component
@Order(1)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // [10021] Prevents MIME-type sniffing attacks
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // [90004] Restricts cross-origin resource sharing at the browser level
        httpResponse.setHeader("Cross-Origin-Resource-Policy", "same-origin");

        // [10049] Ensures no response is stored by downstream caches
        httpResponse.setHeader("Cache-Control", "no-store");

        // Defence-in-depth: prevent framing / clickjacking
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // Disable legacy XSS filter (recommended by OWASP — modern browsers ignore it
        // and an enabled filter can itself be exploited)
        httpResponse.setHeader("X-XSS-Protection", "0");

        chain.doFilter(request, response);
    }
}
