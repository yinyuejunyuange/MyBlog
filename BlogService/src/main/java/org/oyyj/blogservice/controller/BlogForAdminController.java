package org.oyyj.blogservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.dto.*;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.pojo.*;
import org.oyyj.blogservice.service.*;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.blogservice.vo.*;
import org.oyyj.blogservice.vo.admin.BlogTypeVO;
import org.oyyj.blogservice.vo.behavior.MonthlyBehaviorVO;
import org.oyyj.mycommon.annotation.RequestRole;
import org.oyyj.mycommon.pojo.dto.blog.Blog12MonthDTO;
import org.oyyj.mycommonbase.common.RoleEnum;
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
    private IUserBehaviorService iUserBehaviorService;


    /**
     * 博客的每个月增长情况
     * @return
     * @throws AuthenticationException
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/getIncreaseBlog")
    public ResultUtil<Map<String,Long>> getIncreaseBlog() throws AuthenticationException {
        return ResultUtil.success(blogService.getIncreaseBlog());
    }

    /**
     * 每种类别的博客数量
     * @param request
     * @return
     * @throws AuthenticationException
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/getAllTypeNum")
    public ResultUtil<Map<String,Long>> getAllTypeNum(HttpServletRequest request) throws AuthenticationException {
        return ResultUtil.success(blogService.getAllTypeNum());
    }

    /**
     * 获取所有评论的总数
     * @param request
     * @return
     * @throws AuthenticationException
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/getAllMessage")
    public ResultUtil<Long> getAllMessage(HttpServletRequest request) throws AuthenticationException {
        long commentNum = (long)commentService.count();
        long replyNum = (long)replyService.count();

        return ResultUtil.success(commentNum+replyNum);
    }

    /**
     * 获取所有博客数量
     * @param request
     * @return
     * @throws AuthenticationException
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/getBlogNum")
    public ResultUtil<Long> getBlogNum(HttpServletRequest request) throws AuthenticationException {
        return ResultUtil.success((long)blogService.count());
    }

    /**
     * 管理员分页查询博客
     * @param blogName
     * @param authorName
     * @param startDate
     * @param endDate
     * @param status
     * @param currentPage
     * @param pageSize
     * @return
     * @throws JsonProcessingException
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/getBlogListByAdmin")
    public ResultUtil<PageDTO<BlogDTO>> getBlogListAdmin(@RequestParam(value = "blogName",required = false) String blogName,
                                                         @RequestParam(value = "authorName",required = false) String authorName,
                                                         @RequestParam(value = "startDate",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
                                                         @RequestParam(value = "endDate",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,  // spring框架默认是不支持前端 date的iso类型  所以要设置
                                                         @RequestParam(value = "status",required = false) String status,
                                                         @RequestParam(value = "currentPage") Integer currentPage,
                                                         @RequestParam(value = "pageSize") Integer pageSize
                                   ) throws JsonProcessingException {

        return  ResultUtil.success(blogService.getBlogListByAdmin(blogName, authorName, startDate, endDate, status, currentPage, pageSize));
    }

    /**
     * 修改博客状态
     * @param blogId
     * @param status
     * @return
     * @throws AuthenticationException
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/updateBlogStatus")
    public ResultUtil<Boolean> updateBlogStatus(@RequestParam("blogId")String blogId , @RequestParam("status") Integer status) throws AuthenticationException {

        return ResultUtil.success(blogService.update(Wrappers.<Blog>lambdaUpdate().eq(Blog::getId, Long.parseLong(blogId))
                .set(Blog::getStatus, status)));

    }


    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/getCommentForAdmin")
    public ResultUtil<PageDTO<CommentAdminVO>> getCommentForAdmin(@RequestParam(value = "blogName",required = false) String blogName,
                                                      @RequestParam(value = "userName",required = false)String userName,
                                                      @RequestParam(value = "startTime",required = false)  Date startTime,
                                                      @RequestParam(value = "endTime",required = false)  Date endTime,
                                                      @RequestParam(value = "isVisible",required = false)Integer isVisible,
                                                      @RequestParam(value = "currentPage",defaultValue = "1") Integer currentPage,
                                                      @RequestParam(value = "pageSize",defaultValue = "10")Integer pageSize) {

        IPage<Comment> page=new Page<>(currentPage,pageSize);

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

                return ResultUtil.success(pageDTO); // 返回一个空集合
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

                return ResultUtil.success(pageDTO); // 返回一个空集合
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
                .isToxic(i.getIsToxic())
                .mulType(i.getMulType())
                .build()).toList();

        PageDTO<CommentAdminVO> pageDTO=new PageDTO<>();
        pageDTO.setPageSize((int) page.getSize());
        pageDTO.setPageNow(currentPage);
        pageDTO.setTotal((int) page.getTotal());
        pageDTO.setPageList(list);

        return ResultUtil.success(pageDTO);
    }

    // 修改评论状态
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/changeCommentStatus")
    public Map<String,Object> changeCommentStatus(@RequestParam("commentId") String commentId,
                                                  @RequestParam("isVisible") Integer isVisible) throws AuthenticationException {

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

    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/getReplyForAdmin")
    public ResultUtil<PageDTO<ReplyAdminVO>> getReplyForAdmin(@RequestParam(value = "blogName",required = false) String blogName, //
                                                  @RequestParam(value = "userName",required = false)String userName,
                                                  @RequestParam(value = "comment",required = false)String comment, // 被回复的内容
                                                  @RequestParam(value = "startTime",required = false)  Date startTime,
                                                  @RequestParam(value = "endTime",required = false)  Date endTime,
                                                  @RequestParam(value = "isVisible",required = false)Integer isVisible,
                                                  @RequestParam("currentPage") Integer currentPage,
                                                  HttpServletRequest request ){


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

                    return ResultUtil.success(pageDTO); // 返回一个空集合
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

                    return ResultUtil.success(pageDTO); // 返回一个空集合
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

                    return ResultUtil.success(pageDTO); // 返回一个空集合
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
                                        .isToxic(i.getIsToxic())
                                        .mulType(i.getMulType())
                                        .build();
                            }
                    ).filter(Objects::nonNull)  // 跳过 为null的值
                    .toList();


            PageDTO<ReplyAdminVO> pageDTO=new PageDTO<>();
            pageDTO.setPageSize((int) page.getSize());
            pageDTO.setPageNow(currentPage);
            pageDTO.setTotal((int) page.getTotal());
            pageDTO.setPageList(list);

            return ResultUtil.success(pageDTO);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
    // 修改博客状态
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/changeReplyStatus")
    public ResultUtil<String> changeReplyStatus(@RequestParam("replyId") String replyId,
                                                @RequestParam("isVisible") Integer isVisible) throws AuthenticationException {

        if(isVisible!=1&&isVisible!=0){
            log.error("请求状态不正确");
            throw new AuthenticationException("请求状态不正确");
        }
        boolean update = replyService.update(Wrappers.<Reply>lambdaUpdate().eq(Reply::getId, Long.valueOf(replyId))
                .set(Reply::getIsVisible, isVisible)
        );
        if(update){
            return ResultUtil.success("修改成功");
        }else{
            return ResultUtil.fail("修改失败");
        }
    }

    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/blog12Month")
    public ResultUtil<Blog12MonthDTO> blog12Month() throws AuthenticationException {
        return ResultUtil.success(blogService.getBlog12MonthByUserId(null));
    }


    /**
     * 首页各种博客的类别展示
     * @return
     * @throws AuthenticationException
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/blogTypeDashboard")
    public ResultUtil<List<BlogTypeVO>> blogTypeList() throws AuthenticationException {
        return blogService.blogTypeList();
    }

    /**
     * 谋篇博客近12个月的展示趋势
     * @param blogId
     * @return
     * @throws Exception
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/monthlyBehaviorTrend")
    public ResultUtil<List<MonthlyBehaviorVO>> getMonthlyBehaviorTrend( @RequestParam("blogId") Long blogId) throws Exception{
        return ResultUtil.success(iUserBehaviorService.getBlogBehaviorTrend(blogId));
    }

    /**
     * 修改攻击性类别
     * @param comRepInfoDTO
     * @return
     * @throws Exception
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/updateCommentToxic")
    public ResultUtil<Boolean> updateCommentToxic( @RequestBody ComRepInfoDTO comRepInfoDTO) throws Exception{
        if(Objects.isNull(comRepInfoDTO) || Objects.isNull(comRepInfoDTO.getId())){
            return ResultUtil.success(false);
        }
        commentService.update(Wrappers.<Comment>lambdaUpdate()
                .eq(Comment::getId, Long.valueOf(comRepInfoDTO.getId()))
                .set(Comment::getIsToxic, comRepInfoDTO.getIsToxic())
                .set(Comment::getMulType, comRepInfoDTO.getMulType())
        );

        return ResultUtil.success(true);
    }

    /**
     * 修改攻击性类别
     * @param comRepInfoDTO
     * @return
     * @throws Exception
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/updateReplyToxic")
    public ResultUtil<Boolean> updateReplyToxic( @RequestBody ComRepInfoDTO comRepInfoDTO) throws Exception{
        if(Objects.isNull(comRepInfoDTO) || Objects.isNull(comRepInfoDTO.getId())){
            return ResultUtil.success(false);
        }
        replyService.update(Wrappers.<Reply>lambdaUpdate()
                .eq(Reply::getId, Long.valueOf(comRepInfoDTO.getId()))
                .set(Reply::getIsToxic, comRepInfoDTO.getIsToxic())
                .set(Reply::getMulType, comRepInfoDTO.getMulType())
        );

        return ResultUtil.success(true);
    }


}
