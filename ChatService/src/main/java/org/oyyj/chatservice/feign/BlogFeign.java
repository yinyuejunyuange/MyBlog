package org.oyyj.chatservice.feign;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@FeignClient(value = "BlogService")
public interface BlogFeign {

    /**
     * 获取用户的所有收藏
     * @param ids
     * @return
     */
    @PostMapping("/myBlog/blogFeign/titlesByStrIds")
    Map<Long,String> blogTitleByStrIds(@RequestBody List<String> ids);

    /**
     * 获取用户的所有收藏
     * @param ids
     * @return
     */
    @PostMapping("/myBlog/commentFeign/commentsByStrIds")
    Map<Long,String> commentsByStrIds(@RequestBody List<String> ids);

    /**
     * 获取用户的所有收藏
     * @param ids
     * @return
     */
    @PostMapping("/myBlog/commentFeign/replyByStrIds")
    Map<Long,String> replyByStrIds(@RequestBody List<String> ids);

}
