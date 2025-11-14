package org.example.storyreading.storyservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    private final String publicDir;

    public WebConfig(@Value("${storage.public-dir:public}") String publicDir) {
        this.publicDir = publicDir;
        log.info("WebConfig serving static files from: {}", publicDir);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files from configured public directory via /public/**
        String location = "file:" + publicDir + "/";
        registry.addResourceHandler("/public/**")
                .addResourceLocations(location);
        log.info("Added resource handler /public/** -> {}", location);
    }
}
