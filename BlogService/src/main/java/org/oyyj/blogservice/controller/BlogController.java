package org.oyyj.blogservice.controller;

import ch.qos.logback.core.util.FileUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.blogservice.dto.ImageResultDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.dto.ReadDTO;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/blog")
public class BlogController {

    private final int PAGE_SIZE = 6;

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
                .userId(Long.parseLong(blogDTO.getUserId()))
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
    public Map<String, Object> readBlog(@RequestParam("blogId") String  id,@RequestParam(value = "userInfoKey",required = false) String userInfoKey) {
        ReadDTO readDTO = blogService.ReadBlog(Long.valueOf(id),userInfoKey);
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

    // 分页查询 博客文章
    @GetMapping("/list")
    // 从1 开始查询
    public Map<String,Object> getBlogList(@RequestParam int pageNow,@RequestParam(required = false) String type){
        PageDTO<BlogDTO> blogByPage = blogService.getBlogByPage(pageNow, PAGE_SIZE, type);
        return ResultUtil.successMap(blogByPage,"查询成功");
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
        boolean update = blogService.update(Wrappers.<Blog>lambdaUpdate()
                .eq(Blog::getId, Long.valueOf(blogId))
                .set(Blog::getKudos, one.getKudos() - 1)
        );
        return update;
    }



}
