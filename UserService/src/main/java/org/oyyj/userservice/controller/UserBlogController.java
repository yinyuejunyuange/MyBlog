package org.oyyj.userservice.controller;

import com.alibaba.nacos.common.utils.UuidUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import feign.Response;
import feign.form.multipart.Output;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.pojo.LoginUser;
import org.oyyj.userservice.pojo.UserKudos;
import org.oyyj.userservice.pojo.UserStar;
import org.oyyj.userservice.service.IUserKudosService;
import org.oyyj.userservice.service.IUserService;
import org.oyyj.userservice.service.IUserStarService;
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
    public Map<String,Object> cancelKudos(@RequestParam("blogId") String blogId){
        // 将用户点赞表中的数据删除----删除操作指挥存在用户本人不存在并发情况
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();

        boolean user = userKudosService.deleteByMultiId(UserKudos.builder()
                .blogId(Long.valueOf(blogId))
                .userId(principal.getUser().getId())
                .build());

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

}
