package com.deadlineiq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration registering the SecurityInterceptor across application routes,
 * and configuring CORS for desktop clients.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SecurityInterceptor securityInterceptor;
    private final String[] allowedOriginPatterns;

    public WebMvcConfig(
            SecurityInterceptor securityInterceptor,
            @Value("${app.cors.allowed-origin-patterns}") String allowedOriginPatterns) {
        this.securityInterceptor = securityInterceptor;
        this.allowedOriginPatterns = allowedOriginPatterns.split("\\s*,\\s*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/error");
    }
}
