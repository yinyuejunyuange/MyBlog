package org.oyyj.userservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.userservice.DTO.BlogUserInfoDTO;
import org.oyyj.userservice.DTO.ChangeUserDTO;
import org.oyyj.userservice.DTO.RegisterDTO;
import org.oyyj.userservice.DTO.UserDTO;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.pojo.JWTUser;
import org.oyyj.userservice.pojo.LoginUser;
import org.oyyj.userservice.pojo.User;
import org.oyyj.userservice.service.IUserService;
import org.oyyj.userservice.utils.ResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/myBlog/user")
public class UserController {


    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private IUserService userService;
    @Autowired
    private ServletContext servletContext;



    // 用户登录
    @PostMapping("/login")
    public Map<String,Object> UserLogin(@RequestBody UserDTO userDTO) throws JsonProcessingException {
        JWTUser login = userService.login(userDTO.getUsername(), userDTO.getPassword());
        return ResultUtil.successMap(login,"登录成功");
    }

    // 用户注册
    @PostMapping("/register")
    public Map<String,Object> UserRegister(@RequestBody RegisterDTO registerDTO) throws IOException {
        JWTUser jwtUser = userService.registerUser(registerDTO);
        if(Objects.isNull(jwtUser)){
            return ResultUtil.failMap("用户名重复 请重新注册");
        }
        return ResultUtil.successMap(jwtUser,"注册成功 已登录");
    }


    // 用户登出
    @GetMapping("/logout")
    public Map<String,Object> UserLogout() {
        userService.LoginOut();
        return ResultUtil.successMap(null,"退出成功");
    }


    // 用户存储 头像
    @RequestMapping("/makeHead")
    public Map<String,Object> makeUserHead(@RequestParam("file")MultipartFile file) throws IOException {
        String fileName= UUID.randomUUID().toString().substring(0,10)+file.getOriginalFilename();
        String filePath= ResourceUtils.getURL("classpath:").getPath()+"static/image/"+fileName;

        FileUtils.copyInputStreamToFile(file.getInputStream(),new File(servletContext.getContextPath()+"/"+filePath));


        // 从上下文环境获取到数据
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();

        // 存储用户头像
        boolean update = userService.update(Wrappers.<User>lambdaUpdate().eq(User::getId,principal.getUser().getId())
                .set(User::getImageUrl, fileName));// 存储用户头像
        if(update){
            return ResultUtil.successMap("http://localhost:8080/myBlog/user/getHead/"+fileName,"存储成功");
        }

        return ResultUtil.failMap("存储失败");

    }


    // 获取用户头像的方法
    @GetMapping("/getHead/{fileName}")
    public void getUserHead(@PathVariable("fileName") String fileName , HttpServletResponse response) throws IOException {
        String filePath= ResourceUtils.getURL("classpath:").getPath()+"static/image/"+fileName;
        String encodedFileName = URLEncoder.encode(filePath, StandardCharsets.UTF_8); // 避免有中文名 设置字符
        System.out.println("head path:"+filePath);

        File file=new File(filePath);
        if(!file.exists()){
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return ;
        }

        // 设置响应头
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+encodedFileName+"\"");

        Files.copy(file.toPath(),response.getOutputStream());
        response.getOutputStream().flush();
    }

    @GetMapping(value = "/getUserName")
    public Map<Long,String> getAllUserName(HttpServletRequest request){
        String source = request.getHeader("source");
        //System.out.println(source+":================");
        if(source==null||!source.equals("BLOGSERVICE")){
            log.error("请求来源错误");
            return null;
        }
        Map<Long,String> maps=userService.list().stream().collect(Collectors.toMap(User::getId, User::getName));
        System.out.println(maps);
        return userService.list().stream().collect(Collectors.toMap(User::getId, User::getName));
    }

    //给其他服务调用以获取用户名

    @GetMapping("/getNameInIds")
    public Map<Long,String> getUserNameInIds(@RequestParam("ids") List<String>ids, HttpServletRequest request){
        String source = request.getHeader("source");
        //System.out.println(source+":================");
        if(source==null||!source.equals("BLOGSERVICE")){
            log.error("请求来源错误");
            return null;
        }
        if(ids.isEmpty()){
            return null;
        }
        Map<Long, String> collect = userService.list(Wrappers.<User>lambdaQuery()
                        .in(User::getId, ids.stream().map(Long::valueOf).toList()))
                .stream().collect(Collectors.toMap(User::getId, User::getName));

        System.out.println(collect);
        return collect;
    }

    /**
     * 给其他服务调用 以获取用户头像
     * @param ids
     * @param request
     * @return
     */
    @GetMapping("/getImageInIds")
    public Map<Long,String> getUserImageInIds(@RequestParam("ids") List<String>ids, HttpServletRequest request){
        String source = request.getHeader("source");
        //System.out.println(source+":================");
        if(source==null||!source.equals("BLOGSERVICE")){
            log.error("请求来源错误");
            return null;
        }
        if(ids.isEmpty()){
            return null;
        }

        Map<Long, String> collect = userService.list(Wrappers.<User>lambdaQuery()
                        .in(User::getId, ids.stream().map(Long::valueOf).toList()))
                .stream().collect(Collectors.toMap(User::getId, User::getImageUrl));

        System.out.println(collect);
        return collect;
    }


    @GetMapping("/getBlogUserInfo")
    public Map<String,Object> getBlogUserInfo(@RequestParam("userId")String userId){
        BlogUserInfoDTO blogUserInfo = userService.getBlogUserInfo(userId);
        if(blogUserInfo==null){
            return ResultUtil.failMap("参数不合法");
        }
        return ResultUtil.successMap(blogUserInfo,"数据查询成功");
    }

    // 用户关注作者
    @PutMapping("/starBlogAuthor")
    public Map<String,Object> starBlogAuthor(@RequestParam("authorId")String authorId){

        Boolean b = userService.starBlogAuthor(authorId);
        if(b){
            return ResultUtil.successMap(b,"关注成功");
        }else{
            return ResultUtil.failMap("关注失败");
        }
    }

    @PutMapping("/cancelStarBlogAuthor")
    public Map<String,Object> cancelStarBlogAuthor(@RequestParam("authorId")String authorId){

        Boolean b = userService.cancelStarBlogAuthor(authorId);
        if(b){
            return ResultUtil.successMap(b,"取消关注成功");
        }else{
            return ResultUtil.failMap("操作失败");
        }
    }

//    // 用户改变个人信息
//
    @PostMapping("/changeUserInfo")
    public Map<String,Object> changeUserInfo(@RequestBody ChangeUserDTO changeUserDTO){
        return userService.changeUserInfo(changeUserDTO);
    }


    /**
     * 热门搜索
     */
    @GetMapping("/getHotSearch")
    public Map<String,Object> getHotSearch(){
        return userService.getHotSearch();
    }

    /**
     * 用户的搜索
     */
    @GetMapping("/getUserSearch")
    public Map<String,Object> getUserSearch(){
        List<String> userSearch = userService.getUserSearch();
        return ResultUtil.successMap(userSearch,"查询成功");
    }

    // 用户删除自己的搜索记录
    @DeleteMapping("deleteUserSearchByName")
    public Map<String,Object> deleteUserSearchByName( @RequestParam("name") String name){
        boolean b = userService.deleteUserSearchByName(name);
        if(b){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }

    @DeleteMapping("deleteUserAllSearch")
    public Map<String,Object> deleteUserAllSearch( ){
        boolean b = userService.deleteUserAllSearch();
        if(b){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }




}
