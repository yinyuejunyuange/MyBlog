package org.oyyj.userservice.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.oyyj.mycommon.pojo.dto.UserBlogInfoDTO;
import org.oyyj.mycommon.utils.FileUtil;
import org.oyyj.mycommon.utils.TransUtil;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.userservice.dto.*;
import org.oyyj.userservice.Feign.AIChatFeign;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.dto.user.vo.UserInfoVO;
import org.oyyj.userservice.mapper.SysRoleMapper;
import org.oyyj.userservice.mapper.UserMapper;
import org.oyyj.userservice.pojo.*;
import org.oyyj.userservice.service.*;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private Integer PAGE_SIZE=6;

    @Autowired
    private BlogFeign blogFeign;

    @Autowired
    private IUserKudosService userKudosService;

    @Autowired
    private IUserStarService userStarService;

    @Autowired
    private IUserCommentService userCommentService;

    @Autowired
    private IUserReplyService userReplyService;

    @Autowired
    private IUserAttentionService userAttentionService;

    @Autowired
    private AIChatFeign aiChatFeign;

    @Autowired
    private ISearchService searchService;

    @Autowired
    private FileUtil fileUtils;

    @Override
    public ResultUtil<UserInfoVO> userInfoById(Long id , LoginUser loginUser) {
        UserInfoVO userInfoVO = new UserInfoVO();
        User one = getOne(Wrappers.<User>lambdaQuery().eq(User::getId, id));
        if(one == null){
            log.error("用户信息缺失 id为：{}",id);
            return ResultUtil.fail("获取用户信息失败");
        }
        userInfoVO.setUserName(one.getNickName());
        userInfoVO.setHead(one.getImageUrl());
        userInfoVO.setIntroduce(Strings.isNotBlank(one.getIntroduce())? one.getIntroduce():"这个人很神秘 什么都没留下...");
        UserBlogInfoDTO userBlogInfo = blogFeign.getUserBlogInfo(one.getId());
        userInfoVO.setLikes(userBlogInfo.getLikes());
        userInfoVO.setBlogs(userBlogInfo.getBlogs());
        userInfoVO.setStar(userBlogInfo.getStar());
        // 获取关注作者的关注信息
        Integer funS = baseMapper.userFunS(id);
        userInfoVO.setFunS(TransUtil.formatNumber(funS));

        if(Objects.equals(YesOrNoEnum.YES.getCode(), loginUser.getIsUserLogin())){
            // 判断当前用户是否关注此作者
            UserAttention userAttention = userAttentionService.getOne(Wrappers.<UserAttention>lambdaQuery()
                    .eq(UserAttention::getUserId, loginUser.getUserId())
                    .eq(UserAttention::getAttentionId, id)
            );

            if(Objects.isNull(userAttention)){
                userInfoVO.setIsUserStar(YesOrNoEnum.NO.getCode());
            }else{
                userInfoVO.setIsUserStar(YesOrNoEnum.YES.getCode());
            }

        }else{
            userInfoVO.setIsUserStar(YesOrNoEnum.NO.getCode());
        }

        return ResultUtil.success(userInfoVO);
    }

    @Override
    public boolean userKudos(String blogId,LoginUser loginUser) {

        boolean save = userKudosService.save(UserKudos.builder()
                .userId(loginUser.getUserId())
                .blogId(Long.valueOf(blogId))
                .build());
        if(save){
            return save;
        }else{
            return false;
        }
    }

    @Override
    public boolean userStar(String blogId,Long  userId) {

        return userStarService.save(UserStar.builder()
                .blogId(Long.valueOf(blogId))
                .userId(userId)
                .build());
    }

    @Override
    @Transactional // 原子性 保证回滚
    public Boolean kudosComment(String commentId,Byte bytes,Long userId) {


        if(bytes==1){
            UserComment userComment=new UserComment();
            userComment.setCommentId(Long.valueOf(commentId));
            userComment.setUserId(userId);
            boolean save = userCommentService.save(userComment);
            if(save){
                // 评论点赞数加一
                Boolean b = blogFeign.changCommentKudos(Long.valueOf(commentId), bytes);
                if(b){
                    return b;
                }else{
                    // 评论点赞数添加失败
                    throw new RuntimeException("博客评论增加异常");
                }
            }else{
                return false;
            }
        }else if(bytes==2){


            boolean remove = userCommentService.remove(Wrappers.<UserComment>lambdaQuery()
                    .eq(UserComment::getCommentId, Long.valueOf(commentId))
                    .eq(UserComment::getUserId, userId)
            );

            if(remove){
                // 评论点赞数加一
                Boolean b = blogFeign.changCommentKudos(Long.valueOf(commentId), bytes);
                if(b){
                    return b;
                }else{
                    // 评论点赞数添加失败
                    throw new RuntimeException("博客评论减少异常");
                }
            }else{
                return false;
            }
        }else{
            return false;
        }

    }

    @Transactional
    public Boolean kudosReply(String replyId,Byte bytes,Long userId) {
        if(bytes==1){
            UserReply build = UserReply.builder()
                    .userId(userId)
                    .replyId(Long.valueOf(replyId))
                    .build();
            boolean save = userReplyService.save(build);
            if(save){
                Boolean b = blogFeign.changReplyKudos(Long.valueOf(replyId), bytes);
                if(b){
                    return b;
                }else{
                    // 抛出异常 让事务回滚
                    throw new RuntimeException("博客评论点赞数增加异常");

                }
            }else{
                return false;
            }
        } else if (bytes==2) {
            boolean remove = userReplyService.remove(Wrappers.<UserReply>lambdaQuery()
                    .eq(UserReply::getReplyId, Long.valueOf(replyId))
                    .eq(UserReply::getUserId, userId)
            );

            if(remove){
                Boolean b = blogFeign.changReplyKudos(Long.valueOf(replyId), bytes);
                if(b){
                    return b;
                }else{

                    throw new RuntimeException("博客评论点赞数减少异常");
                }
            }else{
                return false;
            }
        }else{

            return false;
        }
    }

    @Override
    //  获取博客作者信息（博客粉丝数 博客创建时间 博客简介 博客原作数  博客访问量）
    public BlogUserInfoDTO getBlogUserInfo(String userId, LoginUser principal){

        Long id = Long.valueOf(userId);
        // 获取用户粉丝数 简介 注册时间
        User one = getOne(Wrappers.<User>lambdaQuery().eq(User::getId, id));
        if(Objects.isNull(one)){
            return null;
        }
        // 获取用户 博客数 博客总访问量 博客总点赞量
        List<Long> blogUserInfo = blogFeign.getBlogUserInfo(id);
        BlogUserInfoDTO blogUserInfoDTO = new BlogUserInfoDTO();
        if(!blogUserInfo.isEmpty()){
            blogUserInfoDTO.setBlogNum(blogUserInfo.getFirst());
            blogUserInfoDTO.setVisitedNum(blogUserInfo.get(1));
            blogUserInfoDTO.setKudosNum(blogUserInfo.get(2));
            blogUserInfoDTO.setUserName(one.getName());
            blogUserInfoDTO.setImageUrl("http://localhost:8080/myBlog/user/getHead/"+one.getImageUrl());
            blogUserInfoDTO.setUserId(userId);
            blogUserInfoDTO.setStarNum(one.getStar());
            blogUserInfoDTO.setCreateTime(one.getCreateTime());
            blogUserInfoDTO.setIntroduction(one.getIntroduce());
        }else{
            blogUserInfoDTO.setUserName(one.getName());
            blogUserInfoDTO.setImageUrl(one.getImageUrl());
            blogUserInfoDTO.setUserId(userId);
            blogUserInfoDTO.setStarNum(one.getStar());
            blogUserInfoDTO.setCreateTime(one.getCreateTime());
            blogUserInfoDTO.setIntroduction(one.getIntroduce());
        }

        if(principal.getIsUserLogin() == 1){

            // 判断当前用户是否关注此作者
            UserAttention userAttention = userAttentionService.getOne(Wrappers.<UserAttention>lambdaQuery()
                    .eq(UserAttention::getUserId, principal.getUserId())
                    .eq(UserAttention::getAttentionId, Long.valueOf(userId))
            );

            if(Objects.isNull(userAttention)){
                blogUserInfoDTO.setIsUserStar(false);
            }else{
                blogUserInfoDTO.setIsUserStar(true);
            }

        }else{
            blogUserInfoDTO.setIsUserStar(false);
        }
        return blogUserInfoDTO;
    }

    @Override
    @Transactional
    public Boolean starBlogAuthor(String authorId,LoginUser user) {
        Long userId = user.getUserId();
        Long attentionId = Long.valueOf(authorId);
        boolean save = userAttentionService.save(UserAttention.builder()
                .userId(userId)
                .attentionId(attentionId)
                .build());
        if(save){
            User one = getOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getId, attentionId)
                    .last("for update") // 悲观锁
            );
            if(Objects.isNull(one)){
                return false;
            }
            return update(Wrappers.<User>lambdaUpdate()
                    .eq(User::getId, attentionId)
                    .set(User::getStar, one.getStar() + 1)
            );
        }else{
            return false;
        }

    }

    @Override
    @Transactional
    public Boolean cancelStarBlogAuthor(String authorId,LoginUser user) {
        Long userId = user.getUserId();
        Long attentionId = Long.valueOf(authorId);
        UserAttention userAttention = userAttentionService.getOne(Wrappers.<UserAttention>lambdaQuery()
                .eq(UserAttention::getUserId, userId)
                .eq(UserAttention::getAttentionId, attentionId)
        );
        if(Objects.isNull(userAttention)){
            log.error("操作错误");
            return false;
        }
        boolean remove = userAttentionService.remove(Wrappers.<UserAttention>lambdaQuery().eq(UserAttention::getUserId, userId)
                .eq(UserAttention::getAttentionId, attentionId));
        if(remove){
            User one = getOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getId, attentionId)
                    .last("for update") // 悲观锁
            );

            if(Objects.isNull(one)){
                return false;
            }

            return update(Wrappers.<User>lambdaUpdate()
                    .eq(User::getId, attentionId)
                    .set(User::getStar, one.getStar() - 1)
            );
        }
        return false;
    }

    @Override
    public Map<String, Object> getUserStarBlog(String userId, int current) {

        List<UserStar> userStars = userStarService.list(Wrappers.<UserStar>lambdaQuery().eq(UserStar::getUserId, Long.valueOf(userId)));

        if(Objects.isNull(userStars)){
            return ResultUtil.failMap("用户没有收藏的文章");
        }
        List<Long> blogs = userStars.stream().map(UserStar::getBlogId).toList();
        //
        return blogFeign.getUserStarBlog(blogs, current);


    }

    @Override
    public PageDTO<BlogUserInfoDTO> getUserStarBlogAuthor(String userId, int current) {

        IPage<UserAttention> userAttentionIPage=new Page<>(current,PAGE_SIZE);

        // 查询用户关注的博客作者
        List<UserAttention> userAttentions = userAttentionService.list(userAttentionIPage, Wrappers.<UserAttention>lambdaQuery()
                .eq(UserAttention::getUserId, Long.valueOf(userId))
        );
        if(Objects.isNull(userAttentions)||userAttentions.isEmpty()){
            log.error("用户没有关注对象");
            return null;
        }

        List<Long> authorIds = userAttentions.stream().map(UserAttention::getAttentionId).toList();
        List<BlogUserInfoDTO> list = list(Wrappers.<User>lambdaQuery().in(User::getId, authorIds)).stream().map(i -> BlogUserInfoDTO.builder()
                .userId(String.valueOf(i.getId()))
                .userName(i.getName())
                .imageUrl("http://localhost:8080/myBlog/user/getHead/" + i.getImageUrl())
                .createTime(i.getCreateTime())
                .introduction(i.getIntroduce())
                .blogNum(blogFeign.getBlogUserInfo(i.getId()).getFirst())
                .visitedNum(blogFeign.getBlogUserInfo(i.getId()).get(1))
                .starNum(i.getStar())
                .kudosNum(blogFeign.getBlogUserInfo(i.getId()).get(2))
                .isUserStar(true)
                .build()).toList();

        PageDTO<BlogUserInfoDTO> blogUserInfoDTOPageDTO=new PageDTO<>();
        blogUserInfoDTOPageDTO.setPageList(list);
        blogUserInfoDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(userAttentionIPage.getTotal())));
        blogUserInfoDTOPageDTO.setPageNow(current);
        blogUserInfoDTOPageDTO.setPageSize(PAGE_SIZE);

        return blogUserInfoDTOPageDTO;
    }
    @Override
    public Map<String,Object> getUsersBlog(Long userId,int current){
        return blogFeign.GetBlogByUserId(userId, current);
    }

    @Override
    @Transactional
    public Map<String, Object> changeUserInfo(ChangeUserDTO changeUserDTO,LoginUser loginUser) {
        User one = getOne(Wrappers.<User>lambdaQuery()
                .eq(User::getId, loginUser.getUserId())
                .last("for update") // 加锁悲观锁
        );
        if(!Objects.isNull(changeUserDTO.getUserName())&&!changeUserDTO.getUserName().isEmpty()){
            one.setName(changeUserDTO.getUserName());
        }
        if(!Objects.isNull(changeUserDTO.getEmail())&&!changeUserDTO.getEmail().isEmpty()){
            one.setEmail(changeUserDTO.getEmail());
        }
        if(!Objects.isNull(changeUserDTO.getSex())){
            one.setSex(changeUserDTO.getSex());
        }
        if(!Objects.isNull(changeUserDTO.getIntroduce())&&!changeUserDTO.getIntroduce().isEmpty()){
            one.setIntroduce(changeUserDTO.getIntroduce());
        }

        boolean b = saveOrUpdate(one);
        return ResultUtil.successMap(b,"修改成功");
    }

    @Async("asyncTaskExecutor")
    @Override
    public void upLoadBlogToAI(BlogDTO blogDTO) {

        // 将用户生成的文档转换成txt格式的文件存储下来并上传到ai中
        String filePath="H:/MyBlogFiles/";

        String fileName=blogDTO.getTitle().replaceAll(" ","_")+".txt";
        File file=new File(filePath,fileName); // 生成一个本地文件

        // 向文件中写数据
        try {
            FileWriter writer=new FileWriter(file);

            writer.write("博客标题\n"+blogDTO.getTitle()+"\n\n");
            writer.write("博客作者\n"+blogDTO.getUserName()+"\n\n");
            writer.write("博客简介\n"+blogDTO.getIntroduce()+"\n\n");
            writer.write("博客内容\n"+blogDTO.getText()+"\n\n");

            writer.flush(); // 让所有缓存全部存储到文件中

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 将File转换成Multipartfile
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            MockMultipartFile multiPartFile = new MockMultipartFile("file", bytes);

            AIFileDTO build = AIFileDTO.builder()
                    .fileAddress(filePath + fileName)
                    .isUpload(0)
                    .isDelete(0)
                    .build();

            ObjectMapper objectMapper = new ObjectMapper();
            String s = objectMapper.writeValueAsString(build);

            // 调用接口 上传文件到工作区
            aiChatFeign.uploadFileToWorkShape(multiPartFile,"myllm",s);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Map<String, Object> getHotSearch() {
        List<HotSearchDTO> hotSearch = searchService.getHotSearch();
        List<String> list = hotSearch.stream().map(HotSearchDTO::getContent).toList();
        return ResultUtil.successMap(list,"查询成功");
    }

    @Override
    public List<String> getUserSearch(LoginUser loginUser) {
        return searchService.list(Wrappers.<Search>lambdaQuery()
                .eq(Search::getUserId, loginUser.getUserId())
                .orderByDesc(Search::getLatelyTime) // 按照最近时间排序
        ).stream().map(Search::getContent).toList();
    }

    @Override
    @Transactional
    public boolean addUserSearch(List<String> names, LoginUser loginUser) {

        Date date=new Date();

        // 获取用户搜索记录
        List<String> list = searchService.list(Wrappers.<Search>lambdaQuery().eq(Search::getUserId, loginUser.getUserId())).stream().map(Search::getContent).toList();


        List<Search> searches=new ArrayList<>();
        for (String name : names) {
            name=name.trim();
            if(name.isEmpty()||list.contains(name)){
                continue; // 为空的记录可以跳过
            }
            Search build = Search.builder()
                    .userId(loginUser.getUserId())
                    .content(name)
                    .createTime(date)
                    .updateTime(date)
                    .latelyTime(date)
                    .isUserDelete(0)
                    .isDelete(0)
                    .build();
            searches.add(build);
        }

        return searchService.saveBatch(searches);
    }

    @Override
    public boolean deleteUserSearchByName(String name ,LoginUser loginUser) {
        if(name.trim().isEmpty()){
            log.error("参数为空");
            return false;
        }
        return searchService.update(Wrappers.<Search>lambdaUpdate()
                .eq(Search::getUserId, loginUser.getUserId())
                .eq(Search::getContent, name)
                .set(Search::getIsUserDelete, 1)
        );
    }

    @Override
    public boolean deleteUserAllSearch(LoginUser loginUser) {
       return searchService.update(Wrappers.<Search>lambdaUpdate()
                .eq(Search::getUserId, loginUser.getUserId())
                .set(Search::getIsUserDelete, 1)
        );
    }

    @Override
    public void getImageUrl(String objectName,HttpServletResponse response) {
        fileUtils.getHeadImgUrl(objectName,response);
    }
}
