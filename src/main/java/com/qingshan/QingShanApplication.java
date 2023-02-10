package com.qingshan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.qingshan.mapper")
@SpringBootApplication
public class QingShanApplication {

    public static void main(String[] args) {
        SpringApplication.run(QingShanApplication.class, args);
    }

}
