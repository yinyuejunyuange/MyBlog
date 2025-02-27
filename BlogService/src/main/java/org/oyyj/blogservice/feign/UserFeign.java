package org.oyyj.blogservice.feign;

import org.oyyj.blogservice.config.FeignUserConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(value = "UserService" ,configuration = FeignUserConfiguration.class)
public interface UserFeign {
    @GetMapping("/myBlog/user/getUserName")
    Map<Long,String> getUserNameMap();

    @GetMapping("/myBlog/user/blog/isUserKudos")
    Boolean isUserKudos(@RequestParam("blogId") Long blogId,@RequestParam("userInfoKey") String userInfoKey);

    @GetMapping("/myBlog/user/blog/isUserStar")
    Boolean isUserStar(@RequestParam("blogId") Long blogId,@RequestParam("userInfoKey")String userInfoKey);
}
