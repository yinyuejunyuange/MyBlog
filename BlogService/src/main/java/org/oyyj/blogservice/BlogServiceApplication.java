package org.oyyj.blogservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients(value="org.oyyj.blogservice.feign")
@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
@ComponentScan(basePackages = {"org.oyyj.blogservice", "org.oyyj.mycommon","org.oyyj.mycommonbase"})
@MapperScan({
        "org.oyyj.blogservice.mapper", // 本模块的Mapper包
        "org.oyyj.blogservice.config.mapper", // 本模块的Mapper包  会把所有接口都作为 mapper使用 注意甄别
        "org.oyyj.mycommon.mapper"    // 公共模块的Mapper包
})
public class BlogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogServiceApplication.class, args);
    }

}
