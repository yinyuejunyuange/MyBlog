package org.oyyj.blogservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.dto.BlogReportForAdminDTO;
import org.oyyj.blogservice.dto.CommentReportForAdminDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.dto.ReplyReportForAdminDTO;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.pojo.*;
import org.oyyj.blogservice.service.*;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.blogservice.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.security.sasl.AuthenticationException;
import java.util.*;

@RestController
@RequestMapping("/myBlog/admin/blog")
@Slf4j
public class BlogForAdminController {

    @Autowired
    private IBlogService blogService;


    @Autowired
    private ICommentService commentService;

    @Autowired
    private IReplyService replyService;
    @Autowired
    private UserFeign userFeign;

    @Autowired
    private IBlogReportService blogReportService;

    @Autowired
    private ICommentReportService commentReportService;

    @Autowired
    private IReplyReportService replyReportService;
    @GetMapping("/getIncreaseBlog")
    public Map<String,Long> getIncreaseBlog(HttpServletRequest request) throws AuthenticationException {

        String source="ADMINSERVICE";

        if(!source.equals(request.getHeader("source"))){
            throw new AuthenticationException("来源不正确");
        }

        return blogService.getIncreaseBlog();
    }

    @GetMapping("/getAllTypeNum")
    public Map<String,Long> getAllTypeNum(HttpServletRequest request) throws AuthenticationException {

        String source="ADMINSERVICE";

        if(!source.equals(request.getHeader("source"))){
            throw new AuthenticationException("来源不正确");
        }
        return blogService.getAllTypeNum();
    }

    @GetMapping("/getAllMessage")
    public Long getAllMessage(HttpServletRequest request) throws AuthenticationException {


        String source="ADMINSERVICE";

        if(!source.equals(request.getHeader("source"))){
            throw new AuthenticationException("来源不正确");
        }

        long commentNum = (long)commentService.list().size();
        long replyNum = (long)replyService.list().size();

        return commentNum+replyNum;
    }

    @GetMapping("/getBlogNum")
    public Long getBlogNum(HttpServletRequest request) throws AuthenticationException {


        String source="ADMINSERVICE";

        if(!source.equals(request.getHeader("source"))){
            throw new AuthenticationException("来源不正确");
        }

        return (long)blogService.list().size();
    }


