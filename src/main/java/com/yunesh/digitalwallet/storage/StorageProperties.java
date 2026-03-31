package com.yunesh.digitalwallet.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    private String supabaseUrl;
    private String supabaseKey;
    private String bucket;
    private String localUploadDir;
}