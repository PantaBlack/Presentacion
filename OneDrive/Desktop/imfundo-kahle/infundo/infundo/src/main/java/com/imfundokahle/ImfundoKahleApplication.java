package com.imfundokahle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
@EnableScheduling
public class ImfundoKahleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImfundoKahleApplication.class, args);
    }
}
