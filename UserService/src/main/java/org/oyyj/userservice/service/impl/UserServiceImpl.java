package org.oyyj.userservice.service.impl;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.oyyj.mycommon.pojo.dto.blog.Blog12MonthDTO;
import org.oyyj.mycommon.pojo.dto.blog.ComRepForUserDTO;
import org.oyyj.mycommon.pojo.vo.UserComRepVO;
import org.oyyj.mycommon.utils.FileUtil;
import org.oyyj.mycommon.utils.TransUtil;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.userservice.Feign.ChatFeign;
import org.oyyj.userservice.Feign.StudyFeign;
import org.oyyj.userservice.dto.*;
import org.oyyj.userservice.Feign.AIChatFeign;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.dto.user.vo.UserInfoVO;
import org.oyyj.userservice.mapper.SysRoleMapper;
import org.oyyj.userservice.mapper.UserMapper;
import org.oyyj.userservice.pojo.*;
import org.oyyj.userservice.service.*;
import org.oyyj.userservice.vo.DashboardTitleVO;
import org.oyyj.userservice.vo.UserInfoForAdminVO;
import org.oyyj.userservice.vo.user.User12MonthVO;
import org.oyyj.userservice.vo.user.UserDetailDTO;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private Integer PAGE_SIZE=10;

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

