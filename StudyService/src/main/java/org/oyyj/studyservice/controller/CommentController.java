package org.oyyj.studyservice.controller;

import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgePointComment.CommentAddDTO;
import org.oyyj.studyservice.dto.knowledgePointComment.ReplyAddDTO;
import org.oyyj.studyservice.pojo.KnowledgePointComment;
import org.oyyj.studyservice.service.KnowledgePointCommentService;
import org.oyyj.studyservice.utils.ParamTypeUtil;
import org.oyyj.studyservice.vo.knowledgeComment.KnowledgeCommentVO;
import org.oyyj.studyservice.vo.knowledgeComment.KnowledgeReplyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/myBlog/knowledge/comment")
public class CommentController {

    @Autowired
    private KnowledgePointCommentService commentService;


    @PostMapping("/add")
    public ResultUtil<String> addComment(@RequestBody CommentAddDTO comment, @RequestUser LoginUser loginUser) {

        boolean success = commentService.addComment(ParamTypeUtil.toLong(comment.getKnowledgeId()),
                comment.getContent(), loginUser);

        return success
                ? ResultUtil.success("评论成功")
                : ResultUtil.fail("评论失败");
    }


    @PostMapping("/reply")
    public ResultUtil<String> addReply(
            @RequestBody ReplyAddDTO comment,
            @RequestUser LoginUser loginUser) {

        boolean success = commentService.addReply(ParamTypeUtil.toLong(comment.getKnowledgeId()),
                ParamTypeUtil.toLong(comment.getRootId()),
                ParamTypeUtil.toLong(comment.getParentId()),
                ParamTypeUtil.toLong(comment.getReplyUserId()),
                comment.getReplyUserName(),
                comment.getContent(),
                loginUser);

        return success
                ? ResultUtil.success("回复成功")
                : ResultUtil.fail("回复失败");
    }


    @GetMapping("/list")
    public ResultUtil<KnowledgeCommentVO> getComment(
            @RequestParam("knowledgePointId") String knowledgePointId,
            @RequestParam(required = false,value = "lastCommentId") String lastCommentId,
            @RequestUser(required = false) LoginUser loginUser) {

        return commentService.getComment(ParamTypeUtil.toLong(knowledgePointId), ParamTypeUtil.toLong(lastCommentId),loginUser);
    }


    @GetMapping("/reply/list")
    public ResultUtil<KnowledgeReplyVO> getReply(
            @RequestParam("parentId") String parentId,
            @RequestParam(required = false,value = "lastReplyId") String lastReplyId,
            @RequestUser(required = false) LoginUser loginUser) {

        return commentService.getReply(ParamTypeUtil.toLong(parentId) , ParamTypeUtil.toLong(lastReplyId), loginUser);
    }

    /**
     * ===============================
     * 删除评论
     * ===============================
     */
    @DeleteMapping("/delete/{commentId}")
    public ResultUtil<String> deleteComment(
            @PathVariable String commentId,
            @RequestUser LoginUser loginUser) {

        return commentService.deleteComment(ParamTypeUtil.toLong(commentId), loginUser);
    }
}