package org.oyyj.gatewaydemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = {"org.oyyj.mycommonbase","org.oyyj.gatewaydemo"})
public class GatewayDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayDemoApplication.class, args);
    }

}
