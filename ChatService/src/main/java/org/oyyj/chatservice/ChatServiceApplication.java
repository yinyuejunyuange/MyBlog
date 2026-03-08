package org.oyyj.chatservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients(value="org.oyyj.chatservice.feign")
@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
@ComponentScan(basePackages = {"org.oyyj.chatservice", "org.oyyj.mycommon","org.oyyj.mycommonbase"})
@MapperScan({
        "org.oyyj.mycommon.mapper",    // 公共模块的Mapper包
        "org.oyyj.chatservice.mapper"
})
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }

}
