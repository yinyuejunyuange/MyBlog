package org.oyyj.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.userservice.dto.*;
import org.oyyj.userservice.pojo.JWTUser;
import org.oyyj.userservice.pojo.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public interface IUserService extends IService<User> {

    boolean userKudos(String blogId, LoginUser loginUser) throws Exception;

    boolean userStar(String blogId, Long userId) throws Exception;

    Boolean kudosComment(String commentId,Byte bytes,Long userId);

    Boolean kudosReply(String replyId,Byte bytes,Long userId);

    BlogUserInfoDTO getBlogUserInfo(String userId, LoginUser principal);

    Boolean starBlogAuthor(String authorId,LoginUser user) throws Exception;

    Boolean cancelStarBlogAuthor(String authorId,LoginUser user);

    Map<String,Object> getUserStarBlog(String userId,int current);

    PageDTO<BlogUserInfoDTO> getUserStarBlogAuthor(String userId, int current);

    Map<String,Object> getUsersBlog(Long userId,int current);

    Map<String,Object> changeUserInfo(ChangeUserDTO changeUserDTO,LoginUser loginUser);

    void upLoadBlogToAI(BlogDTO blogDTO);

    Map<String,Object> getHotSearch();

    List<String> getUserSearch(LoginUser loginUser);

    boolean addUserSearch(List<String> names, LoginUser loginUser);

    boolean deleteUserSearchByName(String name ,LoginUser loginUser);


    boolean deleteUserAllSearch(LoginUser loginUser);

}
