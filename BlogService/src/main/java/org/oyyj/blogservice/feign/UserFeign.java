package org.oyyj.blogservice.feign;

import jakarta.servlet.http.HttpServletRequest;
import org.oyyj.blogservice.config.FeignUserConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(value = "UserService" ,configuration = FeignUserConfiguration.class)
public interface UserFeign {
    @GetMapping("/myBlog/user/getUserName")
    Map<Long,String> getUserNameMap();

    @GetMapping("/myBlog/user/blog/isUserKudos")
    Boolean isUserKudos(@RequestParam("blogId") Long blogId,@RequestParam("userId") String userInfoKey);

    @GetMapping("/myBlog/user/blog/isUserStar")
    Boolean isUserStar(@RequestParam("blogId") Long blogId,@RequestParam("userId")Long userId);

    @GetMapping("/myBlog/user/getNameInIds")
    Map<Long,String> getNameInIds(@RequestParam("ids")List<String> ids);

    @GetMapping("/myBlog/user/getImageInIds")
    Map<Long,String> getImageInIds(@RequestParam("ids")List<String> ids);

    @GetMapping("/myBlog/user/blog/getUserKudosComment")
    Boolean getUserKudosComment(@RequestParam("commentId")String commentId, @RequestParam("userId")Long userId);

    @GetMapping("/myBlog/user/blog/getUserKudosReply")
    Boolean getUserKudosReply(@RequestParam("replyId")String replyId,@RequestParam("userId")Long userId);

    @GetMapping("/myBlog/user/getUserIdByName")
    String getUserIdByName(@RequestParam("userName") String userName);

    @GetMapping("/myBlog/user/getIdsLikeName")
    List<Long> getIdsLikeName(@RequestParam("name") String name);

    @GetMapping("/myBlog/user/blog/isUserLikeComments")
    List<Long> isUserLikeComments(@RequestParam("commentIds")List<Long> commentIds, @RequestParam("userId")Long userId);

    @GetMapping("/myBlog/user/blog/isUserLikeReply")
    List<Long> isUserLikeReply(@RequestParam("replyIds") List<Long> replyIds, @RequestParam("userId") Long userId);


    /**
     * 用户点赞评论
     */
    @PutMapping("/myBlog/user/blog/kudosComment")
    Boolean kudosComment(@RequestParam("commentId") String commentId, @RequestParam("userId") Long userId);

    /**
     * 用户取消点赞评论
     */
    @PutMapping("/myBlog/user/blog/cancelKudosComment")
    Boolean cancelKudosComment(@RequestParam("commentId") String commentId, @RequestParam("userId") Long userId);


    // ===================== 评论回复点赞 =====================

    /**
     * 用户点赞回复
     */
    @PutMapping("/myBlog/user/blog/kudosReply")
    Boolean kudosReply(@RequestParam("replyId") String replyId, @RequestParam("userId") Long userId);


    // ===================== 博客收藏 =====================

    /**
     * 用户收藏博客
     */
    @PutMapping("/myBlog/user/blog/userStar")
    Boolean userStar(@RequestParam("blogId") String blogId,@RequestParam("userId") Long userId);

    /**
     * 用户取消收藏博客
     */
    @PutMapping("/myBlog/user/blog/cancelStar")
    Boolean cancelStar(@RequestParam("blogId") String blogId,@RequestParam("userId") Long userId);



}
