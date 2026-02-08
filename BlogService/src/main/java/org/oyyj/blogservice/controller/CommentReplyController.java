package org.oyyj.blogservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.dto.ReadCommentDTO;
import org.oyyj.blogservice.dto.ReadReplyDTO;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.service.ICommentService;
import org.oyyj.blogservice.service.IReplyReportService;
import org.oyyj.blogservice.service.IReplyService;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/myBlog/blog/comment")
@Slf4j
public class CommentReplyController {

    @Autowired
    private ICommentService commentService;

    @Autowired
    private IBlogService blogService;

    @Autowired
    private IReplyService replyService;

    @Autowired
    private UserFeign  userFeign;



    /**
     * 添加评论
     * @param blogId
     * @param context
     * @param loginUser
     * @return
     */
    @PutMapping("/writeComment")
    @Transactional
    public Long writeComment(@RequestParam("blogId")Long blogId,
                             @RequestParam("context")String context,
                             @RequestUser() LoginUser loginUser){
        Date date = new Date();
        Map<Long, String> imageInIds = userFeign.getImageInIds(Collections.singletonList(String.valueOf(loginUser.getUserId())));
        String userImage = null;
        if(imageInIds != null && !imageInIds.containsKey(loginUser.getUserId())){
            userImage = imageInIds.get(loginUser.getUserId());
        }
        Comment build = Comment.builder()
                .blogId(blogId)
                .userId(loginUser.getUserId())
                .userName(loginUser.getUserName())
                .userImage(userImage)
                .context(context)
                .createTime(date)
                .updateTime(date)
                .isDelete(0)
                .isVisible(0)
                .build();

        // 检查博客是否具有攻击性质

        boolean save = commentService.save(build);
        if(save){
            // 增加博客的评论数
            blogService.blogComment(blogId,loginUser);
            return build.getId();
        }else{
            log.warn("评论添加失败");
            return null;
        }
    }
    // 删除评论
    @DeleteMapping("/removeComment")
    public Boolean removeComment(@RequestParam("commentId")Long commentId){
        return commentService.remove(Wrappers.<Comment>lambdaQuery().eq(Comment::getId, commentId));

    }
    // 回复评论或者用户

    /**
     *
     * @param loginUser 登录者
     * @param repliedUserId 被回复者ID
     * @param commentId 评论ID
     * @param context 内容
     * @return
     */
    @PutMapping("/replyComment")
    public Long replyComment(@RequestUser() LoginUser loginUser,
                             @RequestParam(value = "repliedUserId", required = false) Long repliedUserId,
                             @RequestParam("commentId")Long commentId,
                             @RequestParam("context")String context){
        Date date=new Date();
        // todo 修改成从一个用户处获取ID
        Map<Long, String> imageInIds = userFeign.getImageInIds(Collections.singletonList(String.valueOf(loginUser.getUserId())));
        String userImage = null;
        if(imageInIds != null && !imageInIds.containsKey(loginUser.getUserId())){
            userImage = imageInIds.get(loginUser.getUserId());
        }
        Map<Long, String> nameInIds = userFeign.getNameInIds(Collections.singletonList(String.valueOf(loginUser.getUserId())));
        String repliedName = null;
        if(nameInIds != null && !nameInIds.containsKey(loginUser.getUserId())){
            repliedName= nameInIds.get(loginUser.getUserId());
        }
        Reply build = Reply.builder()
                .commentId(commentId)
                .userId(loginUser.getUserId())
                .userName(loginUser.getUserName())
                .userImage(userImage)
                .repliedId(repliedUserId)
                .repliedName(repliedName)
                .context(context)
                .kudos(0L)
                .createTime(date)
                .updateTime(date)
                .isDelete(0)
                .isVisible(0)
                .build();
        boolean save = replyService.save(build);
        if(save){
            return build.getId();
        }else{
            return null;
        }
    }
    // 删除回复
    @DeleteMapping("/removeReply")
    public Boolean removeReply(@RequestParam("replyId")Long replyId){
        return replyService.remove(Wrappers.<Reply>lambdaQuery().eq(Reply::getId, replyId));

    }

    // 获得回复
    @GetMapping("/getComment")
    public Map<String,Object> getComment(@RequestParam("blogId")String blogId,
                                         @RequestUser() LoginUser loginUser,
                                         @RequestParam("pageNum")Integer pageNum){
        List<ReadCommentDTO> blogComment = commentService.getBlogComment(blogId,loginUser,pageNum);
        return ResultUtil.successMap(blogComment,"评论查询成功");
    }
    // 获得回复
    @GetMapping("/getReply")
    public Map<String,Object> getReply(@RequestParam("commentId")Long commentId,
                                         @RequestUser() LoginUser loginUser,
                                         @RequestParam("pageNum")Integer pageNum){
        List<ReadReplyDTO> reply = replyService.getReply(commentId, loginUser, pageNum);
        return ResultUtil.successMap(reply,"评论查询成功");
    }

    // 改变评论点赞数
    @PutMapping("/changCommentKudos")
    public Boolean changCommentKudos(@RequestParam("commentId")Long commentId,@RequestParam("isAdd") Integer isAdd){
        return commentService.changeCommentKudos(commentId,isAdd);
    }


    // 改变回复点赞数
    @PutMapping("/changReplyKudos")
    public Boolean changReplyKudos(@RequestParam("replyId")Long replyId,@RequestParam("isAdd") Integer isAdd){
        return replyService.changeReplyKudos(replyId,isAdd);
    }

}
