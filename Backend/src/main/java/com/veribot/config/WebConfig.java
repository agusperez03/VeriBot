package com.veribot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
        		.allowedOriginPatterns("*") // permite todos, con compatibilidad con allowCredentials
                .allowedMethods("POST")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}