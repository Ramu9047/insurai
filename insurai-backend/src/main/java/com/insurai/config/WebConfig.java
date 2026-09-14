package com.insurai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final com.insurai.security.RateLimitInterceptor rateLimitInterceptor;
    private final com.insurai.security.CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebConfig(com.insurai.security.RateLimitInterceptor rateLimitInterceptor,
                     com.insurai.security.CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(@org.springframework.lang.NonNull java.util.List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addInterceptors(
            @org.springframework.lang.NonNull org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(java.util.Objects.requireNonNull(rateLimitInterceptor))
                .addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(@org.springframework.lang.NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
