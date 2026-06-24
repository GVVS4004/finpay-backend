package com.finpay.account.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class InternalServiceFilter extends OncePerRequestFilter {
    private static final String INTERNAL_SERVICE_HEADER = "X-Internal-service";
    private static final String EXPECTED_VALUE = "transaction-service";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isInternalOnlyEndpoint(path)) {
            String serviceHeader = request.getHeader(INTERNAL_SERVICE_HEADER);
            if (!EXPECTED_VALUE.equals(serviceHeader)) {
                log.warn("Blocked direct access to internal endpoint: {} from IP: {}",
                        path, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Access denied. Internal endpoint.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isInternalOnlyEndpoint(String path) {
        return path.matches(".*/api/accounts/.*/deposit") ||
                path.matches(".*/api/accounts/.*/withdraw");
    }
}
