package org.oyyj.taskservice.feign;

import jakarta.servlet.http.HttpServletRequest;
import org.oyyj.taskservice.config.FeignUserConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

@FeignClient(value = "UserService",configuration = FeignUserConfiguration.class)
public interface UserFeign {

    @GetMapping("/myBlog/user/isUserExist")
    Date isUserExist(@RequestParam("userId")Long userId );
}
