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

        // Fix 10021: Stop browsers from sniffing content types away from declared
        // headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // Fix 90004: Prevent cross-origin tracking and protect resource consumption
        // boundaries
        httpResponse.setHeader("Cross-Origin-Resource-Policy", "same-origin");

        chain.doFilter(request, response);
    }
}
