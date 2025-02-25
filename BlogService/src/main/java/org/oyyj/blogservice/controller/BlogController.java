package org.oyyj.blogservice.controller;

import ch.qos.logback.core.util.FileUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.blogservice.dto.ImageResultDTO;
import org.oyyj.blogservice.dto.ReadDTO;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Autowired
    private IBlogService blogService;

    @Autowired
    private ServletContext servletContext; // 应用上下文

    @CrossOrigin // 允许此方法跨域
    @PostMapping("/write")
    public Map<String, Object> writeBlog(@RequestBody BlogDTO blogDTO) {
        Date date=new Date();

        Blog build = Blog.builder()
                .title(blogDTO.getTitle())
                .context(blogDTO.getContext())
                .userId(blogDTO.getUserId())
                .createTime(date)
                .updateTime(date)
                .status(blogDTO.getStatus())
                .typeList(blogDTO.getTypeList())
                .introduce(blogDTO.getIntroduce())
                .isDelete(0)
                .build();

        blogService.saveBlog(build);
        return ResultUtil.successMap(null,"博客保存成功");
    }

    @GetMapping("/read")
    public Map<String, Object> readBlog(@RequestParam String  id) {
        ReadDTO readDTO = blogService.ReadBlog(Long.valueOf(id));
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


        String uploadPath= ResourceUtils.getURL("classpath:").getPath()+"static/image/"+fileName;

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
        String filePath= ResourceUtils.getURL("classpath:").getPath()+"static/image/"+fileName;
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

}
