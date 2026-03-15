package org.oyyj.userservice.Feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "ChatService")
public interface ChatFeign {

    /**
     * 新增粉丝
     * @param userId  关注者
     * @param followId  被关注者
     */
    @PutMapping("/myBlog/follow/fans")
    void addFansInfo(@RequestParam("userId")  String userId, @RequestParam("followId") String followId);

    /**
     *
     * @param userId 作者ID
     * @param behaviorId 操作者ID
     * @param targetId 操作目标ID
     * @param targetType  目标类别
     * @param behavior 操作行为
     */
    @PutMapping("/myBlog/follow/likeStar")
    void addLikeStar(@RequestParam("userId")  String userId,
                            @RequestParam("behaviorId") String behaviorId,
                            @RequestParam("targetId") String targetId,
                            @RequestParam("targetType")  Integer targetType,
                            @RequestParam("behavior") Integer behavior);

    // 删除信

    /**
     * 脱粉
     * @param userId
     * @param followId
     */
    @DeleteMapping("/myBlog/follow/unfollow")
    void unfollow(@RequestParam("userId")  String userId , @RequestParam("followId")  String followId);

    @DeleteMapping("/myBlog/follow/cancelLikeStar")
    void cancelLikeStar(@RequestParam("userId")  String userId,
                               @RequestParam("targetId") String targetId,
                               @RequestParam("targetType")  Integer targetType,
                               @RequestParam("behavior") Integer behavior);

}
