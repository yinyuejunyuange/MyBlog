package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.oyyj.blogservice.dto.*;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.blogservice.vo.blogs.BlogSearchVO;
import org.oyyj.blogservice.vo.blogs.CommendBlogsByAuthor;
import org.oyyj.mycommon.pojo.dto.UserBlogInfoDTO;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;
import java.util.Map;


public interface IBlogService extends IService<Blog> {
    boolean saveBlog(Blog blog);

    ReadDTO ReadBlog(Long id, LoginUser loginUser);

    void readBlogValid(Long id , LoginUser loginUser) throws Exception;

    PageDTO<BlogDTO> getBlogByPage(int current, int pageSize, String type, LoginUser loginUser);




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

    /**
     * 收藏博客--博客记录数+1
     * @param blogId
     * @return
     */
    boolean blogStar(Long blogId , LoginUser loginUser);

    /**
     * 取消收藏博客信息 -- 博客记录-1
     * @param blogId
     * @return
     */
    boolean cancelStar(Long blogId , LoginUser loginUser);

    /**
     * 点赞博客 -- 博客点赞数+1
     * @param blogId
     * @return
     */
    boolean blogKudos(Long blogId , LoginUser loginUser);

    /**
     * 取消点赞博客 -- 博客点赞数-1
     * @param blogId
     * @return
     */
    boolean cancelKudos(Long blogId , LoginUser loginUser);


    // todo 测试使用待会删除
    Map<String,Object> uploadFileChunk(FileUploadDTO fileUploadDTO);

    // todo 测试使用待会删除
    Map<String,Object> mergeFileChunk(String fileNo,Long totalFileChunks, String orgFileName);

    /**
     * 获取用户有关博客的信息
     * @param blogId
     * @return
     */
    UserBlogInfoDTO getUserBlogInfo(Long blogId);

    /**
     *
     * @param userId
     * @param currentBlogId
     * @return
     */
    CommendBlogsByAuthor commendBlogsByAuthor(Long userId, Long currentBlogId);

    /**
     * 添加评论 博客评论数+1
     * @param blogId
     * @return
     */
    void blogComment(Long blogId , LoginUser loginUser);

    /**
     * 通过关键获取博客信息
     * @param keyWord
     * @param currentPage
     * @param pageSize
     * @return
     */
    ResultUtil<List<BlogSearchVO>> getBlogByKeyWord(String keyWord, Integer currentPage, Integer pageSize);
}
