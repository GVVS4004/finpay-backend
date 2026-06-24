package com.finpay.transaction.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalServiceInterceptor() {
        return requestTemplate -> requestTemplate.header("X-Internal-Service", "transaction-service");
    }
}