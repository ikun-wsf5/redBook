package com.trxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "storage.aliyun.oss")
@Component
@Data
public class AliOssProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
