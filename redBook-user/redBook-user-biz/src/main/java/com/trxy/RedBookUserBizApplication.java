package com.trxy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.trxy.mapper")
@EnableFeignClients("com.trxy")
public class RedBookUserBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedBookUserBizApplication.class, args);
    }

}
