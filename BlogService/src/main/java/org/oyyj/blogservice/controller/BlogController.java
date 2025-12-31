package org.oyyj.blogservice.controller;

import ch.qos.logback.core.util.FileUtil;
import com.alibaba.nacos.api.model.v2.Result;
import com.alibaba.nacos.api.naming.pojo.healthcheck.impl.Http;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.oyyj.blogservice.dto.*;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.pojo.*;
import org.oyyj.blogservice.service.*;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.blogservice.vo.*;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.security.sasl.AuthenticationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/blog")
public class BlogController {

    private static final Logger log = LoggerFactory.getLogger(BlogController.class);
    private final int PAGE_SIZE = 6;

    @Autowired
    private IBlogService blogService;

    @Autowired
    private ServletContext servletContext; // 应用上下文

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

    @CrossOrigin // 允许此方法跨域
    @PostMapping("/write")
    public Map<String, Object> writeBlog(@RequestBody BlogDTO blogDTO) {
        Date date=new Date();

        Blog build = Blog.builder()
                .title(blogDTO.getTitle())
                .context(blogDTO.getContext())
                .userId(Long.parseLong(blogDTO.getUserId()))
                .createTime(date)
                .updateTime(date)
                .status(blogDTO.getStatus())
                .typeList(blogDTO.getTypeList())
                .introduce(blogDTO.getIntroduce())
                .isDelete(0)
                .build();

        blogService.saveBlog(build);
        return ResultUtil.successMap(build.getId(),"博客保存成功");
    }

    @GetMapping("/read")
    public Map<String, Object> readBlog(@RequestParam("blogId") String  id,@RequestUser LoginUser loginUser) {
        ReadDTO readDTO = blogService.ReadBlog(Long.valueOf(id),loginUser);
        if(Objects.isNull(readDTO)){
            return ResultUtil.failMap("查询失败");
        }
        return ResultUtil.successMap(readDTO,"查询成功");
    }

    /**
     * 上传图片的接口
     * @param file
     * @return
     */

    @RequestMapping("/file/upload")
    public ImageResultDTO uploadPict(@RequestPart("image") MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString().substring(0,10)+file.getOriginalFilename(); //生成文件名称 确保不重复


        //String uploadPath= ResourceUtils.getURL("classpath:").getPath()+"static/image/"+fileName;
        String uploadPath= "H:/10516/Test/BlogImage/"+fileName;

        // 下载图片
        FileUtils.copyInputStreamToFile(file.getInputStream(),new File(servletContext.getContextPath()+"/"+uploadPath));

        // 返回一个满足富文本要求的数据格式
        String url="http://localhost:8080/myBlog/user/blog/file/download/"+fileName;

        return new ImageResultDTO(url);
    }

    /**
     * 文件下载接口 将图片按照数据流的方式传递给前端
     * @param fileName
     * @param response
     * @throws IOException
     */
    @GetMapping("/file/download/{fileName}")
    public void downloadFile(@PathVariable("fileName")String fileName, HttpServletResponse response) throws IOException {
        // 获取真实的文件路径
        //String filePath= ResourceUtils.getURL("classpath:").getPath()+"static/image/"+fileName;
        String filePath= "H:/10516/Test/BlogImage/"+fileName;
        System.out.println("filePath:"+filePath);

        File file=new File(filePath); //获取文件数据
        if(!file.exists()){
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // 设置响应头
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+filePath+"\"");

        Files.copy(file.toPath(), response.getOutputStream());
        response.getOutputStream().flush();

    }

//    // 分页查询 博客文章
//    @GetMapping("/list")
//    public Map<String,Object> getBlogList(@RequestParam int pageNow, @RequestParam(required = false) String type , @RequestUser(required = false ) LoginUser loginUser) {
//        PageDTO<BlogDTO> blogByPage = blogService.getBlogByPage(pageNow, PAGE_SIZE, type,loginUser);
//        return ResultUtil.successMap(blogByPage,"查询成功");
//    }

