package org.oyyj.userservice.controller;

import com.alibaba.nacos.common.utils.UuidUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import feign.Response;
import feign.form.multipart.Output;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.userservice.DTO.CommentDTO;
import org.oyyj.userservice.DTO.ReplyDTO;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.pojo.*;
import org.oyyj.userservice.service.*;
import org.oyyj.userservice.utils.RedisUtil;
import org.oyyj.userservice.utils.ResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.naming.AuthenticationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/myBlog/user/blog")
public class UserBlogController {

    private static final Logger log = LoggerFactory.getLogger(UserBlogController.class);
    @Autowired
    private IUserService userService;

    @Autowired
    private IUserStarService userStarService;

    @Autowired
    private IUserKudosService userKudosService;

    @Autowired
    private BlogFeign blogFeign;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IUserCommentService userCommentService;

    @Autowired
    private IUserReplyService userReplyService;


    // 用户查找
        // 根据分类查找
    @GetMapping("/getBlogList")
    public Map<String,Object> getBlogList(@RequestParam int pageNow,@RequestParam(required = false) String type){
        return blogFeign.getBlogListByPage(pageNow, type);
    }


    // 用户编写

    @PostMapping("/write")
    public Map<String,Object> UserWrite(@RequestBody BlogDTO blogDTO) throws IOException {
        return userService.saveBlog(blogDTO);
    }
    // 用户评论

    // 用户阅读

    @GetMapping("/read")
    public Map<String,Object> UserRead(@RequestParam("id") String id) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 判断当前用户是否登录
        if(!authentication.isAuthenticated()){
           log.warn("当前用户未登录");
           return userService.readBlog(id,null);

        }

        UsernamePasswordAuthenticationToken userAuthentication = (UsernamePasswordAuthenticationToken) authentication;

        LoginUser principal = (LoginUser) userAuthentication.getPrincipal();
        Long userId = principal.getUser().getId();

        // 利用uuid生成一个key值
        String userInfoKey = UUID.randomUUID().toString();

        redisUtil.set(userInfoKey,userId,1, TimeUnit.MINUTES);// 存储1分钟

