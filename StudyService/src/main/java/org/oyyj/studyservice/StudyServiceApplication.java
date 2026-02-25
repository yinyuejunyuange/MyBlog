package org.oyyj.studyservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication

@EnableDiscoveryClient
@ComponentScan(basePackages = {"org.oyyj.studyservice", "org.oyyj.mycommon","org.oyyj.mycommonbase"})
@MapperScan({
		"org.oyyj.studyservice.mapper", // 本模块的Mapper包 会把所有接口都作为 mapper使用 注意甄别
		"org.oyyj.mycommon.mapper"    // 公共模块的Mapper包
})
public class StudyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudyServiceApplication.class, args);
	}

}
