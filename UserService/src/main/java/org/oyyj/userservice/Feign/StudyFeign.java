package org.oyyj.userservice.Feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 调用学习相关服务
 */
@FeignClient(value = "StudyService")
public interface StudyFeign {

    @GetMapping("/myBlog/knowledgePoint/admin/totalCount")
    Long totalCount();

}
