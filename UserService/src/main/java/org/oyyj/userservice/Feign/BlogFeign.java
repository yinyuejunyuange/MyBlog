package org.oyyj.userservice.Feign;

import feign.Response;
import jakarta.servlet.http.HttpServletResponse;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.blogservice.dto.ImageResultDTO;
import org.oyyj.blogservice.pojo.Blog;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Objects;

@FeignClient("BlogService")
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

}
