package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.oyyj.blogservice.dto.*;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.blogservice.vo.admin.BlogTypeVO;
import org.oyyj.blogservice.vo.blogs.BlogSearchVO;
import org.oyyj.blogservice.vo.blogs.CommendBlogsByAuthor;
import org.oyyj.mycommon.pojo.dto.UserBlogInfoDTO;
import org.oyyj.mycommon.pojo.dto.blog.Blog12MonthDTO;
import org.oyyj.mycommon.pojo.dto.blog.ComRepForUserDTO;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public interface IBlogService extends IService<Blog> {
    boolean saveBlog(Blog blog);

    boolean saveBlog(BlogDTO blogDTO,LoginUser loginUser);

    /**
     * 获取简介
     * @param mdContent
     * @return
     */
    CompletableFuture<ResultUtil<String>> getSummary(String mdContent);

    ReadDTO ReadBlog(Long id, LoginUser loginUser);

    void readBlogValid(Long id , LoginUser loginUser) throws Exception;

    PageDTO<BlogDTO> getBlogByPage(int current, int pageSize, String type, LoginUser loginUser);

    void publishDelayBlogs();

    Map<Long,List<Long>> getUserBlogNum(List<Long> userId);

    /**
     * 首页 博客
     * @param loginUser
     * @return
     */
    List<BlogDTO> getHomeBlogs(LoginUser loginUser);

    /**
     * 定期轮询 检查预定了 但是没有发布的数据
     */
    void publishErrorBlogs();

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

    PageDTO<BlogDTO> getBlogListByAdmin(String blogName, String authorName, Date startDate, Date endDate, String status, Integer currentPage, Integer pageSize) throws JsonProcessingException;

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
    Map<String,Object> mergeFileChunk(String fileNo,Long totalFileChunks, String orgFileName,Long userId);

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

    /**
     * 用户在博客中上传文件
     * @param file
     * @param loginUser
     * @return
     */
    ResultUtil<String> uploadBlogImg(MultipartFile file, LoginUser loginUser);

    /**
     * 用户所有作品
     * @param userId
     * @return
     */
    ResultUtil<List<BlogDTO>> getBlogWork(Long userId,Integer currentPage, Integer pageSize);

    /**
     * 用户所有点赞
     * @param userId
     * @return
     */
    ResultUtil<List<BlogDTO>> getBlogLike(Long userId,Integer currentPage, Integer pageSize);

    /**
     * 用户所有点赞
     * @param userId
     * @return
     */
    ResultUtil<List<BlogDTO>> getBlogStar(Long userId,Integer currentPage, Integer pageSize);


    /**
     * 查询 某用户近12月的博客发表记录
     * @param userId
     * @return
     */
    Blog12MonthDTO getBlog12MonthByUserId(Long userId);

    /**
     * 查询 指定的 userIds中的博客数量
     * @param userIds
     * @return
     */
    Map<Long,Integer> countByUserList(List<Long> userIds);

    /**
     * 查询 指定 ids中的评论数量 攻击性评论占比
     * @param userIds
     * @return
     */
    List<ComRepForUserDTO> countCommentReplyByUserList(List<Long> userIds);

    /**
     * 查询管理端首页 博客 种类圆饼图 展示
     * @return
     */
    ResultUtil<List<BlogTypeVO>> blogTypeList();

    /**
     * 获取热条图片
     * @return
     */
    ResultUtil<List<BlogDTO>> blogListHotImage();

    /**
     * 获取热条项目
     * @return
     */
    ResultUtil<List<BlogDTO>> blogListHotProject();

}
