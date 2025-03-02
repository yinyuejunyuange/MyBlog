package org.oyyj.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletResponse;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.userservice.DTO.BlogUserInfoDTO;
import org.oyyj.userservice.DTO.CommentDTO;
import org.oyyj.userservice.DTO.RegisterDTO;
import org.oyyj.userservice.DTO.ReplyDTO;
import org.oyyj.userservice.pojo.JWTUser;
import org.oyyj.userservice.pojo.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;


public interface IUserService extends IService<User> {

    JWTUser login(String username, String password) throws JsonProcessingException;

    // 登出
    void LoginOut();

    JWTUser registerUser(RegisterDTO registerDTO) throws IOException;

    Map<String,Object> saveBlog(BlogDTO blogDTO);

    Map<String,Object> readBlog(String blogId,String userInfoKey);

    Object uploadPict(MultipartFile file);

    void downloadFile(String fileName, HttpServletResponse response);

    boolean userKudos(String blogId);

    boolean userStar(String blogId);

    Long addComment(CommentDTO commentDTO);

    Long addReply(ReplyDTO replyDTO);

    Boolean kudosComment(String commentId,Byte bytes);

    Boolean kudosReply(String replyId,Byte bytes);

    BlogUserInfoDTO getBlogUserInfo(String userId);

    Boolean starBlogAuthor(String authorId);

    Boolean cancelStarBlogAuthor(String authorId);
}
