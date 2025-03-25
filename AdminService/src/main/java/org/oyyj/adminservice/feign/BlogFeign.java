package org.oyyj.adminservice.feign;

import jakarta.servlet.http.HttpServletRequest;
import org.oyyj.adminservice.config.FeignRequestConfig;
import org.oyyj.adminservice.dto.PageDTO;
import org.oyyj.adminservice.vo.CommentAdminVO;
import org.oyyj.adminservice.vo.ReplyAdminVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
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

    @GetMapping("/blog/getCommentForAdmin")
    PageDTO<CommentAdminVO> getCommentForAdmin( @RequestParam(value = "blogName",required = false) String blogName,
                                                @RequestParam(value = "userName",required = false)String userName,
                                                @RequestParam(value = "startTime",required = false)  Date startTime,
                                                @RequestParam(value = "endTime",required = false)  Date endTime,
                                                @RequestParam(value = "isVisible",required = false)Integer isVisible,
                                                @RequestParam("currentPage") Integer currentPage);
    @PutMapping("/blog/changeCommentStatus")
    Map<String,Object> changeCommentStatus(@RequestParam("commentId") String commentId,
                                                  @RequestParam("isVisible") Integer isVisible);

    @GetMapping("/blog/getReplyForAdmin")
    PageDTO<ReplyAdminVO> getReplyForAdmin(@RequestParam(value = "blogName",required = false) String blogName, //
                                                  @RequestParam(value = "userName",required = false)String userName,
                                                  @RequestParam(value = "comment",required = false)String comment, // 被回复的内容
                                                  @RequestParam(value = "startTime",required = false)  Date startTime,
                                                  @RequestParam(value = "endTime",required = false)  Date endTime,
                                                  @RequestParam(value = "isVisible",required = false)Integer isVisible,
                                                  @RequestParam("currentPage") Integer currentPage);

    @PutMapping("/blog/changeReplyStatus")
    Map<String,Object> changeReplyStatus(@RequestParam("replyId") String replyId,
                                                @RequestParam("isVisible") Integer isVisible);

    @DeleteMapping("/blog/deleteComment")
    Map<String,Object> deleteComment(@RequestParam("commentId") Long commentId);

    @DeleteMapping("/blog/deleteReply")
    Map<String,Object> deleteReply(@RequestParam("replyId") Long replyId);
}