    @GetMapping("/getBlogListByAdmin") // 将json传递过去
    public String getBlogListAdmin(@RequestParam(value = "blogName",required = false) String blogName,
                                   @RequestParam(value = "authorName",required = false) String authorName,
                                   @RequestParam(value = "startDate",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
                                   @RequestParam(value = "endDate",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,  // spring框架默认是不支持前端 date的iso类型  所以要设置
                                   @RequestParam(value = "status",required = false) String status,
                                   @RequestParam(value = "currentPage") Integer currentPage,
                                   HttpServletRequest request) throws AuthenticationException, JsonProcessingException {

        String source="ADMINSERVICE";

        if(!source.equals(request.getHeader("source"))){
            throw new AuthenticationException("来源不正确");
        }

        return  blogService.getBlogListByAdmin(blogName, authorName, startDate, endDate, status, currentPage);
    }

    @PutMapping("/updateBlogStatus")
    public Boolean updateBlogStatus(@RequestBody Map<String,Object> map, HttpServletRequest request) throws AuthenticationException {
        String source="ADMINSERVICE";

        if(!source.equals(request.getHeader("source"))){
            throw new AuthenticationException("来源不正确");
        }

        String blogIds=map.get("blogId").toString();
        String status = (String) map.get("status");
        int statusNum;
        switch(status){
            case "保存中":
                statusNum=1;
                break;
            case "发布":
                statusNum=2;
                break;
            case "审核中":
                statusNum=3;
                break;
            case "禁止查看":
                statusNum=4;
                break;
            default:
                log.error("请求参数不正确"+status);
                throw new AuthenticationException("参数不正确");
        }

        return blogService.update(Wrappers.<Blog>lambdaUpdate().eq(Blog::getId, blogIds)
                .set(Blog::getStatus, statusNum));

    }

    @DeleteMapping("/deleteBlog")
    public Boolean deleteBlog(@RequestParam("blogId") Long blogId, HttpServletRequest request) throws AuthenticationException {

        String source="ADMINSERVICE";

        if(!source.equals(request.getHeader("source"))){
            throw new AuthenticationException("来源不正确");
        }

        return blogService.remove(Wrappers.<Blog>lambdaQuery().eq(Blog::getId,blogId));
    }


    @GetMapping("/getCommentForAdmin")
    public PageDTO<CommentAdminVO> getCommentForAdmin(@RequestParam(value = "blogName",required = false) String blogName,
                                                      @RequestParam(value = "userName",required = false)String userName,
                                                      @RequestParam(value = "startTime",required = false)  Date startTime,
                                                      @RequestParam(value = "endTime",required = false)  Date endTime,
                                                      @RequestParam(value = "isVisible",required = false)Integer isVisible,
                                                      @RequestParam("currentPage") Integer currentPage,
                                                      HttpServletRequest request ){

        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("获取到 来源不正确的请求");
            throw new IllegalArgumentException("请求来源不正确");
        }

        IPage<Comment> page=new Page<>(currentPage,20);

        LambdaQueryWrapper<Comment> lqw=new LambdaQueryWrapper<>();
        if(Objects.nonNull(blogName)&&!blogName.isEmpty()){

            List<Long> list = blogService.list(Wrappers.<Blog>lambdaQuery().like(Blog::getTitle, blogName)).stream()
                    .map(Blog::getId).toList();
            if(list.isEmpty()){
                PageDTO<CommentAdminVO> pageDTO=new PageDTO<>();
                pageDTO.setPageSize((int) page.getSize());
                pageDTO.setPageNow(currentPage);
                pageDTO.setTotal((int) page.getTotal());
                pageDTO.setPageList(new ArrayList<>());

                return pageDTO; // 返回一个空集合
            }else{
                lqw.in(Comment::getBlogId,list);
            }
        }

        if(Objects.nonNull(userName)&&!userName.isEmpty()){
            List<Long> list = userFeign.getIdsLikeName(userName);
            if(list.isEmpty()){
                PageDTO<CommentAdminVO> pageDTO=new PageDTO<>();
                pageDTO.setPageSize((int) page.getSize());
                pageDTO.setPageNow(currentPage);
                pageDTO.setTotal((int) page.getTotal());
                pageDTO.setPageList(new ArrayList<>());

                return pageDTO; // 返回一个空集合
            }else{
                lqw.in(Comment::getUserId,list);
            }
        }

        if(Objects.nonNull(startTime)){
            lqw.ge(Comment::getCreateTime,startTime);
        }

        if(Objects.nonNull(endTime)){
            lqw.le(Comment::getCreateTime,endTime);
        }

        if(Objects.nonNull(isVisible)){
            lqw.eq(Comment::getIsVisible,isVisible);
        }

        List<CommentAdminVO> list = commentService.list(page, lqw).stream().map(i -> CommentAdminVO.builder()
                .id(String.valueOf(i.getId()))
                .blogName(blogService.getOne(Wrappers.<Blog>lambdaQuery().eq(Blog::getId, i.getBlogId())).getTitle())
                .userName(userFeign.getNameInIds(Collections.singletonList(String.valueOf(i.getUserId()))).get(i.getUserId()))
                .context(i.getContext())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .isVisible(i.getIsVisible())
                .build()).toList();

        PageDTO<CommentAdminVO> pageDTO=new PageDTO<>();
        pageDTO.setPageSize((int) page.getSize());
        pageDTO.setPageNow(currentPage);
        pageDTO.setTotal((int) page.getTotal());
        pageDTO.setPageList(list);

        return pageDTO;
    }

    // 修改博客状态
    @PutMapping("/changeCommentStatus")
    public Map<String,Object> changeCommentStatus(@RequestParam("commentId") String commentId,
                                                  @RequestParam("isVisible") Integer isVisible,
                                                  HttpServletRequest request) throws AuthenticationException {

        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }

        if(isVisible!=1&&isVisible!=0){
            log.error("请求状态不正确");
            throw new AuthenticationException("请求状态不正确");
        }
        boolean update = commentService.update(Wrappers.<Comment>lambdaUpdate().eq(Comment::getId, Long.valueOf(commentId))
                .set(Comment::getIsVisible, isVisible)
        );
        if(update){
            return ResultUtil.successMap(null,"修改成功");
        }else{
            return ResultUtil.failMap("修改失败");
        }
    }

