package org.oyyj.blogservice.controller;

import ch.qos.logback.core.util.FileUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.oyyj.blogservice.dto.*;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.service.ICommentService;
import org.oyyj.blogservice.service.IReplyService;
import org.oyyj.blogservice.util.ResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
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

    @GetMapping("/getBlogByTypeList")
    public Map<String,Object> getBlogByTypeList(@RequestParam("typeList") List<String> typeList
            ,@RequestParam("current")int current){
        PageDTO<BlogDTO> blogByName = blogService.getBlogByTypeList(current, PAGE_SIZE, typeList);
        if(Objects.isNull(blogByName)){
            return ResultUtil.failMap("参数不合法");
        }
        return ResultUtil.successMap(blogByName,"查询成功");
    }

    @GetMapping("/getBlogByUserId")
    public Map<String,Object> getBlogByUserId(@RequestParam("userId") Long userId
            ,@RequestParam("current")int current){
        PageDTO<BlogDTO> blogByName = blogService.getBlogByUserId(current, PAGE_SIZE, userId);
        if(Objects.isNull(blogByName)){
            return ResultUtil.failMap("参数不合法");
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


}

