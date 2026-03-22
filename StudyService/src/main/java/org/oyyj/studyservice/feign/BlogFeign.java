package org.oyyj.studyservice.feign;

import org.oyyj.mycommon.pojo.dto.comment.CommentToxicDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "BlogService")
public interface BlogFeign {

    @PutMapping("/myBlog/commentFeign/toxicJudgement")
    void toxicJudgement(@RequestBody CommentToxicDTO commentToxicDTO);

}
