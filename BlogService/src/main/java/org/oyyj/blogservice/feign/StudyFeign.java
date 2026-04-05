package org.oyyj.blogservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 学习/Ai相关接口
 */
@FeignClient(value="StudyService")
public interface StudyFeign {

    @GetMapping("/myBlog/agentInfo/admin/isCommentToxic")
    Boolean isCommentToxic(@RequestParam("type")Integer type,
                           @RequestParam("id")Long id,
                           @RequestParam("comment")String comment);


    /**
     * 判断评论是否正确
     * @param type
     * @param id
     * @param comment
     */
    @PutMapping("/myBlog/agent/comment/isToxic")
    void commentIsToxic( @RequestParam("type")Integer type,
                         @RequestParam("id")Long id,
                         @RequestParam("comment")String comment);

}
