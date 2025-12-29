package org.oyyj.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableFeignClients(value = "org.oyyj.userservice.Feign")
@SpringBootApplication
@EnableAsync// 开启异步注解
@ComponentScan(basePackages = {"org.oyyj.userservice", "org.oyyj.mycommon","org.oyyj.mycommonbase"})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
