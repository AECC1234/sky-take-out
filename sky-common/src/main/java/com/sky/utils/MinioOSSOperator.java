package com.sky.utils;

import io.minio.MinioClient;
import io.minio.admin.MinioAdminClient;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sky.minio")
public class MinioOSSOperator {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String externalHost;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public MinioAdminClient minioAdminClient() {
        return MinioAdminClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
