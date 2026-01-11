package org.oyyj.mycommon.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    /**
     * minio服务地址
     */
    private String endpoint;
    /**
     * 账号
     */
    private String accessKey;
    /**
     * 密钥
     */
    private String accessSecret;
    /**
     * 图片存储桶
     */
    private String imageBucketName;
    /**
     * 文件存储桶
     */
    private String documentBucketName;
    /**
     * 单独上传最大文件
     */
    private Integer maxFileSize;
    /**
     * 分片上传 大小
     */
    private Integer partFileSize;


    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey,accessSecret)
                .build();
    }
}