//    @Autowired
//    private AIChatFeign aiChatFeign;

    @Autowired
    private ChatFeign chatFeign;


    @Autowired
    private FileUtil fileUtils;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private StudyFeign studyFeign;
    @Autowired
    private UserMapper userMapper;

    @Override
    public ResultUtil<UserInfoVO> userInfoById(Long id , LoginUser loginUser) {
        UserInfoVO userInfoVO = new UserInfoVO();
        User one = getOne(Wrappers.<User>lambdaQuery().eq(User::getId, id));
        if(one == null){
            log.error("用户信息缺失 id为：{}",id);
            return ResultUtil.fail("获取用户信息失败");
        }
        userInfoVO.setUserName(one.getNickName());
        userInfoVO.setUserId(String.valueOf(one.getId()));
        userInfoVO.setHead(one.getImageUrl());
        userInfoVO.setIntroduce(Strings.isNotBlank(one.getIntroduce())? one.getIntroduce():"这个人很神秘 什么都没留下...");
        UserBlogInfoDTO userBlogInfo = blogFeign.getUserBlogInfo(one.getId());
        userInfoVO.setLikes(String.valueOf(userBlogInfo.getLikes()));
        userInfoVO.setBlogs(String.valueOf(userBlogInfo.getBlogs()));
        userInfoVO.setStar(String.valueOf(userBlogInfo.getStar()));
        // 获取关注作者的关注信息
        Integer funS = baseMapper.userFunS(id);
        userInfoVO.setFunS(TransUtil.formatNumber(funS));

        if(Objects.equals(YesOrNoEnum.YES.getCode(), loginUser.getIsUserLogin())){

            if(id.equals(loginUser.getUserId())){
                userInfoVO.setIsUserSelf(true);
                userInfoVO.setIsUserStar(YesOrNoEnum.NO.getCode());
            }else{
                userInfoVO.setIsUserSelf(false);
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
            }

        }else{
            userInfoVO.setIsUserStar(YesOrNoEnum.NO.getCode());
            userInfoVO.setIsUserSelf(false);
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
        Map<Long,List<Long>> blogUserInfo = blogFeign.getBlogUserInfo(Collections.singletonList(id));
        BlogUserInfoDTO blogUserInfoDTO = new BlogUserInfoDTO();
        if(!blogUserInfo.isEmpty()){
            blogUserInfoDTO.setBlogNum( blogUserInfo.containsKey(Long.parseLong(userId))? blogUserInfo.get(Long.parseLong(userId)).getFirst():0);
            blogUserInfoDTO.setVisitedNum(blogUserInfo.containsKey(Long.parseLong(userId))? blogUserInfo.get(Long.parseLong(userId)).get(1):0);
            blogUserInfoDTO.setKudosNum(blogUserInfo.containsKey(Long.parseLong(userId))? blogUserInfo.get(Long.parseLong(userId)).get(2):0);
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
            // 提交关注信息
            chatFeign.addFansInfo(String.valueOf(userId),authorId);
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
            chatFeign.unfollow(String.valueOf(userId),authorId);
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
    public PageDTO<BlogUserInfoDTO> getUserStarBlogAuthor(String userId, String title, int current) {

        IPage<UserAttention> userAttentionIPage=new Page<>(current,PAGE_SIZE);

        List<User> idList = list(Wrappers.<User>lambdaQuery()
                .like(User::getName, title)
                .select(User::getId)
        );

        if(idList.isEmpty()){
            PageDTO<BlogUserInfoDTO> blogUserInfoDTOPageDTO=new PageDTO<>();
            blogUserInfoDTOPageDTO.setPageList(List.of());
            blogUserInfoDTOPageDTO.setTotal(0);
            blogUserInfoDTOPageDTO.setPageNow(current);
            blogUserInfoDTOPageDTO.setPageSize(PAGE_SIZE);
        }
        // 查询用户关注的博客作者
        List<UserAttention> userAttentions = userAttentionService.list(userAttentionIPage, Wrappers.<UserAttention>lambdaQuery()
                .eq(UserAttention::getUserId, Long.valueOf(userId))
        );
        if(Objects.isNull(userAttentions)||userAttentions.isEmpty()){
            log.error("用户没有关注对象");
            return null;
        }

        List<Long> authorIds = userAttentions.stream().map(UserAttention::getAttentionId).toList();

        List<User> userList = list(Wrappers.<User>lambdaQuery().in(User::getId, authorIds));
        List<Long> userIds =userList.stream().map(User::getId).toList();
        Map<Long,List<Long>> collect =   blogFeign.getBlogUserInfo(userIds);

        List<BlogUserInfoDTO> list =userList.stream().map(i -> BlogUserInfoDTO.builder()
                .userId(String.valueOf(i.getId()))
                .userName(i.getName())
                .imageUrl( i.getImageUrl())
                .createTime(i.getCreateTime())
                .introduction(i.getIntroduce())
                .blogNum( collect.containsKey(i.getId())? collect.get(i.getId()).getFirst(): 0)
                .visitedNum(collect.containsKey(i.getId())? collect.get(i.getId()).get(1): 0)
                .starNum(i.getStar())
                .kudosNum(collect.containsKey(i.getId())? collect.get(i.getId()).get(2): 0)
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
//
//    @Async("asyncTaskExecutor")
//    @Override
//    public void upLoadBlogToAI(BlogDTO blogDTO) {
//
//        // 将用户生成的文档转换成txt格式的文件存储下来并上传到ai中
//        String filePath="H:/MyBlogFiles/";
//
//        String fileName=blogDTO.getTitle().replaceAll(" ","_")+".txt";
//        File file=new File(filePath,fileName); // 生成一个本地文件
//
//        // 向文件中写数据
//        try {
//            FileWriter writer=new FileWriter(file);
//
//            writer.write("博客标题\n"+blogDTO.getTitle()+"\n\n");
//            writer.write("博客作者\n"+blogDTO.getUserName()+"\n\n");
//            writer.write("博客简介\n"+blogDTO.getIntroduce()+"\n\n");
//            writer.write("博客内容\n"+blogDTO.getText()+"\n\n");
//
//            writer.flush(); // 让所有缓存全部存储到文件中
//
//            writer.close();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        // 将File转换成Multipartfile
//        try {
//            byte[] bytes = Files.readAllBytes(file.toPath());
//            MockMultipartFile multiPartFile = new MockMultipartFile("file", bytes);
//
//            AIFileDTO build = AIFileDTO.builder()
//                    .fileAddress(filePath + fileName)
//                    .isUpload(0)
//                    .isDelete(0)
//                    .build();
//
//            ObjectMapper objectMapper = new ObjectMapper();
//            String s = objectMapper.writeValueAsString(build);
//
//            // 调用接口 上传文件到工作区
//            aiChatFeign.uploadFileToWorkShape(multiPartFile,"myllm",s);
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//    }


    @Override
    public void getImageUrl(String objectName,HttpServletResponse response) {
        fileUtils.getHeadImgUrl(objectName,response);
    }

    @Override
    public List<Long> getUserLikeBlog(Long userId ,Integer currentPage ,Integer pageSize) {
        Page<UserKudos> page = new Page<>(currentPage,pageSize);
        return userKudosService.list(page,Wrappers.<UserKudos>lambdaQuery()
                .eq(UserKudos::getUserId,userId)
        ).stream().map(UserKudos::getBlogId).toList();
    }

    @Override
    public List<Long> getUserStarBlog(Long userId ,Integer currentPage ,Integer pageSize) {
        Page<UserStar> page = new Page<>(currentPage,pageSize);
        return userStarService.list(page, Wrappers.<UserStar>lambdaQuery()
                .eq(UserStar::getUserId,userId)
        ).stream().map(UserStar::getBlogId).toList();
    }

    @Override
    public ResultUtil<org.oyyj.userservice.vo.UserInfoVO> getUserInfo(Long userId, LoginUser loginUser) {
        org.oyyj.userservice.vo.UserInfoVO userInfoVO = new org.oyyj.userservice.vo.UserInfoVO();
        User one = getOne(Wrappers.<User>lambdaQuery().eq(User::getId, userId));
        if(one == null){
            log.error("用户信息缺失 id为：{}",userId);
            return ResultUtil.fail("获取用户信息失败");
        }
        userInfoVO.setUserName(one.getName());
        userInfoVO.setImageHead(one.getImageUrl());
        userInfoVO.setIntroduction(Strings.isNotBlank(one.getIntroduce())? one.getIntroduce():"这个人很神秘 什么都没留下...");
        UserBlogInfoDTO userBlogInfo = blogFeign.getUserBlogInfo(one.getId());
        userInfoVO.setKudus(userBlogInfo.getLikes());
        userInfoVO.setBlogs(userBlogInfo.getBlogs());
        userInfoVO.setStar(userBlogInfo.getStar());
        userInfoVO.setView(userBlogInfo.getView());
        // 获取关注作者的关注信息
        Integer funS = baseMapper.userFunS(userId);
        userInfoVO.setBeAttention(funS);
        Integer att = baseMapper.userAttention(userId);
        userInfoVO.setAttention(att);
        userInfoVO.setUserId(String.valueOf(userId));

        if(Objects.equals(YesOrNoEnum.YES.getCode(), loginUser.getIsUserLogin())){
            if(loginUser.getUserId().equals(userId)){
                userInfoVO.setIsUserFollow(false);
                userInfoVO.setIsUserSelf(true);
            }else{
                userInfoVO.setIsUserSelf(false);
                // 判断当前用户是否关注此作者
                UserAttention userAttention = userAttentionService.getOne(Wrappers.<UserAttention>lambdaQuery()
                        .eq(UserAttention::getUserId, loginUser.getUserId())
                        .eq(UserAttention::getAttentionId, userId)
                );
                if(Objects.isNull(userAttention)){
                    userInfoVO.setIsUserFollow(false);
                }else{
                    userInfoVO.setIsUserFollow(true);
                }
            }
        }else{
            userInfoVO.setIsUserFollow(false);
        }
        return ResultUtil.success(userInfoVO);
    }

    @Override
    public List<String> getUserRoleInfo(Long userId) {
        return sysRoleMapper.roleNameByUserId(userId);
    }

    /**
     * 分页查询用户信息
     * @param userName
     * @param startTime
     * @param endTime
     * @param isUserFreeze
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public Page<UserInfoForAdminVO> getUserInfoForAdmin(String userName, Date startTime, Date endTime, Integer isUserFreeze, Integer pageNum, Integer pageSize) {

        LambdaQueryWrapper<User> lqw = Wrappers.<User>lambdaQuery();

        if(StringUtils.isNotEmpty(userName)){
            lqw.like(User::getName,userName);
        }

        if(Objects.nonNull(startTime)){
            lqw.ge(User::getCreateTime,startTime);
        }

        if(Objects.nonNull(endTime)){
            lqw.le(User::getCreateTime,endTime);
        }

        if(Objects.nonNull(isUserFreeze)){
            lqw.eq(User::getIsFreeze,isUserFreeze);
        }

        if(pageNum == null || pageNum <= 0){
            pageNum = 1;
        }
        if(pageSize == null || pageSize <= 0){
            pageSize = 10;
        }

        Page<User> page = new Page<>(pageNum,pageSize);
        List<User> list = list(page, lqw);
        List<Long> userIds = list.stream().map(User::getId).toList();

        List<ComRepForUserDTO> comRepForUserDTOS = blogFeign.countCommentReplyByUserList(userIds);
        Map<Long, ComRepForUserDTO> comRepMap = comRepForUserDTOS.stream().collect(Collectors.toMap(ComRepForUserDTO::getUserId, Function.identity()));
        Map<Long, Integer> longIntegerMap = blogFeign.countByUserList(userIds);

        List<UserInfoForAdminVO> result = list.stream().map(item -> {
            UserInfoForAdminVO userInfoForAdminVO = UserInfoForAdminVO.fromUser(item);
            if (comRepMap.containsKey(item.getId())) {
                ComRepForUserDTO comRepForUserDTO = comRepMap.get(item.getId());
                userInfoForAdminVO.setComRepCount(comRepForUserDTO.getCommentCount() + comRepForUserDTO.getReplyCount());
                userInfoForAdminVO.setToxicCount(comRepForUserDTO.getToxicCount());
                userInfoForAdminVO.setToxicRate(comRepForUserDTO.getToxicRate());
            }
            userInfoForAdminVO.setBlogCount(longIntegerMap.get(item.getId()));
            return userInfoForAdminVO;
        }).toList();

        Page<UserInfoForAdminVO> resultPage = new Page<>(pageNum,pageSize);
        resultPage.setTotal(page.getTotal());
        resultPage.setRecords(result);
        return resultPage;
    }

    @Override
    public Page<UserInfoForAdminVO> getAdminPage(String userName, Date startTime, Date endTime, Integer isUserFreeze, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<User> lqw = Wrappers.<User>lambdaQuery();

        // 查询所有是Admin且非 SupperAdmin的用户

        List<Long> longs = sysRoleMapper.selectAllAdmin();
        if(longs == null || longs.isEmpty()){
            return new  Page<>(pageNum,pageSize);
        }else{
            lqw.in(User::getId,longs);
        }

        if(StringUtils.isNotEmpty(userName)){
            lqw.like(User::getName,userName);
        }

        if(Objects.nonNull(startTime)){
            lqw.ge(User::getCreateTime,startTime);
        }

        if(Objects.nonNull(endTime)){
            lqw.le(User::getCreateTime,endTime);
        }

        if(Objects.nonNull(isUserFreeze)){
            lqw.eq(User::getIsFreeze,isUserFreeze);
        }

        if(pageNum == null || pageNum <= 0){
            pageNum = 1;
        }
        if(pageSize == null || pageSize <= 0){
            pageSize = 10;
        }
        Page<User> page = new Page<>(pageNum,pageSize);

        page = page(page, lqw);

        List<User> records = page.getRecords();
        List<UserInfoForAdminVO> collect = records.stream().map(UserInfoForAdminVO::fromUser).collect(Collectors.toList());

        Page<UserInfoForAdminVO> resultPage = new Page<>(pageNum,pageSize);
        resultPage.setTotal(page.getTotal());
        resultPage.setRecords(collect);
        return resultPage;
    }

    @Override
    public UserDetailDTO getUserDetail(String userId) {
        User byId = getById(Long.parseLong(userId));
        UserDetailDTO userDetailDTO = UserDetailDTO.fromUser(byId);
        Blog12MonthDTO blog12MonthByUserId = blogFeign.getBlog12MonthByUserId(Long.parseLong(userId));
        userDetailDTO.setBlog12MonthDTO(blog12MonthByUserId);
        List<UserComRepVO> userComRepVOS = blogFeign.toxicComRepResult(Long.parseLong(userId));
        userDetailDTO.setUserComRepVOList(userComRepVOS);
        Map<Long, Integer> blogMap= blogFeign.countByUserList(Collections.singletonList(Long.parseLong(userId)));
        if(blogMap!= null && !blogMap.isEmpty() && blogMap.containsKey(Long.parseLong(userId))){
            userDetailDTO.setBlogCount(blogMap.get(Long.parseLong(userId)));
        }
        List<ComRepForUserDTO> comRepForUserDTOS = blogFeign.countCommentReplyByUserList(Collections.singletonList(Long.parseLong(userId)));
        Map<Long, ComRepForUserDTO> comRepMap = comRepForUserDTOS.stream().collect(Collectors.toMap(ComRepForUserDTO::getUserId, Function.identity()));

        if (comRepMap.containsKey(Long.parseLong(userId))) {
            ComRepForUserDTO comRepForUserDTO = comRepMap.get(Long.parseLong(userId));
            userDetailDTO.setComRepCount(comRepForUserDTO.getCommentCount() + comRepForUserDTO.getReplyCount());
            userDetailDTO.setToxicCount(comRepForUserDTO.getToxicCount());
            userDetailDTO.setToxicRate(comRepForUserDTO.getToxicRate());
        }

        return userDetailDTO;
    }

    /**
     * 获取仪表盘的title信息
     * @return
     */
    @Override
    public ResultUtil<DashboardTitleVO> getDashboardTitle() {
        CompletableFuture<Long> totalBlogFuture = CompletableFuture.supplyAsync(() -> blogFeign.totalBlogs());

        CompletableFuture<Long> totalComRepFuture = CompletableFuture.supplyAsync(() -> blogFeign.totalComReps());

        CompletableFuture<Long> totalPointFuture = CompletableFuture.supplyAsync(() -> studyFeign.totalCount());

        CompletableFuture<Long> totalUserFuture = CompletableFuture.supplyAsync(this::count);

        CompletableFuture.allOf(totalBlogFuture,totalComRepFuture,totalPointFuture,totalUserFuture).join();

        DashboardTitleVO dashboardTitleVO = new DashboardTitleVO();
        dashboardTitleVO.setUsers(Math.toIntExact(totalUserFuture.join()));
        dashboardTitleVO.setBlogs(Math.toIntExact(totalBlogFuture.join()));
        dashboardTitleVO.setComments(Math.toIntExact(totalComRepFuture.join()));
        dashboardTitleVO.setKnowledgePoints(Math.toIntExact(totalPointFuture.join()));

        return ResultUtil.success(dashboardTitleVO);
    }

    /**
     * 近12个月的用户增长信息
     * @return
     */
    @Override
    public ResultUtil<User12MonthVO> user12MonthVOResultUtil() {
        // 1. 从数据库获取原始数据
        List<User12MonthDTO> rawData = baseMapper.selectUserGrowthLast12Months();

        // 将原始数据转为 Map 方便检索: { "2025-03": 15 }
        Map<String, Integer> dataMap = rawData.stream().collect(Collectors.toMap(
                User12MonthDTO::getMonth,
                User12MonthDTO::getCount
        ));

        List<String> months = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM");

        // 2. 生成近12个月的连续时间轴并填充数据
        for (int i = 11; i >= 0; i--) {
            String month = LocalDate.now().minusMonths(i).format(formatter);
            months.add(month);
            // 如果某月没数据，补 0
            counts.add(dataMap.getOrDefault(month, 0));
        }

        // 3. 组装 VO
        User12MonthVO vo = new User12MonthVO();
        vo.setMonthList(months);
        // 如果你坚持 VO 里的类型是 List<Integer>，这里需要转一下
        vo.setUserCountList(counts);
        return ResultUtil.success(vo);
    }

    /**
     * 修改用户信息
     * @param userItemInfoDTO
     * @param loginUser
     * @return
     */
    @Override
    public ResultUtil<Boolean> updateUserInfo(UserItemInfoDTO userItemInfoDTO, LoginUser loginUser) {

        User one = getOne(Wrappers.<User>lambdaQuery()
                .eq(User::getName, userItemInfoDTO.getUserName())
                .ne(User::getId, loginUser.getUserId())
        );
        if(Objects.nonNull(one)){
            return ResultUtil.fail("命名重复");
        }

        return ResultUtil.success(update(Wrappers.<User>lambdaUpdate()
                .eq(User::getId, loginUser.getUserId())
                .set(User::getName, userItemInfoDTO.getUserName())
                .set(User::getImageUrl, userItemInfoDTO.getImageHead())
                .set(User::getIntroduce, userItemInfoDTO.getIntroduction())
        ));
    }

    @Override
    public ResultUtil<List<UserInfoVO>> hotAuthorList(LoginUser loginUser) {
        Long userId = null;
        if(YesOrNoEnum.YES.getCode().equals( loginUser.getIsUserLogin())){
            userId = loginUser.getUserId();
        }
        List<Long> hotAuthor = blogFeign.getHotAuthor(userId);
        if(hotAuthor == null || hotAuthor.isEmpty()){
            return ResultUtil.fail("查询失败");
        }

        List<User> list = list(Wrappers.<User>lambdaQuery()
                .in(User::getId, hotAuthor)
        );
        List<UserInfoVO> result = list.stream().map(one -> {
            UserInfoVO userInfoVO = new UserInfoVO();
            userInfoVO.setUserName(one.getName());
            userInfoVO.setUserId(String.valueOf(one.getId()));
            userInfoVO.setHead(one.getImageUrl());
            userInfoVO.setIntroduce(Strings.isNotBlank(one.getIntroduce()) ? one.getIntroduce() : "这个人很神秘 什么都没留下...");
            UserBlogInfoDTO userBlogInfo = blogFeign.getUserBlogInfo(one.getId());
            userInfoVO.setLikes(String.valueOf(userBlogInfo.getLikes()));
            userInfoVO.setBlogs(String.valueOf(userBlogInfo.getBlogs()));
            userInfoVO.setStar(String.valueOf(userBlogInfo.getStar()));
            return userInfoVO;
        }).toList();

        return  ResultUtil.success(result);
    }


}
