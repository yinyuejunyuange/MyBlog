package org.oyyj.adminservice.feign;

import jakarta.servlet.http.HttpServletRequest;
import org.oyyj.adminservice.config.FeignRequestConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@FeignClient(value = "BlogService",configuration = FeignRequestConfig.class)
public interface BlogFeign {

    @GetMapping("/blog/getIncreaseBlog")
    Map<String,Long> getIncreaseBlog();

    @GetMapping("/blog/getAllTypeNum")
    Map<String,Long> getAllTypeNum();

    @GetMapping("/blog/getAllMessage")
    Long getAllMessage();

    @GetMapping("/blog/getBlogNum")
    Long getBlogNum();

    @GetMapping("/blog/getBlogListByAdmin") // 将json传递过去
    String getBlogListAdmin(@RequestParam(value = "blogName",required = false) String blogName,
                                   @RequestParam(value = "authorName",required = false) String authorName,
                                   @RequestParam(value = "startDate",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
                                   @RequestParam(value = "endDate",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,  // spring框架默认是不支持前端 date的iso类型  所以要设置
                                   @RequestParam(value = "status",required = false) String status,
                                   @RequestParam(value = "currentPage") Integer currentPage);
    @PutMapping("/blog/updateBlogStatus")
    Boolean updateBlogStatus(@RequestBody Map<String,Object> map);

    @DeleteMapping("/blog/deleteBlog")
    Boolean deleteBlog(@RequestParam("blogId") Long blogId);
}


