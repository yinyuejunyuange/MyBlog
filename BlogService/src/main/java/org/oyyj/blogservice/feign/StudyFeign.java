package org.oyyj.blogservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 学习/Ai相关接口
 */
@FeignClient(value="StudyService")
public interface StudyFeign {

    @GetMapping("/myBlog/agentInfo/admin/isCommentToxic")
    Boolean isCommentToxic(Integer type, Long id, String comment);

}
