package com.xiaowei.weipicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xiaowei.weipicturebackend.mapper")
public class WeiPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeiPictureBackendApplication.class, args);
    }

}