    // 分页查询 博客文章
    @GetMapping("/homeBlogs")
    public Map<String,Object> homeBlogs( @RequestUser(required = false ) LoginUser loginUser) {
        List<BlogDTO> homeBlogs = blogService.getHomeBlogs(loginUser);
        return ResultUtil.successMap(homeBlogs,"查询成功");
    }


    // 用户点赞博客 博客的属性 kudos加一
    @PutMapping("/blogKudos")
    @Transactional // 保证数据一致性
    public Boolean blogKudos(@RequestParam("blogId")String blogId){
        Blog one = blogService.getOne(Wrappers.<Blog>lambdaQuery()
                .eq(Blog::getId, Long.valueOf(blogId))
                .last("for update") // 使用悲观锁保持数据的避免高斌发问题
        );
        boolean update = blogService.update(Wrappers.<Blog>lambdaUpdate()
                .eq(Blog::getId, Long.valueOf(blogId))
                .set(Blog::getKudos, one.getKudos() + 1)
        );

        return update;
    }

    // 用户取消点赞 博客的属性kudos减一

    @PutMapping("/cancelKudos")
    @Transactional // 原子性 一致性
    public Boolean cancelKudos(@RequestParam("blogId")String blogId){
        Blog one = blogService.getOne(Wrappers.<Blog>lambdaQuery()
                .eq(Blog::getId, Long.valueOf(blogId))
                .last("for update")  // 悲观锁
        );

        if(one.getKudos()==0){
            log.error("点赞数为0 出现异常");
            return false;
        }

        boolean update = blogService.update(Wrappers.<Blog>lambdaUpdate()
                .eq(Blog::getId, Long.valueOf(blogId))
                .set(Blog::getKudos, one.getKudos() - 1)
        );
        return update;
    }

    // 用户收藏博客 博客 收藏数加一

    @PutMapping("/blogStar")
    @Transactional // 作为一个事物提交
    public Boolean blogStar(@RequestParam("blogId")String blogId){
        Blog one = blogService.getOne(Wrappers.<Blog>lambdaQuery()
                .eq(Blog::getId, Long.valueOf(blogId))
                .last("for update") // 使用悲观锁 确认数据的一致性
        );
        boolean update = blogService.update(Wrappers.<Blog>lambdaUpdate()
                .eq(Blog::getId, Long.valueOf(blogId))
                .set(Blog::getStar, one.getStar() + 1)
        );

        return update;
    }

    @PutMapping("/cancelStar")
    @Transactional
    public Boolean cancelStar(@RequestParam("blogId")String blogId){
        Blog one = blogService.getOne(Wrappers.<Blog>lambdaQuery()
                .eq(Blog::getId, Long.valueOf(blogId))
                .last("for update") // 使用悲观锁 确认数据的一致性
        );

        if(one.getStar()==0){
            log.error("收藏数为0 出现异常");
            return false;
        }
        boolean update = blogService.update(Wrappers.<Blog>lambdaUpdate()
                .eq(Blog::getId, Long.valueOf(blogId))
                .set(Blog::getStar, one.getStar() - 1)
        );

        return update;
    }

