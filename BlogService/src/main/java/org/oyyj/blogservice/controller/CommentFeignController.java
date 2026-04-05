package org.oyyj.blogservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.blogservice.service.ICommentService;
import org.oyyj.blogservice.service.IReplyService;
import org.oyyj.mycommon.pojo.dto.comment.CommentToxicDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/myBlog/commentFeign")
public class CommentFeignController {

    @Autowired
    private ICommentService commentService;

    @Autowired
    private IReplyService replyService;

    /**
     * 获取用户的所有收藏
     * @param ids
     * @return
     */
    @PostMapping("/commentsByStrIds")
    public Map<Long,String> commentsByStrIds(@RequestBody List<String> ids){
        List<Long> list = ids.stream().map(Long::parseLong).toList();
        if(list.isEmpty()){
            return Map.of();
        }

        return commentService.list(Wrappers.<Comment>lambdaQuery()
                .in(Comment::getId, list)
        ).stream().collect(Collectors.toMap(Comment::getId, Comment::getContext));
    }

    /**
     * 获取用户的所有收藏
     * @param ids
     * @return
     */
    @PostMapping("/replyByStrIds")
    public Map<Long,String> replyByStrIds(@RequestBody List<String> ids){
        List<Long> list = ids.stream().map(Long::parseLong).toList();
        if(list.isEmpty()){
            return Map.of();
        }

        return replyService.list(Wrappers.<Reply>lambdaQuery()
                .in(Reply::getId, list)
        ).stream().collect(Collectors.toMap(Reply::getId, Reply::getContext));
    }

    /**
     * 异步填写攻击性判断
     * @param commentToxicDTO
     */
    @PutMapping("/toxicJudgement")
    @Async
    void toxicJudgement(@RequestBody CommentToxicDTO commentToxicDTO){
        if(commentToxicDTO == null){
            return;
        }
        String mulType ;
        if(commentToxicDTO.getTopicList() != null && !commentToxicDTO.getTopicList().isEmpty()){
            mulType = String.join(",", commentToxicDTO.getTopicList());
        }else{
            mulType = "";
        }
        if(commentToxicDTO.getType() == 0){
            // 处理评论
            commentService.update(Wrappers.<Comment>lambdaUpdate()
                    .eq(Comment::getId,commentToxicDTO.getId())
                    .set(Comment::getIsToxic,commentToxicDTO.getIsToxic())
                    .set(Comment::getMulType,mulType)
            );
        }
        if(commentToxicDTO.getType() == 1){
            // 处理回复
            replyService.update(
                    Wrappers.<Reply>lambdaUpdate()
                    .eq(Reply::getId,commentToxicDTO.getId())
                    .set(Reply::getIsToxic,commentToxicDTO.getIsToxic())
                    .set(Reply::getMulType,mulType)
            );
        }
    }
}
