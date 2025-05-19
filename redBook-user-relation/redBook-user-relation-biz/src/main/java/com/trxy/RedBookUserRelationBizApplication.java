package com.trxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.trxy")
public class RedBookUserRelationBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedBookUserRelationBizApplication.class, args);
    }
}
