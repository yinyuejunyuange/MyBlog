package org.oyyj.userservice.controller;

import feign.Response;
import feign.form.multipart.Output;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@RestController
@RequestMapping("/myBlog/user/blog")
public class UserBlogController {

    @Autowired
    private IUserService userService;

    @Autowired
    private BlogFeign blogFeign;


    // 用户查找

    // 用户编写

    @PostMapping("/write")
    public Map<String,Object> UserWrite(@RequestBody BlogDTO blogDTO) throws IOException {
        return userService.saveBlog(blogDTO);
    }
    // 用户评论

    // 用户阅读

    @GetMapping("/read")
    public Map<String,Object> UserRead(@RequestParam("id") String id) throws IOException {
        return userService.readBlog(id);
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

    // 用户收藏


}
