package com.xiaowei.weipicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "unsplash")
public class UnsplashConfig {
    /**
     * unsplash访问密钥
     */
    private String accessKey;
}
