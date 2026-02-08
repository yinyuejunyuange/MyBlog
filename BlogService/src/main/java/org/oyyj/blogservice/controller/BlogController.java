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
import org.oyyj.blogservice.vo.blogs.CommendBlogsByAuthor;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommon.config.RabbitMqConfig;
import org.oyyj.mycommon.pojo.dto.UserBlogInfoDTO;
import org.oyyj.mycommon.service.IUploadMetadataService;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/myBlog/blog")
public class BlogController {

    private static final Logger log = LoggerFactory.getLogger(BlogController.class);
    private final int PAGE_SIZE = 6;


    @Autowired
    private IBlogService blogService;


    @Autowired
    private ServletContext servletContext; // 应用上下文



    @Autowired
    private IUploadMetadataService uploadMetadataService;

    @CrossOrigin // 允许此方法跨域
    @PostMapping("/write")
    public Map<String, Object> writeBlog(@RequestBody BlogDTO blogDTO ,@RequestUser LoginUser loginUser ) {
        Date date=new Date();

        Blog build = Blog.builder()
                .title(blogDTO.getTitle())
                .context(blogDTO.getContext())
                .userId(loginUser.getUserId())
                .createTime(date)
                .updateTime(date)
                .status(blogDTO.getStatus())
                .typeList(blogDTO.getTypeList())
                .introduce(blogDTO.getIntroduce())
                .isDelete(0)
                .build();

        boolean success = blogService.saveBlog(build);
        if(!success){
            log.error("用户添加博客失败 userId:{}",loginUser.getUserId());
        }
        return success
                ? ResultUtil.successMap(build.getId(),"博客保存成功")
                : ResultUtil.failMap("服务繁忙请稍后重试");
    }

    /**
     * 阅读博客
     * @param id
     * @param loginUser
     * @return
     */
    @GetMapping("/read")
    public Map<String, Object> readBlog(@RequestParam("id") String  id,@RequestUser(required = false) LoginUser loginUser) {
        ReadDTO readDTO = blogService.ReadBlog(Long.valueOf(id),loginUser);
        if(Objects.isNull(readDTO)){
            return ResultUtil.failMap("查询失败");
        }
        return ResultUtil.successMap(readDTO,"查询成功");
    }

    /**
     * 博客有效阅读
     * @param blogId
     * @param loginUser
     * @return
     */
    @GetMapping("/valid/read")
    public Map<String,Object> validBlogRead(@RequestParam("blogId") String blogId,@RequestUser() LoginUser loginUser) throws Exception {
        blogService.readBlogValid(Long.valueOf(blogId),loginUser);
        return ResultUtil.successMap(null,"阅读成功");
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
    // 博客文章
    @GetMapping("/homeBlogs")
    public Map<String,Object> homeBlogs( @RequestUser(required = false ) LoginUser loginUser) {
        List<BlogDTO> homeBlogs = blogService.getHomeBlogs(loginUser);
        return ResultUtil.successMap(homeBlogs,"查询成功");
    }


    // 用户点赞博客 博客的属性 kudos加一
    @PutMapping("/blogKudos")
    @Transactional // 保证数据一致性
    public Boolean blogKudos(@RequestParam("blogId")String blogId, @RequestUser() LoginUser loginUser){
        return blogService.blogKudos(Long.valueOf(blogId),loginUser);
    }

    // 用户取消点赞 博客的属性kudos减一

    @PutMapping("/cancelKudos")
    @Transactional // 原子性 一致性
    public Boolean cancelKudos(@RequestParam("blogId")String blogId , @RequestUser() LoginUser loginUser){
        return blogService.cancelKudos(Long.valueOf(blogId),loginUser);
    }

    // 用户收藏博客 博客 收藏数加一

    @PutMapping("/blogStar")
    public Boolean blogStar(@RequestParam("blogId")String blogId , @RequestUser() LoginUser loginUser){
        return blogService.blogStar(Long.valueOf(blogId), loginUser);
    }

    @PutMapping("/cancelStar")
    @Transactional
    public Boolean cancelStar(@RequestParam("blogId")String blogId, @RequestUser() LoginUser loginUser){
        return blogService.cancelStar(Long.valueOf(blogId),loginUser);
    }


    // 获取博客作者的创作信息
    @GetMapping("/getBlogUserInfo")
    public List<Long> getBlogUserInfo(@RequestParam("userId") Long userId ){
        return blogService.getUserBlogNum(userId);
    }

    // todo 修改成为全文检索
    @GetMapping("/getBlogByName")
    public Map<String,Object> getBlogByName(@RequestParam("blogName") String blogName
            ,@RequestParam("current")int current){
        PageDTO<BlogDTO> blogByName = blogService.getBlogByName(current, PAGE_SIZE, blogName);
        if(Objects.isNull(blogByName)){
            return ResultUtil.failMap("参数不合法");
        }
        return ResultUtil.successMap(blogByName,"查询成功");
    }

    /**
     * 根据博客的作者信息 获取 最近博客以及最欢迎博客  同时排除当前博客
     * @param userId 作者Id
     * @param currentId 当前博客ID
     * @return
     */
    @GetMapping("/commendBlogByAuthor")
    public ResultUtil<CommendBlogsByAuthor> commendBlogBySelect(@RequestParam("userId")Long  userId,
                                                                @RequestParam("currentId")Long  currentId ){
        return ResultUtil.success(blogService.commendBlogsByAuthor(userId, currentId));
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

    // todo 测试文件上传 待会删除
    @PostMapping("/testUploadFile")
    public Map<String,Object> testUploadFile(@RequestPart("file")MultipartFile file,
                                             @RequestPart("fileUploadDTO")FileUploadDTO fileUploadDTO ){
        try {
            fileUploadDTO.setFile(file);
            blogService.uploadFileChunk(fileUploadDTO);
            return ResultUtil.successMap(null,"分片上传成功成功");
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResultUtil.failMap("查询失败");
        }
    }

    // todo 测试文件上传 待会删除
    @PostMapping("/testMergeFile")
    public Map<String,Object> testMergeFile( @RequestBody FileMergeDTO filemergeDTO){
        try {
            blogService.mergeFileChunk(filemergeDTO.getFileNo(), filemergeDTO.getTotalFileChunks(), filemergeDTO.getOrgFileName());
            return ResultUtil.successMap(null,"分片上传成功成功");
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResultUtil.failMap("查询失败");
        }
    }

    // todo 测试文件上传 待会删除
    @PostMapping("/testExistFile")
    public Map<String,Object> testExistFile( @RequestParam("fileNo")String fileNo){
        try {
            List<Long> existsChunks = uploadMetadataService.getExistsChunks(fileNo);
            return ResultUtil.successMap(existsChunks,"分片上传成功成功");
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResultUtil.failMap("查询失败");
        }
    }

    /**
     * 根据用户ID获取其博客相关信息 如 点赞数 关注数
     * @param userId
     * @return
     */
    @GetMapping("/userBlogInfo")
    public UserBlogInfoDTO getUserBlogInfo(@RequestParam("userId") Long userId){
        return  blogService.getUserBlogInfo(userId);
    }


    @GetMapping("/getByKeyWords")
    public ResultUtil<List<BlogDTO>> getBlogByKeyWords(@RequestParam("keyWord") String keyWord,
                                                       @RequestParam("pageNum") Integer pageNum,
                                                       @RequestParam("pageSize") Integer pageSize){
        return blogService.getBlogByKeyWord(keyWord,pageNum,pageSize);
    }

}

