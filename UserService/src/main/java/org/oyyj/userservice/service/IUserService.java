package org.oyyj.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.userservice.dto.*;
import org.oyyj.userservice.dto.user.vo.UserInfoVO;
import org.oyyj.userservice.pojo.JWTUser;
import org.oyyj.userservice.pojo.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public interface IUserService extends IService<User> {

    ResultUtil<UserInfoVO> userInfoById(Long id , LoginUser loginUser);

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



    /**
     * 获取用户头像信息
     * @param objectName minio对象名称
     * @return
     */
    void getImageUrl(String objectName,HttpServletResponse response);

    /**
     * 获取用户点赞的博客
     * @param userId
     * @return
     */
    List<Long> getUserLikeBlog(Long userId ,Integer currentPage ,Integer pageSize);

    /**
     * 获取用户的收藏博客
     * @param userId
     * @return
     */
    List<Long> getUserStarBlog(Long userId ,Integer currentPage ,Integer pageSize);

    ResultUtil<org.oyyj.userservice.vo.UserInfoVO> getUserInfo(Long userId,LoginUser loginUser);

}
