package com.example.grameone_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration for the Grameone Admin API.
 *
 * Allowed origins:
 *   - Local dev:    http://localhost:4200, http://127.0.0.1:4200
 *   - Production:   https://grame-one-front-end.vercel.app
 *   - Render self:  https://grame-one-back-end.onrender.com
 *
 * Add new deployment URLs to the allowedOriginPatterns list and redeploy.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(
                                // ─── Local development ───────────────────────
                                "http://localhost:4200",
                                "http://localhost:3000",
                                "http://127.0.0.1:4200",
                                // ─── Production (Vercel) ─────────────────────
                                "https://grame-one-front-end.vercel.app",
                                // ─── Render backend self-referencing ─────────
                                "https://grame-one-back-end.onrender.com"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization", "Content-Disposition")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