    @DeleteMapping("/deleteComment")
    public Map<String,Object> deleteComment(@RequestParam("commentId") Long commentId,
                                            HttpServletRequest request){

        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("获取到 来源不正确的请求");
            throw new IllegalArgumentException("请求来源不正确");
        }

        boolean remove = commentService.remove(Wrappers.<Comment>lambdaQuery().eq(Comment::getId, commentId));
        if(remove){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }

    @GetMapping("/getReplyForAdmin")
    public PageDTO<ReplyAdminVO> getReplyForAdmin(@RequestParam(value = "blogName",required = false) String blogName, //
                                                  @RequestParam(value = "userName",required = false)String userName,
                                                  @RequestParam(value = "comment",required = false)String comment, // 被回复的内容
                                                  @RequestParam(value = "startTime",required = false)  Date startTime,
                                                  @RequestParam(value = "endTime",required = false)  Date endTime,
                                                  @RequestParam(value = "isVisible",required = false)Integer isVisible,
                                                  @RequestParam("currentPage") Integer currentPage,
                                                  HttpServletRequest request ){

        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("获取到 来源不正确的请求");
            throw new IllegalArgumentException("请求来源不正确");
        }

        try {
            IPage<Reply> page=new Page<>(currentPage,20);

            LambdaQueryWrapper<Reply> lqw=new LambdaQueryWrapper<>();
            if(Objects.nonNull(blogName)&&!blogName.isEmpty()){


                List<Long> list = blogService.list(Wrappers.<Blog>lambdaQuery().like(Blog::getTitle, blogName)).stream()
                        .flatMap(i -> commentService.list(Wrappers.<Comment>lambdaQuery().eq(Comment::getBlogId, i.getId()))
                                .stream().map(Comment::getId))
                        .toList();
                // flatMap：这个方法允许将每个元素的流转换成多个元素，并将它们展平为一个单一的流。
                if(list.isEmpty()){
                    PageDTO<ReplyAdminVO> pageDTO=new PageDTO<>();
                    pageDTO.setPageSize((int) page.getSize());
                    pageDTO.setPageNow(currentPage);
                    pageDTO.setTotal((int) page.getTotal());
                    pageDTO.setPageList(new ArrayList<>());

                    return pageDTO; // 返回一个空集合
                }else{
                    // 将查询到的 评论id 与 回复相关联
                    lqw.in(Reply::getCommentId,list);
                }
            }

            if(Objects.nonNull(userName)&&!userName.isEmpty()){
                List<Long> list = userFeign.getIdsLikeName(userName);
                if(list.isEmpty()){
                    PageDTO<ReplyAdminVO> pageDTO=new PageDTO<>();
                    pageDTO.setPageSize((int) page.getSize());
                    pageDTO.setPageNow(currentPage);
                    pageDTO.setTotal((int) page.getTotal());
                    pageDTO.setPageList(new ArrayList<>());

                    return pageDTO; // 返回一个空集合
                }else{
                    lqw.in(Reply::getUserId,list);
                }
            }

            if(Objects.nonNull(comment)&&!comment.isEmpty()){
                List<Long> list = commentService.list(Wrappers.<Comment>lambdaQuery().like(Comment::getContext, comment))
                        .stream().map(Comment::getId).toList();
                if(list.isEmpty()){
                    PageDTO<ReplyAdminVO> pageDTO=new PageDTO<>();
                    pageDTO.setPageSize((int) page.getSize());
                    pageDTO.setPageNow(currentPage);
                    pageDTO.setTotal((int) page.getTotal());
                    pageDTO.setPageList(new ArrayList<>());

                    return pageDTO; // 返回一个空集合
                }else{
                    lqw.in(Reply::getCommentId,list);
                }
            }

            if(Objects.nonNull(startTime)){
                lqw.ge(Reply::getCreateTime,startTime);
            }

            if(Objects.nonNull(endTime)){
                lqw.le(Reply::getCreateTime,endTime);
            }

            if(Objects.nonNull(isVisible)){
                lqw.eq(Reply::getIsVisible,isVisible);
            }

            List<Reply> replies = replyService.list(page, lqw);
            List<String> userIds = replies.stream().map(i->String.valueOf(i.getUserId())).toList();

            Map<Long, String> nameInIds = userFeign.getNameInIds(userIds);

            List<ReplyAdminVO> list = replies.stream().map(i -> {
                                Long commentId = i.getCommentId();
                                Comment one = commentService.getOne(Wrappers.<Comment>lambdaQuery().eq(Comment::getId, commentId));
                                if(Objects.isNull(one)){

                                    return null;  // 评论和博客存在被删除的情况 只要是这样 相关的评论就是不可见的 所以 直接返回null
                                }
                                Blog blog = blogService.getOne(Wrappers.<Blog>lambdaQuery().eq(Blog::getId, one.getBlogId()));
                                if(Objects.isNull(blog)){
                                    return null; // 理由同上
                                }
                                return ReplyAdminVO.builder()
                                        .id(String.valueOf(i.getId()))
                                        .userName(nameInIds.get(i.getUserId()))
                                        .blogName(blog.getTitle())
                                        .comment(one.getContext())
                                        .context(i.getContext())
                                        .createTime(i.getCreateTime())
                                        .updateTime(i.getUpdateTime())
                                        .isVisible(i.getIsVisible())
                                        .build();
                            }
                    ).filter(Objects::nonNull)  // 跳过 为null的值
                    .toList();


            PageDTO<ReplyAdminVO> pageDTO=new PageDTO<>();
            pageDTO.setPageSize((int) page.getSize());
            pageDTO.setPageNow(currentPage);
            pageDTO.setTotal((int) page.getTotal());
            pageDTO.setPageList(list);

            return pageDTO;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
    // 修改博客状态
    @PutMapping("/changeReplyStatus")
    public Map<String,Object> changeReplyStatus(@RequestParam("replyId") String replyId,
                                                @RequestParam("isVisible") Integer isVisible,
                                                HttpServletRequest request) throws AuthenticationException {

        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }

        if(isVisible!=1&&isVisible!=0){
            log.error("请求状态不正确");
            throw new AuthenticationException("请求状态不正确");
        }
        boolean update = replyService.update(Wrappers.<Reply>lambdaUpdate().eq(Reply::getId, Long.valueOf(replyId))
                .set(Reply::getIsVisible, isVisible)
        );
        if(update){
            return ResultUtil.successMap(null,"修改成功");
        }else{
            return ResultUtil.failMap("修改失败");
        }
    }
    @DeleteMapping("/deleteReply")
    public Map<String,Object> deleteReply(@RequestParam("replyId") Long replyId,
                                          HttpServletRequest request){
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("获取到 来源不正确的请求");
            throw new IllegalArgumentException("请求来源不正确");
        }

