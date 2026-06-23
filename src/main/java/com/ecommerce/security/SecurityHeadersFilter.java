package com.ecommerce.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // FIX 10021: Stop browsers from sniffing content types away from declared
        // payloads
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // FIX 90004: Isolate resource boundaries against cross-origin tracking
        // mechanisms
        httpResponse.setHeader("Cross-Origin-Resource-Policy", "same-origin");

        // FIX 10049: Prevent web browsers from caching sensitive REST API or error data
        httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        httpResponse.setHeader("Pragma", "no-cache");

        chain.doFilter(request, response);
    }
}
