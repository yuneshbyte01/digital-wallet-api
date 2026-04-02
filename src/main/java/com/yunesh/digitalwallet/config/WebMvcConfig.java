package com.yunesh.digitalwallet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/profile-photos/**")
                .addResourceLocations("file:uploads/profile-photos/");
        registry.addResourceHandler("/kyc-documents/**")
                .addResourceLocations("file:uploads/kyc-documents/");
    }
}