        boolean remove = replyService.remove(Wrappers.<Reply>lambdaQuery().eq(Reply::getId, replyId));
        if(remove){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }

    // 举报博客
    @PutMapping("/reportBlog")
    public Map<String,Object> reportBlogs(@RequestBody BlogReportVO blogReportVO, HttpServletRequest request) throws AuthenticationException {
        if(!"USERSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }

        return blogReportService.reportBlogs(blogReportVO);
    }

    // todo 管理员查询博客举报
    @GetMapping("/getBlogReports")
    public PageDTO<BlogReportForAdminDTO> getBlogReports(@RequestParam("currentPage") Integer currentPage,
                                                         @RequestParam(value = "adminName",required = false) String adminName,
                                                         @RequestParam(value = "status",required = false) Integer status,
                                                         HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("数据来源不正确");
            throw new AuthenticationException("数据来源不正确");
        }

        return blogReportService.reportBlogsPage(currentPage,adminName,status);
    }


    // todo 管理员 修改博客举报状态
    @PutMapping("/updateBlogReport")
    public Map<String,Object> updateBlogReport(@RequestBody AdminUpdateBlogReportVO adminUpdateBlogReportVO
            ,HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }
        return blogReportService.updateBlogReport(adminUpdateBlogReportVO);
    }
    // todo 管理员删除 博客举报

    @DeleteMapping("/deleteBlogReport")
    public Map<String ,Object> deleteBlogReport(@RequestParam("blogReportId")String blogReportId,
                                                HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }
        boolean remove = blogReportService.remove(Wrappers.<BlogReport>lambdaQuery()
                .eq(BlogReport::getId, Long.parseLong(blogReportId)));
        if(remove){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }

    // 举报评论
    @PutMapping("/reportComment")
    public Map<String,Object> reportComments(@RequestBody CommentReportVO commentReportVO, HttpServletRequest request) throws AuthenticationException {
        if(!"USERSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }

        return commentReportService.commentReport(commentReportVO);
    }

    // todo 管理员查询 评论举报
    @GetMapping("/getCommentReports")
    public PageDTO<CommentReportForAdminDTO> getCommentReports(@RequestParam("currentPage") Integer currentPage,
                                                               @RequestParam(value = "adminName",required = false) String adminName,
                                                               @RequestParam(value = "status",required = false) Integer status,
                                                               HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("数据来源不正确");
            throw new AuthenticationException("数据来源不正确");
        }
        return commentReportService.reportCommentsPage(currentPage,adminName,status);
    }

    // todo 管理员 修改评论举报状态
    @PutMapping("/updateCommentReport")
    public Map<String,Object> updateCommentReport(@RequestBody AdminUpdateCommentReportVO adminUpdateCommentReportVO
            ,HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }
        return commentReportService.updateCommentReport(adminUpdateCommentReportVO);
    }


    // todo 管理员删除 评论举报


    @DeleteMapping("/deleteCommentReport")
    public Map<String ,Object> deleteCommentReport(@RequestParam("commentReportId")String commentReportId,
                                                   HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }
        boolean remove = commentReportService.remove(Wrappers.<CommentReport>lambdaQuery()
                .eq(CommentReport::getId, Long.parseLong(commentReportId)));
        if(remove){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }

    // 举报回复  todo 修改
    @PutMapping("/reportReply")
    public Map<String,Object> reportReply(@RequestBody ReplyReportVO replyReportVO, HttpServletRequest request) throws AuthenticationException {
        if(!"USERSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }

        return replyReportService.ReplyReport(replyReportVO);
    }

    // todo 管理员查询 回复举报

    @GetMapping("/getReplyReports")
    public PageDTO<ReplyReportForAdminDTO> getReplyReports(@RequestParam("currentPage") Integer currentPage,
                                                           @RequestParam(value = "adminName",required = false) String adminName,
                                                           @RequestParam(value = "status",required = false) Integer status,
                                                           HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("数据来源不正确");
            throw new AuthenticationException("数据来源不正确");
        }
        return replyReportService.reportReplyPage(currentPage,adminName,status);
    }

    // todo 管理员 修改回复举报状态
    @PutMapping("/updateReplyReport")
    public Map<String,Object> updateReplyReport(@RequestBody AdminUpdateReplyReportVO adminUpdateReplyReportVO
            ,HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }
        return replyReportService.updateReplyReport(adminUpdateReplyReportVO);
    }


    // todo 管理员删除 回复举报
    @DeleteMapping("/deleteReplyReport")
    public Map<String ,Object> deleteReplyReport(@RequestParam("ReplyReportId")String replyReportId,
                                                 HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }
        boolean remove = replyReportService.remove(Wrappers.<ReplyReport>lambdaQuery()
                .eq(ReplyReport::getId, Long.parseLong(replyReportId)));
        if(remove){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }
}
