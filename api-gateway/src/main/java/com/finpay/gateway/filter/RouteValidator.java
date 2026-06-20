package com.finpay.gateway.filter;

import org.springframework.stereotype.Component;
import org.springframework.http.server.reactive.ServerHttpRequest;
import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh");
    public Predicate<ServerHttpRequest> isSecured = request -> PUBLIC_ENDPOINTS.stream().noneMatch(uri -> request.getURI().getPath().startsWith(uri));
}
