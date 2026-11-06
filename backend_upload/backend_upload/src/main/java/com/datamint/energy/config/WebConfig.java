package com.datamint.energy.config;

import com.datamint.energy.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor auth;
    public WebConfig(AuthInterceptor auth) { this.auth = auth; }

    @Override public void addInterceptors(InterceptorRegistry r) { r.addInterceptor(auth).addPathPatterns("/api/**"); }

    /** Allows the Android app and any other client to call the API. */
    @Override public void addCorsMappings(CorsRegistry r) {
        r.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedHeaders("*");
    }
}
