package com.trxy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.trxy.mapper")
@EnableFeignClients(basePackages = "com.trxy")
public class RedBookNoteBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedBookNoteBizApplication.class, args);
    }

}