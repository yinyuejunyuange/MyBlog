package org.oyyj.userservice.Feign;

import feign.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.oyyj.userservice.DTO.*;
import org.oyyj.userservice.config.FeignUserConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.security.sasl.AuthenticationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@FeignClient(value = "BlogService",configuration = FeignUserConfig.class)
public interface BlogFeign {

    @PostMapping("/blog/write")
    Map<String,Object> writeBlog(@RequestBody BlogDTO blogDTO);

    @GetMapping("/blog/read")
    Map<String,Object> readBlog(@RequestParam("blogId") String id,@RequestParam(value ="userInfoKey",required = false ) String userInfoKey);

    @PostMapping(value = "/blog/file/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageResultDTO uploadPict(@RequestPart("image") MultipartFile file);

    @GetMapping("/blog/file/download/{fileName}")
    void download(@PathVariable() String fileName, HttpServletResponse response);

    @GetMapping("/blog/file/download/{fileName}")
    Response getFile(@PathVariable() String fileName);

    @GetMapping("/blog/list")
    Map<String,Object> getBlogListByPage(@RequestParam int pageNow ,@RequestParam(required = false) String type);


    @PutMapping("/blog/blogKudos")
    Boolean blogKudos(@RequestParam("blogId")String blogId);

    @PutMapping("/blog/cancelKudos")
    Boolean cancelKudos(@RequestParam("blogId")String blogId);

    @PutMapping("/blog/blogStar")
    Boolean blogStar(@RequestParam("blogId")String blogId);

    @PutMapping("/blog/cancelStar")
    Boolean cancelStar(@RequestParam("blogId")String blogId);

    @PutMapping("/blog/writeComment")
    Long writeComment(@RequestParam("userId") Long userId,
                         @RequestParam("blogId")Long blogId,
                         @RequestParam("context")String context);

    @PutMapping("/blog/replyComment")
    Long replyComment(@RequestParam("userId")Long userId,
                         @RequestParam("commentId")Long commentId,
                         @RequestParam("context")String context);

    @DeleteMapping("/blog/removeComment")
    Boolean removeComment(@RequestParam("commentId")Long commentId);

    @DeleteMapping("/blog/removeReply")
    Boolean removeReply(@RequestParam("replyId")Long replyId);

    @GetMapping("/blog/getComment")
    Map<String,Object> getComment(@RequestParam("BlogId")String blogId,@RequestParam(value = "userInfoKey",required = false)String userInfoKey);


    // 改变评论点赞数
    @PutMapping("/blog/changCommentKudos")
    Boolean changCommentKudos(@RequestParam("commentId")Long commentId,@RequestParam("bytes") Byte bytes);
    // 改变回复点赞数
    @PutMapping("/blog/changReplyKudos")
    Boolean changReplyKudos(@RequestParam("replyId")Long replyId,@RequestParam("bytes") Byte bytes);

    @GetMapping("/blog/getBlogUserInfo")
    List<Long> getBlogUserInfo(@RequestParam("userId") Long userId );

    @GetMapping("/blog/getBlogByName")
    Map<String,Object> GetBlogByName(@RequestParam("blogName") String blogName
            ,@RequestParam("current")int current);

    @GetMapping("/blog/getBlogByTypeList")
    Map<String,Object> GetBlogByTypeList(@RequestParam("typeList") List<String> typeList
            ,@RequestParam("current")int current);

    @GetMapping("/blog/getBlogByUserId")
   Map<String,Object> GetBlogByUserId(@RequestParam("userId") Long userId
            ,@RequestParam("current")int current);

    @GetMapping("/blog/getUserStarBlog")
   Map<String,Object> getUserStarBlog(@RequestParam("blogs") List<Long> blogs
            ,@RequestParam("current")int current);

    @GetMapping("/blog/getHotBlog")
    Map<String,Object> getHotBlog();

    // 举报博客
    @PutMapping("/blog/reportBlog")
    Map<String,Object> reportBlogs(@RequestBody BlogReportDTO blogReportVO) throws AuthenticationException ;


    // 举报评论
    @PutMapping("/blog/reportComment")
    Map<String,Object> reportComments(@RequestBody CommentReportDTO commentReportVO) throws AuthenticationException;

    // 举报回复
    @PutMapping("/blog/reportReply")
    Map<String,Object> reportReply(@RequestBody ReplyReportDTO replyReportVO) throws AuthenticationException;
}
