package org.oyyj.adminservice.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @RequestMapping("/01")
    public void test(){
        System.out.println("输出test");
    }
}