        return userService.readBlog(id,userInfoKey);
    }

    // 上传图片
    @CrossOrigin // 允许跨域
    @RequestMapping("/file/upload")
    public Object uploadImage(@RequestParam("image")MultipartFile file){
        return userService.uploadPict(file);
    }



    // 下载图片
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
//    public void downloadFile(@PathVariable("fileName")String fileName, HttpServletResponse response){
//        userService.downloadFile(fileName,response);
//    }

    // 用户点赞
    @PutMapping("/kudos")
    public Map<String,Object> kudosBlog(@RequestParam("blogId") String blogId){

        System.out.println("123123123:"+blogId);

        // 1. 将用户 点赞表中添加用户和博客对应的id
        boolean userKudos = userService.userKudos(blogId);
        // 2. 修改博客的信息 将博客中的kudos加一
        Boolean blogKudos = blogFeign.blogKudos(blogId);

        if(userKudos&&blogKudos){
            return ResultUtil.successMap(true,"点赞成功");
        }else{
            return ResultUtil.failMap("点赞失败");
        }
    }



    // 用户取消点赞
    @PutMapping("/cancelKudos")
    public Map<String,Object> cancelKudos(@RequestParam("blogId") String blogId){
        // 将用户点赞表中的数据删除----删除操作指挥存在用户本人不存在并发情况
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();

//        boolean user = userKudosService.deleteByMultiId(UserKudos.builder()
//                .blogId(Long.valueOf(blogId))
//                .userId(principal.getUser().getId())
//                .build());

        boolean user = userKudosService.remove(Wrappers.<UserKudos>lambdaQuery()
                .eq(UserKudos::getUserId, principal.getUser().getId())
                .eq(UserKudos::getBlogId, Long.valueOf(blogId))
        );

        // 对应博客的点赞数减一
        Boolean blog = blogFeign.cancelKudos(blogId);

        if(user&&blog){
            return ResultUtil.successMap(true,"取消成功");
        }else{
            return ResultUtil.failMap("取消失败");
        }


    }

    // 判断当前用户是否点赞
    @GetMapping("/isUserKudos")
    public boolean isUserKudos(@RequestParam("blogId") Long blogId,@RequestParam("userInfoKey") String userInfoKey, HttpServletRequest request)  {

        try {
            String source = request.getHeader("source");
            if(source==null||!source.equals("BLOGSERVICE")){
                System.out.println(source);
                throw new AuthenticationException("请求来源不正确");
            }

            String s = String.valueOf(redisUtil.get(userInfoKey)) ;
            Long id=Long.valueOf(s); // 获取存储的信息
            UserKudos one = userKudosService.getOne(Wrappers.<UserKudos>lambdaQuery().eq(UserKudos::getUserId, id)
                    .eq(UserKudos::getBlogId, blogId));
            if(Objects.isNull(one)){
                return false;
            }

            return true;
        } catch (AuthenticationException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    // 用户收藏
    @PutMapping("/userStar")
    public Map<String,Object> userStar(@RequestParam("blogId") String blogId){
        // 用户收藏表中添加对应的用户博客id；
        boolean userStar = userService.userStar(blogId);
        // 修改博客信息中star加一
        Boolean blogStar = blogFeign.blogStar(blogId);

        if(userStar&&blogStar){
            return ResultUtil.successMap(true,"收藏成功");
        }else{
            return ResultUtil.failMap("收藏失败");
        }
    }

    // 用户取消收藏
    @PutMapping("/cancelStar")
    public Map<String,Object> cancelStar(@RequestParam("blogId") String blogId){
        // 将用户点赞表中的数据删除----删除操作指挥存在用户本人不存在并发情况
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();

//        boolean user = userKudosService.remove(Wrappers.<UserKudos>lambdaQuery()
//                .eq(UserKudos::getUserId, principal.getUser().getId())
//                .eq(UserKudos::getBlogId, Long.valueOf(blogId))
//        );

        boolean user = userStarService.remove(Wrappers.<UserStar>lambdaQuery()
                .eq(UserStar::getUserId, principal.getUser().getId())
                .eq(UserStar::getBlogId, Long.valueOf(blogId))
        );

        // 对应博客的点赞数减一
        Boolean blog = blogFeign.cancelStar(blogId);

        if(user&&blog){
            return ResultUtil.successMap(true,"取消成功");
        }else{
            return ResultUtil.failMap("取消失败");
        }


    }

    // 判断当前用户是否收藏
    @GetMapping("/isUserStar")
    public Boolean isUserStar(@RequestParam("blogId") Long blogId,@RequestParam("userInfoKey")String userInfoKey,HttpServletRequest request){
        try {
            String source = request.getHeader("source");
            if(source==null||!source.equals("BLOGSERVICE")){
                throw new AuthenticationException("请求来源不正确");
            }


            String s = String.valueOf(redisUtil.get(userInfoKey));
            Long id=Long.valueOf(s);
            UserStar one = userStarService.getOne(Wrappers.<UserStar>lambdaQuery().eq(UserStar::getUserId, id)
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

    // 用户添加评论
    @PostMapping("/addComment")
    public Map<String,Object> writeComment(@RequestBody CommentDTO commentDTO){
        if(Objects.isNull(commentDTO)){
            return ResultUtil.failMap("参数不可为空");
        }

        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        boolean authenticated = authentication.isAuthenticated();
        if(!authenticated){
            return ResultUtil.failMap("未授权 请重新登录");
        }
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        commentDTO.setUserId(String.valueOf(principal.getUser().getId()));

        Long b = userService.addComment(commentDTO);
        if(!Objects.isNull(b)){

            return ResultUtil.successMap(String.valueOf(b),"评论添加成功");
        }else{
            return ResultUtil.failMap("评论添加失败");
        }
    }

    // 用户回复评论
    @PostMapping("/replyComment")
    public Map<String,Object> replyComment(@RequestBody ReplyDTO replyDTO){
        if(Objects.isNull(replyDTO)){
            return ResultUtil.failMap("参数不可为空");
        }

        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        boolean authenticated = authentication.isAuthenticated();
        if(!authenticated){
            return ResultUtil.failMap("未授权 请重新登录");
        }
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        replyDTO.setUserId(String.valueOf(principal.getUser().getId()));

        Long b = userService.addReply(replyDTO);
        if(!Objects.isNull(b)){
            return ResultUtil.successMap(String.valueOf(b),"回复添加成功");
        }else{
            return ResultUtil.failMap("回复添加失败");
        }
    }

    // 用户获取评论

    @GetMapping("/getComment")
    public Map<String,Object> getComment(@RequestParam("blogId")String blogId){
        Authentication authentications = SecurityContextHolder.getContext().getAuthentication();
        if(authentications.isAuthenticated()){
            // 用户已经登录
            UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            LoginUser principal = (LoginUser) authentication.getPrincipal();
            // 获得临时uuid
            String uuid = UUID.randomUUID().toString();
            redisUtil.set(uuid,principal.getUser().getId(),1,TimeUnit.MINUTES);
            return blogFeign.getComment(blogId,uuid);
        }
        // 用户没有登录

        return blogFeign.getComment(blogId,null);
    }

    // 用户点赞评论
    @GetMapping("/kudosComment")
    public Map<String,Object> kudosComment(@RequestParam("commentId")String commentId,@RequestParam("bytes")Byte bytes){

        if(bytes!=1&&bytes!=2){
            log.error("bytes 参数错误："+bytes);
            return ResultUtil.failMap("参数错误");
        }

        Boolean b = userService.kudosComment(commentId,bytes);
        if(b){
            return ResultUtil.successMap(true,"操作成功");
        }else{
            return ResultUtil.failMap("操作失败");
        }

    }
    // 用户点赞回复
    @GetMapping("/kudosReply")
    public Map<String,Object> kudosReply(@RequestParam("replyId")String replyId,@RequestParam("bytes")Byte bytes){

        if(bytes!=1&&bytes!=2){
            log.error("bytes 参数错误："+bytes);
            return ResultUtil.failMap("参数错误");
        }
        Boolean b = userService.kudosReply(replyId, bytes);

        if(b){
            return ResultUtil.successMap(true,"操作成功");
        }else{
            return ResultUtil.failMap("操作失败");
        }
    }

    // 判断用户是否点赞评论
    @GetMapping("/getUserKudosComment")
    public Boolean getUserKudosComment(@RequestParam("commentId")String commentId,@RequestParam("userInfoKey")String userInfoKey,HttpServletRequest request){
        try {
            String source = request.getHeader("source");
            if(source==null||!source.equals("BLOGSERVICE")){
                throw new AuthenticationException("请求来源不正确");
            }

            // 获取用户id
            String s = String.valueOf(redisUtil.get(userInfoKey));
            Long userId = Long.valueOf(s);

            // 判断 用户是否点赞此评论
            UserComment one = userCommentService.getOne(Wrappers.<UserComment>lambdaQuery()
                    .eq(UserComment::getCommentId, Long.valueOf(commentId))
                    .eq(UserComment::getUserId, userId)
            );

            return !Objects.isNull(one);
        } catch (AuthenticationException e) {
            throw new RuntimeException(e);
        }
    }

    // 判断用户是否点赞回复
    @GetMapping("/getUserKudosReply")
    public Boolean getUserKudosReply(@RequestParam("replyId")String replyId,@RequestParam("userInfoKey")String userInfoKey,HttpServletRequest request){
        try {
            String source = request.getHeader("source");
            if(source==null||!source.equals("BLOGSERVICE")){
                throw new AuthenticationException("请求来源不正确");
            }

            // 获取用户id
            String s = String.valueOf(redisUtil.get(userInfoKey));
            Long userId = Long.valueOf(s);

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

    // 搜索博客 名称模糊搜做

    @GetMapping("/getBlogByName")
    public Map<String,Object> getBlogByName(@RequestParam("blogName") String blogName
            ,@RequestParam("current")int current){
        return blogFeign.GetBlogByName(blogName,current);
    }

    // 类型搜索

    @GetMapping("/getBlogByTypeList")
    public Map<String,Object> getBlogByTypeList(@RequestParam("typeList") List<String> typeList
            ,@RequestParam("current")int current){
        log.info("123123123");
        return blogFeign.GetBlogByTypeList(typeList,current);
    }
    // 作者搜索
    @GetMapping("/getBlogByUserId")
    public Map<String,Object> getBlogByUserId(@RequestParam("userId") String userId
            ,@RequestParam("current")int current){
        return blogFeign.GetBlogByUserId(Long.valueOf(userId),current);
    }



}
