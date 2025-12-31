package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.dto.ReadCommentDTO;
import org.oyyj.blogservice.dto.ReadDTO;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;
import java.util.Map;


public interface IBlogService extends IService<Blog> {
    void saveBlog(Blog blog);

    ReadDTO ReadBlog(Long id, LoginUser loginUser);

    PageDTO<BlogDTO> getBlogByPage(int current, int pageSize, String type, LoginUser loginUser);

    List<ReadCommentDTO> getBlogComment(String blogId,String userInfoKey);

    Boolean changeCommentKudos(Long commentId,Byte bytes);

    Boolean changeReplyKudos(Long replyId,Byte bytes);

    List<Long> getUserBlogNum(Long userId);

    /**
     * 首页 博客
     * @param loginUser
     * @return
     */
    List<BlogDTO> getHomeBlogs(LoginUser loginUser);

    PageDTO<BlogDTO> getBlogByName(int current,int pageSize,String blogName);

    /**
     * 根据作者查询博客信息
     * @param current 当前页数
     * @param pageSize 查询量
     * @param userId 作者ID
     * @param typeList 博客类别
     * @param orderBy 排序依据的字段
     * @param isDesc 正序or倒序
     * @return
     */
    PageDTO<BlogDTO> getBlogByUserId(int current,int pageSize,Long userId, List<String> typeList,String orderBy,String isDesc);


    PageDTO<BlogDTO> getBlogByIds(int current, int pageSize, List<Long> blogs);

    List<BlogDTO> getHotBlogs();

    Map<String,Long> getIncreaseBlog();

    Map<String,Long> getAllTypeNum();

    String getBlogListByAdmin(String blogName,
                              String authorName,
                              Date startDate,
                              Date endDate,  // spring框架默认是不支持前端 date的iso类型  所以要设置
                              String status,
                              Integer currentPage) throws JsonProcessingException;

}
