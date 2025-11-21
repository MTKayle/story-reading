package org.example.storyreading.userservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${storage.public-dir:public}")
    private String publicDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve public directory - always resolve from user-service directory
        Path publicPath;
        Path relativePath = Paths.get(publicDir);
        if (relativePath.isAbsolute()) {
            publicPath = relativePath;
        } else {
            // Get the directory where the JAR/classes are running from
            Path currentDir = Paths.get("").toAbsolutePath();
            log.info("Current working directory: {}", currentDir);
            
            // If running from user-service directory, go up one level to services, then to public
            if (currentDir.endsWith("user-service")) {
                // Go up to services directory, then resolve publicDir (which is ../public)
                publicPath = currentDir.getParent().resolve("public").normalize();
            } else if (currentDir.endsWith("services")) {
                // Already in services directory
                publicPath = currentDir.resolve("public").normalize();
            } else {
                // Try to resolve from current directory
                publicPath = currentDir.resolve(publicDir).normalize();
            }
        }
        
        Path avatarsPath = publicPath.resolve("avatars");
        
        // Ensure directory exists
        try {
            if (!Files.exists(avatarsPath)) {
                Files.createDirectories(avatarsPath);
                log.info("Created avatars directory: {}", avatarsPath);
            }
        } catch (Exception e) {
            log.error("Failed to create avatars directory: {}", e.getMessage(), e);
        }
        
        String avatarsPathStr = avatarsPath.toAbsolutePath().toUri().toString();
        
        // Ensure path ends with / for directory
        if (!avatarsPathStr.endsWith("/")) {
            avatarsPathStr += "/";
        }
        
        registry.addResourceHandler("/public/avatars/**")
                .addResourceLocations("file:" + avatarsPathStr)
                .setCachePeriod(3600); // Cache for 1 hour
        
        log.info("Added resource handler /public/avatars/** -> {}", avatarsPathStr);
        log.info("Avatars directory exists: {}", Files.exists(avatarsPath));
        log.info("Avatars directory path: {}", avatarsPath.toAbsolutePath());
    }
}

