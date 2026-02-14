package org.oyyj.userservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import feign.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommon.annotation.SourceCheck;
import org.oyyj.mycommonbase.common.ServiceItems;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.userservice.dto.*;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.pojo.*;
import org.oyyj.userservice.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/myBlog/user/blog")
@Slf4j
public class UserBlogController {
    @Autowired
    private IUserStarService userStarService;

    @Autowired
    private IUserKudosService userKudosService;

    @Autowired
    private BlogFeign blogFeign;

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private IUserService userService;

    @Autowired
    private IUserCommentService userCommentService;

    @Autowired
    private IUserReplyService userReplyService;



    // 下载图片
    @SourceCheck(allowService = {ServiceItems.BLOG_SERVICE, ServiceItems.GATE_SERVICE})
    @GetMapping("/file/download/{fileName}")
    public void downloadFile(@PathVariable("fileName")String fileName, HttpServletResponse response){
        //userService.downloadFile(fileName,response);
        try {
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+fileName+"\"");

            OutputStream outputStream=response.getOutputStream(); // 因为 服务中 获取图片没有返回值 所以要把文件保留下来
            Response res=blogFeign.getFile(fileName);
            InputStream is=res.body().asInputStream(); // 获取响应的输入流
            IOUtils.copy(is,outputStream); // 调用 IOUtils方法 将输入流 复制到 输出流 以此返回前端
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 判断当前用户是否点赞
    @GetMapping("/isUserKudos")
    public boolean isUserKudos(@RequestParam("blogId") Long blogId,@RequestParam("userId") String userId)  {

        UserKudos one = userKudosService.getOne(Wrappers.<UserKudos>lambdaQuery().eq(UserKudos::getUserId, userId)
                .eq(UserKudos::getBlogId, blogId));
        return Objects.nonNull(one);
    }

    // 判断当前用户是否收藏
    @GetMapping("/isUserStar")
    public Boolean isUserStar(@RequestParam("blogId") Long blogId,@RequestParam("userId")Long userId,HttpServletRequest request){
        try {
            String source = request.getHeader("source");
            if(source==null||!source.equals("BLOGSERVICE")){
                throw new AuthenticationException("请求来源不正确");
            }

            UserStar one = userStarService.getOne(Wrappers.<UserStar>lambdaQuery().eq(UserStar::getUserId, userId)
                    .eq(UserStar::getBlogId, blogId));
            if(Objects.isNull(one)){
                return false;
            }

            return true;
        } catch (AuthenticationException e) {
            log.error(e.getMessage());
            return false;
        }
    }


    // 判断用户是否点赞评论
    @GetMapping("/getUserKudosComment")
    public Boolean getUserKudosComment(@RequestParam("commentId")String commentId,@RequestParam("userId")Long userId,HttpServletRequest request){
        
        // 判断 用户是否点赞此评论
        UserComment one = userCommentService.getOne(Wrappers.<UserComment>lambdaQuery()
                .eq(UserComment::getCommentId, Long.valueOf(commentId))
                .eq(UserComment::getUserId, userId)
        );

        return Objects.nonNull(one);
    }

    // 判断用户是否点赞回复
    @GetMapping("/getUserKudosReply")
    public Boolean getUserKudosReply(@RequestParam("replyId")String replyId,@RequestParam("userId")Long userId,HttpServletRequest request){
        try {
            String source = request.getHeader("source");
            if(source==null||!source.equals("BLOGSERVICE")){
                throw new AuthenticationException("请求来源不正确");
            }

            // 判断 用户是否点赞此评论
            UserReply one = userReplyService.getOne(Wrappers.<UserReply>lambdaQuery()
                    .eq(UserReply::getReplyId, Long.valueOf(replyId))
                    .eq(UserReply::getUserId, userId)
            );

            return !Objects.isNull(one);
        } catch (AuthenticationException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/isUserLikeComments")
    public List<Long> isUserLikeComments( @RequestParam("commentIds")List<Long> commentIds, @RequestParam("userId")Long userId,HttpServletRequest request ){
        return userCommentService.isUserLikeComment(commentIds,userId);
    }

    @GetMapping("/isUserLikeReply")
    public List<Long> isUserLikeReply( @RequestParam("replyIds") List<Long> replyIds, @RequestParam("userId") Long userId,HttpServletRequest request ){
        return userReplyService.isUserLikeReply(replyIds,userId);
    }

    // 用户点赞评论
    @PutMapping("/kudosComment")
    public Boolean kudosComment(@RequestParam("commentId")String commentId,@RequestParam("userId") Long userId){
        UserComment one = userCommentService.getOne(Wrappers.<UserComment>lambdaQuery()
                .eq(UserComment::getCommentId, Long.valueOf(commentId))
                .eq(UserComment::getUserId, userId)
        );
        if(Objects.nonNull(one)){
            log.warn("用户{} 重复点赞评论{}",userId,commentId);
            return true;
        }
        one = new UserComment();
        one.setCommentId(Long.valueOf(commentId));
        one.setUserId(userId);
        return userCommentService.save(one);
    }

    @PutMapping("/cancelKudosComment")
    public Boolean cancelKudosComment(@RequestParam("commentId")String commentId,@RequestParam("userId") Long userId){
        UserComment one = userCommentService.getOne(Wrappers.<UserComment>lambdaQuery()
                .eq(UserComment::getCommentId, Long.valueOf(commentId))
                .eq(UserComment::getUserId, userId)
        );
        if(Objects.isNull(one)){
            log.warn("用户{} 重复取消点赞评论{}",userId,commentId);
            return true;
        }
        return userCommentService.remove(Wrappers.<UserComment>lambdaQuery()
                .eq(UserComment::getCommentId, Long.valueOf(commentId))
                .eq(UserComment::getUserId, userId)
        );
    }

    /**
     * 用户点赞博客
     * @param blogId
     * @param userId
     * @return
     */
    @PutMapping("/kudosBlog")
    public Boolean kudosBlog(@RequestParam("blogId")Long blogId,@RequestParam("userId") Long userId){
        UserKudos one = userKudosService.getOne(Wrappers.<UserKudos>lambdaQuery()
                .eq(UserKudos::getBlogId, blogId)
                .eq(UserKudos::getUserId, userId)
        );
        if(one != null){
            log.warn("用户重复点赞博客{} 用户ID{}",blogId,userId);
            return true;
        }
        UserKudos userKudos = new UserKudos();
        userKudos.setBlogId(blogId);
        userKudos.setUserId(userId);
        return userKudosService.save(userKudos);
    }

    /**
     * 用户取消点赞博客
     * @param blogId
     * @param userId
     * @return
     */
    @PutMapping("/cancelKudosBlog")
    public Boolean cancelKudosBlog(@RequestParam("blogId")Long blogId,@RequestParam("userId") Long userId){
        UserKudos one = userKudosService.getOne(Wrappers.<UserKudos>lambdaQuery()
                .eq(UserKudos::getBlogId, blogId)
                .eq(UserKudos::getUserId, userId)
        );
        if(one == null){
            log.warn("用户重复取消点赞博客{} 用户ID{}",blogId,userId);
            return true;
        }
        return userKudosService.remove(Wrappers.<UserKudos>lambdaQuery()
            .eq(UserKudos::getBlogId, blogId)
            .eq(UserKudos::getUserId, userId)
        );
    }


    // 用户取消收藏
    @PutMapping("/cancelStar")
    public Boolean cancelStar(@RequestParam("blogId") String blogId ,@RequestParam("userId") Long userId){
        return userStarService.remove(Wrappers.<UserStar>lambdaQuery()
                .eq(UserStar::getUserId, userId)
                .eq(UserStar::getBlogId, Long.valueOf(blogId))
        );
    }


    // 用户点赞回复
    @PutMapping("/kudosReply")
    public Boolean kudosReply(@RequestParam("replyId")String replyId, @RequestParam("userId") Long userId){

        UserReply one = userReplyService.getOne(Wrappers.<UserReply>lambdaQuery()
                .eq(UserReply::getReplyId, Long.valueOf(replyId))
                .eq(UserReply::getUserId, userId)
        );
        if(Objects.nonNull(one)){
            log.warn("用户{} 重复点赞评论{}",userId,replyId);
            return true;
        }
        one = new UserReply();
        one.setReplyId(Long.valueOf(replyId));
        one.setUserId(userId);
        return userReplyService.save(one);
    }

    // 用户取消点赞回复
    @PutMapping("/cancelKudosReply")
    public Boolean cancelKudosReply(@RequestParam("replyId")String replyId, @RequestParam("userId") Long userId){

        UserReply one = userReplyService.getOne(Wrappers.<UserReply>lambdaQuery()
                .eq(UserReply::getReplyId, Long.valueOf(replyId))
                .eq(UserReply::getUserId, userId)
        );
        if(Objects.isNull(one)){
            log.warn("用户{} 重复取消点赞回复{}",userId,replyId);
            return true;
        }
        one = new UserReply();
        one.setReplyId(Long.valueOf(replyId));
        one.setUserId(userId);
        return userReplyService.remove(Wrappers.<UserReply>lambdaQuery()
                .eq(UserReply::getReplyId, Long.valueOf(replyId))
                .eq(UserReply::getUserId, userId)
        );
    }


    // 用户收藏
    @PutMapping("/userStar")
    public Boolean userStar(@RequestParam("blogId") String blogId ,@RequestParam("userId") Long userId) throws Exception {
        // 用户收藏表中添加对应的用户博客id；
        return  userService.userStar(blogId,userId);
    }

}


