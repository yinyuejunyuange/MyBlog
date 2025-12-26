package org.oyyj.blogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients(value="org.oyyj.blogservice.feign")
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"org.oyyj.blogservice", "org.oyyj.mycommon","org.oyyj.mycommonbase"})
public class BlogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogServiceApplication.class, args);
    }

}
