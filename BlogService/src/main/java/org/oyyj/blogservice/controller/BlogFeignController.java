package org.oyyj.blogservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.blogservice.pojo.ReplyReport;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.service.ICommentService;
import org.oyyj.blogservice.service.IReplyReportService;
import org.oyyj.blogservice.service.IReplyService;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.mycommon.pojo.dto.blog.Blog12MonthDTO;
import org.oyyj.mycommon.pojo.dto.blog.ComRepForUserDTO;
import org.oyyj.mycommon.pojo.vo.UserComRepVO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/myBlog/blogFeign")
public class BlogFeignController {

    private final IBlogService iBlogService;
    private final ICommentService iCommentService;
    private final IReplyService iReplyService;

    public BlogFeignController(IBlogService iBlogService, ICommentService iCommentService, IReplyService iReplyService) {
        this.iBlogService = iBlogService;
        this.iCommentService = iCommentService;
        this.iReplyService = iReplyService;
    }

    /**
     * 获取用户的所有收藏
     * @param ids
     * @return
     */
    @PostMapping("/titlesByStrIds")
    public Map<Long,String> blogTitleByStrIds(@RequestBody List<String> ids){
        List<Long> list = ids.stream().map(Long::parseLong).toList();
        if(list.isEmpty()){
            return Map.of();
        }

        return iBlogService.list(Wrappers.<Blog>lambdaQuery()
                .in(Blog::getId, list)
        ).stream().collect(Collectors.toMap(Blog::getId, Blog::getTitle));
    }



    // 查询 某用户近12月的博客发表记录
    @GetMapping("/userBlog12Month")
    public Blog12MonthDTO getBlog12MonthByUserId(@RequestParam("userId")Long userId){
        return iBlogService.getBlog12MonthByUserId(userId);
    }

    @GetMapping("/totalBlogs")
    public Long getTotalBlogs(){
        return iBlogService.count();
    }

    @GetMapping("/totalComReps")
    public Long getTotalComReps(){
        long commentNums = iCommentService.count();
        long replyNums = iReplyService.count();
        return commentNums + replyNums;
    }

    // 查询 指定的 userIds中的博客数量
    @PostMapping("/countBlogByUserList")
    public Map<Long,Integer> countByUserList(@RequestBody List<Long> userIds){
        return iBlogService.countByUserList(userIds);
    }

    // 查询 指定 ids中的评论数量 攻击性评论占比
    @PostMapping("/countCommentReplyByUserList")
    public List<ComRepForUserDTO> countCommentReplyByUserList(@RequestBody List<Long> userIds){
        return iBlogService.countCommentReplyByUserList(userIds);
    }

    @GetMapping("/toxicComRepResult")
    public List<UserComRepVO>  toxicComRepResult(@RequestParam("userId")Long userId){
        List<UserComRepVO> result = new ArrayList<>();
        List<Comment> comments = iCommentService.list(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getUserId, userId)
                .ne(Comment::getIsToxic, 0)
        );
        List<Reply> replyList = iReplyService.list(Wrappers.<Reply>lambdaQuery()
                .eq(Reply::getUserId, userId)
                .ne(Reply::getIsToxic, 0)
        );

        List<Long> blogList = new ArrayList<>(comments.stream().map(Comment::getBlogId).toList());
        List<Long> replyRelateCommentIds = replyList.stream().map(Reply::getCommentId).toList();
        Map<Long,Long> replyBlogMap = new HashMap<>();
        if(!replyRelateCommentIds.isEmpty()){
            List<Comment> commentsRelateReply = iCommentService.list(Wrappers.<Comment>lambdaQuery()
                    .in(Comment::getId, replyRelateCommentIds)
            );
            Map<Long, Long> commentBlogMap = commentsRelateReply.stream().collect(Collectors.toMap(Comment::getId, Comment::getBlogId));
            Map<Long,Long> replyCommentMap  = replyList.stream().collect(Collectors.toMap(Reply::getId, Reply::getCommentId));
            replyCommentMap.forEach((k,v)->{
                Long l = commentBlogMap.get(v);
                replyBlogMap.put(k,l);
            });
            List<Long> list = commentsRelateReply.stream().map(Comment::getBlogId).toList();
            blogList.addAll(list);
        }

        Map<Long, String> collect;

        if(!blogList.isEmpty()){
            collect = iBlogService.listByIds(blogList).stream().collect(Collectors.toMap(Blog::getId, Blog::getTitle));
        } else {
            collect = Map.of();
        }

        result.addAll(comments.stream().map(item -> {
            UserComRepVO userComRepVO = new UserComRepVO();
            userComRepVO.setId(String.valueOf(item.getId()));
            userComRepVO.setComment(item.getContext());
            userComRepVO.setBlogId(String.valueOf(item.getBlogId()));
            userComRepVO.setBlogName(collect.get(item.getBlogId()));
            userComRepVO.setType("评论");
            userComRepVO.setIsVisible(item.getIsVisible());
            userComRepVO.setIsToxic(item.getIsToxic());
            userComRepVO.setMulType(item.getMulType());
            return userComRepVO;
        }).toList());

        result.addAll(replyList.stream().map(item -> {
            UserComRepVO userComRepVO = new UserComRepVO();
            userComRepVO.setId(String.valueOf(item.getId()));
            userComRepVO.setComment(item.getContext());
            Long blogId = replyBlogMap.get(item.getId());
            userComRepVO.setBlogId(String.valueOf(blogId));
            userComRepVO.setBlogName(collect.get(blogId));
            userComRepVO.setType("回复");
            userComRepVO.setIsVisible(item.getIsVisible());
            userComRepVO.setIsToxic(item.getIsToxic());
            userComRepVO.setMulType(item.getMulType());
            return userComRepVO;
        }).toList());
        return result;
    }

}
