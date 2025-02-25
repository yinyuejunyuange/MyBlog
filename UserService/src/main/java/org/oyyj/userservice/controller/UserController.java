package org.oyyj.userservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.userservice.DTO.RegisterDTO;
import org.oyyj.userservice.DTO.UserDTO;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.pojo.JWTUser;
import org.oyyj.userservice.pojo.LoginUser;
import org.oyyj.userservice.pojo.User;
import org.oyyj.userservice.service.IUserService;
import org.oyyj.userservice.utils.ResultUtil;
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
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/myBlog/user")
public class UserController {



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
            return ResultUtil.successMap(null,"存储成功");
        }

        return ResultUtil.failMap("存储失败");

    }


    // 获取用户头像的方法
    @GetMapping("/getHead/{fileName}")
    public void getUserHead(@PathVariable("fileName") String fileName , HttpServletResponse response) throws IOException {
        String filePath= ResourceUtils.getURL("classpath").getPath()+"static/image/"+fileName;
        System.out.println("head path:"+filePath);

        File file=new File(filePath);
        if(!file.exists()){
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return ;
        }

        // 设置响应头
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filePath+"\"");

        Files.copy(file.toPath(),response.getOutputStream());
        response.getOutputStream().flush();
    }


}