    // 编写评论
    @PutMapping("/writeComment")
    @Transactional
    public Long writeComment(@RequestParam("userId") Long userId,
                                @RequestParam("blogId")Long blogId,
                                @RequestParam("context")String context){
        Date date = new Date();

        Comment build = Comment.builder()
                .blogId(blogId)
                .userId(userId)
                .context(context)
                .createTime(date)
                .updateTime(date)
                .isDelete(0)
                .isVisible(0)
                .build();
        boolean save = commentService.save(build);
        if(save){
            Blog one = blogService.getOne(Wrappers.<Blog>lambdaQuery()
                    .eq(Blog::getId, blogId)
                    .last("for update") // 悲观锁
            );

            blogService.update(Wrappers.<Blog>lambdaUpdate().eq(Blog::getId,blogId).set(Blog::getCommentNum,one.getCommentNum()+1));
            return build.getId(); // 返回评论id
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


    // 回复评论
    @PutMapping("/replyComment")
    @Transactional // 原子性
    public Long replyComment(@RequestParam("userId")Long userId,
                                @RequestParam("commentId")Long commentId,
                                @RequestParam("context")String context){
        Date date=new Date();

        Reply build = Reply.builder()
                .commentId(commentId)
                .userId(userId)
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
    public Map<String,Object> getComment(@RequestParam("BlogId")String blogId,@RequestParam(value = "userInfoKey",required = false)String userInfoKey){
        List<ReadCommentDTO> blogComment = blogService.getBlogComment(blogId,userInfoKey);
        return ResultUtil.successMap(blogComment,"评论查询成功");
    }

    // 改变评论点赞数
    @PutMapping("/changCommentKudos")
    public Boolean changCommentKudos(@RequestParam("commentId")Long commentId,@RequestParam("bytes") Byte bytes){
        return blogService.changeCommentKudos(commentId,bytes);
    }

    // 改变回复点赞数
    @PutMapping("/changReplyKudos")
    public Boolean changReplyKudos(@RequestParam("replyId")Long replyId,@RequestParam("bytes") Byte bytes){
        return blogService.changeReplyKudos(replyId,bytes);
    }

    // 获取博客作者的创作信息
    @GetMapping("/getBlogUserInfo")
    public List<Long> getBlogUserInfo(@RequestParam("userId") Long userId ){
        return blogService.getUserBlogNum(userId);
    }

    @GetMapping("/getBlogByName")
    public Map<String,Object> getBlogByName(@RequestParam("blogName") String blogName
            ,@RequestParam("current")int current){
        PageDTO<BlogDTO> blogByName = blogService.getBlogByName(current, PAGE_SIZE, blogName);
        if(Objects.isNull(blogByName)){
            return ResultUtil.failMap("参数不合法");
        }
        return ResultUtil.successMap(blogByName,"查询成功");
    }

    @GetMapping("/getBlogBySearch")
    public Map<String,Object> getBlogByUserId(@RequestParam("userId") Long userId,
                                              @RequestParam("current")int current,
                                              @RequestParam("pageSize") int pageSize,
                                              @RequestParam(value = "orderBy",required = false) String orderBy,
                                              @RequestParam(value = "orderWay",required = false) String orderWay,
                                              @RequestParam(value = "typeList",required = false) String typeList){
        PageDTO<BlogDTO> blogByName = blogService.getBlogByUserId(current, pageSize, userId,List.of(typeList.split(",")) ,orderBy,orderWay);
        if(Objects.isNull(blogByName)){
            return ResultUtil.failMap("查询失败");
        }
        return ResultUtil.successMap(blogByName,"查询成功");
    }

    @GetMapping("/getUserStarBlog")
    public Map<String,Object> getUserStarBlog(@RequestParam("blogs") List<Long> blogs
            ,@RequestParam("current")int current){
        PageDTO<BlogDTO> blogByIds = blogService.getBlogByIds(current, PAGE_SIZE, blogs);
        if(Objects.isNull(blogByIds)){
            return ResultUtil.failMap("参数不合法");
        }

        return ResultUtil.successMap(blogByIds,"查询成功");
    }

    @GetMapping("/getHotBlog")
    public Map<String,Object> getHotBlog(){
        try {
            List<BlogDTO> hotBlogs = blogService.getHotBlogs();
            System.out.println(hotBlogs);

            return ResultUtil.successMap(hotBlogs,"查询成功");
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResultUtil.failMap("查询失败");
        }
    }

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
    public Boolean updateBlogStatus(@RequestBody Map<String,Object> map,HttpServletRequest request) throws AuthenticationException {
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
    public PageDTO<CommentAdminVO> getCommentForAdmin( @RequestParam(value = "blogName",required = false) String blogName,
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
    public PageDTO<ReplyAdminVO> getReplyForAdmin( @RequestParam(value = "blogName",required = false) String blogName, //
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
    public Map<String,Object> reportBlogs(@RequestBody BlogReportVO blogReportVO,HttpServletRequest request) throws AuthenticationException {
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

