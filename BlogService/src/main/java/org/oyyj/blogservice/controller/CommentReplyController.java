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
import org.oyyj.blogservice.util.PyApiUtil;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.blogservice.vo.commet.CommentResultVO;
import org.oyyj.blogservice.vo.reply.ReplyResultVO;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    @Autowired
    private PyApiUtil pyApiUtil;



    /**
     * 添加评论
     * @param blogId
     * @param context
     * @param loginUser
     * @return
     */
    @PutMapping("/writeComment")
    @Transactional
    public ResultUtil<Long> writeComment(@RequestParam("blogId")Long blogId,
                             @RequestParam("context")String context,
                             @RequestUser() LoginUser loginUser){
        Date date = new Date();
        Map<Long, String> imageInIds = userFeign.getImageInIds(Collections.singletonList(String.valueOf(loginUser.getUserId())));
        String userImage = null;
        if(imageInIds != null && imageInIds.containsKey(loginUser.getUserId())){
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
            pyApiUtil.getCommentToxicPredict(context,build.getId(),1);
            blogService.blogComment(blogId,loginUser);
            return ResultUtil.success(blogId);
        }else{
            log.warn("数据库添加评论添加失败");
            return ResultUtil.fail("查询失败添加失败");
        }
    }
    // 删除评论
    @DeleteMapping("/removeComment")
    public ResultUtil<Boolean> removeComment(@RequestParam("commentId")Long commentId){
        boolean remove = commentService.remove(Wrappers.<Comment>lambdaQuery().eq(Comment::getId, commentId));
        return remove? ResultUtil.success(remove) : ResultUtil.fail("评论删除失败");

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
    public ResultUtil<Long> replyComment(@RequestUser() LoginUser loginUser,
                             @RequestParam(value = "repliedUserId", required = false) Long repliedUserId,
                             @RequestParam("commentId")Long commentId,
                             @RequestParam("context")String context){
        Date date=new Date();
        // todo 修改成从一个用户处获取ID
        List<String> userIds = new ArrayList<>();
        userIds.add(String.valueOf(loginUser.getUserId()));
        if(repliedUserId != null){
            userIds.add(String.valueOf(repliedUserId));
        }
        Map<Long, String> imageInIds = userFeign.getImageInIds(userIds);
        String userImage = null;
        if(imageInIds != null && imageInIds.containsKey(loginUser.getUserId())){
            userImage = imageInIds.get(loginUser.getUserId());
        }
        Map<Long, String> nameInIds = userFeign.getNameInIds(userIds);
        String repliedName = null;
        if(nameInIds != null && repliedUserId !=null && nameInIds.containsKey(repliedUserId)){
            repliedName= nameInIds.get(repliedUserId);
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
            pyApiUtil.getCommentToxicPredict(context,build.getId(),0);
            return ResultUtil.success(build.getId());
        }else{
            log.error("数据库添加回复失败！");
            return ResultUtil.fail("回复失败");
        }
    }
    // 删除回复
    @DeleteMapping("/removeReply")
    public ResultUtil<Boolean> removeReply(@RequestParam("replyId")Long replyId ,@RequestUser LoginUser loginUser){
        Reply one = replyService.getOne(Wrappers.<Reply>lambdaQuery()
                .eq(Reply::getId, replyId)
                .select(Reply::getUserId)
        );
        if( one == null ||  !one.getUserId().equals(loginUser.getUserId())){
            return ResultUtil.fail("回复不可删除");
        }
        boolean remove = replyService.remove(Wrappers.<Reply>lambdaQuery().eq(Reply::getId, replyId));
        return remove? ResultUtil.success(remove) :  ResultUtil.fail("删除回复失败");
    }

    // 获得回复

    /**
     *
     * @param blogId
     * @param loginUser
     * @param lastTime 游标 （上次查询最后的时间）
     * @param lastId 游标 （上次查询最后的ID）
     * @return
     */
    @GetMapping("/getComment")
    public ResultUtil<CommentResultVO> getComment(@RequestParam("blogId")String blogId,
                                                  @RequestUser(required = false) LoginUser loginUser,
                                                  @RequestParam(value = "lastTime",required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                      Date lastTime,
                                                  @RequestParam(value = "lastId",required = false)String lastId){
        return ResultUtil.success(commentService.getBlogComment(blogId,loginUser,lastTime,lastId));
    }
    // 获得回复
    @GetMapping("/getReply")
    public ResultUtil<ReplyResultVO> getReply(@RequestParam("commentId")Long commentId,
                                              @RequestUser(required = false) LoginUser loginUser,
                                              @RequestParam(value = "lastId",required = false)String lastId,
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                              @RequestParam(value = "lastTime",required = false) Date lastTime
                                       ){
        return ResultUtil.success(replyService.getReply(commentId, loginUser, lastId,lastTime));
    }

    // 改变评论点赞数
    @PutMapping("/changCommentKudos")
    public ResultUtil<Boolean> changCommentKudos(@RequestParam("commentId")Long commentId,
                                                 @RequestParam("isAdd") Integer isAdd,
                                                 @RequestUser LoginUser loginUser ){
        Boolean change = commentService.changeCommentKudos(commentId, isAdd,loginUser);
        return change?ResultUtil.success(change): ResultUtil.fail("评论修改失败");
    }


    // 改变回复点赞数
    @PutMapping("/changReplyKudos")
    public ResultUtil<Boolean> changReplyKudos(@RequestParam("replyId")Long replyId,
                                               @RequestParam("isAdd") Integer isAdd,
                                               @RequestUser LoginUser loginUser){
        Boolean change = replyService.changeReplyKudos(replyId, isAdd,loginUser);
        return change?ResultUtil.success(change): ResultUtil.fail("回复修改失败");
    }

}
