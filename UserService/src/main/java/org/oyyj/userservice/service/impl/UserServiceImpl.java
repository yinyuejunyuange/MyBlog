package org.oyyj.userservice.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import org.bouncycastle.jcajce.provider.symmetric.AES;
import org.oyyj.userservice.DTO.*;
import org.oyyj.userservice.Feign.AIChatFeign;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.mapper.SysRoleMapper;
import org.oyyj.userservice.mapper.UserMapper;
import org.oyyj.userservice.pojo.*;
import org.oyyj.userservice.service.*;
import org.oyyj.userservice.utils.RedisUtil;
import org.oyyj.userservice.utils.ResultUtil;
import org.oyyj.userservice.utils.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private Integer PAGE_SIZE=6;


    @Autowired
    private BlogFeign blogFeign;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private ServletContext servletContext; // 应用上下文环境

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
    private IAsyncService asyncService;

    @Autowired
    private ISearchService searchService;

    @Override // 返回相关结果
    public JWTUser login(String username, String password) throws JsonProcessingException {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        if(Objects.isNull(authentication)){
            // 认证失败
            throw new RuntimeException("登录失败 用户名或密码错误");
        }
        // 封装 userdetails信息
            // 登录成功后 authentication中的Principal 中会存储用户的信息
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        String token = TokenProvider.createToken(loginUser, "web", "USER"); // 获取到token
        // 将token存储到redis中
        redisUtil.set(String.valueOf(loginUser.getUser().getId()), token,24, TimeUnit.HOURS); // 存储并设置时间24小时
        return JWTUser.builder()
                .id(String.valueOf(loginUser.getUser().getId()))
                .username(loginUser.getUsername())
                .imageUrl(loginUser.getUser().getImageUrl())
                .token(token)
                .isValid(true)
                .build();

    }

    // 从上下文环境中 securitycontextHolder 获取到用户的信息
    @Override
    public void LoginOut() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken=
                (UsernamePasswordAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) usernamePasswordAuthenticationToken.getPrincipal();
        // 删除redis中用户的信息
        redisUtil.delete(String.valueOf(loginUser.getUser().getId()));
    }

    @Transactional
    @Override
    public JWTUser registerUser(RegisterDTO registerDTO) throws IOException {

        User one = getOne(Wrappers.<User>lambdaQuery().eq(User::getName, registerDTO.getUsername()));
        if(!Objects.isNull(one)){
            return null;
        }

        String resourcePath;
        if(registerDTO.getSex()==1){
            resourcePath="man.jpg";
        }else{
            resourcePath="woman.jpg";
        }
        String imageUrl = servletContext.getContextPath()+"/"+ resourcePath; // 获取资源路径
        Date date = new Date();
        User build = User.builder()
                .name(registerDTO.getUsername())
                .sex(registerDTO.getSex())
                .email(registerDTO.getEmail())
                .imageUrl(imageUrl)
                .createTime(date)
                .updateTime(date)
                .isDelete(0)
                .isFreeze(0)
                .build();
        String encode = passwordEncoder.encode(registerDTO.getPassword());
        build.setPassword(encode);

        boolean save = save(build);

        if(save){
            // 默认将 新注册的用户设置为 user
            Long roleId = roleMapper.selectRoleBuName("user");
            Integer i = roleMapper.defaultSetUser(build.getId(), roleId);
            if(i==0){
                // 权限添加失败
                throw new RuntimeException("权限添加 失败");
            }

            // 生成token 并存储到redis
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(registerDTO.getUsername(), registerDTO.getPassword());
            Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);

            if(Objects.isNull(authentication)){
                // 认证失败
                throw new RuntimeException("登录失败 用户名或密码错误");
            }

            LoginUser principal = (LoginUser) authentication.getPrincipal();

            String token = TokenProvider.createToken(principal, "web", "USER");
            redisUtil.set(String.valueOf(build.getId()),token,24,TimeUnit.HOURS);

            // 将用户的搜索记录添加到数据库中
            boolean b = addUserSearch(registerDTO.getSearchs());
            if(!b){
                log.error("用户搜索记录添加失败");
            }

            return JWTUser.builder()
                    .id(String.valueOf(build.getId()))
                    .isValid(true)
                    .token(token)
                    .username(build.getName())
                    .build();
        }else{
            throw  new RuntimeException("注册失败");
        }
    }

    @Override
    public Map<String, Object> saveBlog(BlogDTO blogDTO) {
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        if(Objects.isNull(authentication)){
            return ResultUtil.failMap("登录过期 请重新登录");
        }
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        Long id = principal.getUser().getId();
        blogDTO.setUserId(String.valueOf(id));
        if(Objects.isNull(blogDTO.getStatus())){
            blogDTO.setStatus(1); // 设置为保存
            // 第一次编写的博客文档 全部都设置成为 保存
        }

        Map<String, Object> map = blogFeign.writeBlog(blogDTO);
        if((int)map.get("code")==200){
//            upLoadBlogToAI(blogDTO); // 异步执行上传文件
            blogDTO.setId(String.valueOf(map.get("data")) );
            asyncService.upLoadBlogToAI(blogDTO);
        }
        return map;
    }

    @Override
    public Map<String, Object> readBlog(String blogId,String userInfoKey) {

        return blogFeign.readBlog(blogId,userInfoKey);
    }

    @Override
    public Object uploadPict(MultipartFile file) {
        return blogFeign.uploadPict(file);
    }

    @Override
    public void downloadFile(String fileName, HttpServletResponse response) {
        blogFeign.download(fileName,response);
    }

    @Override
    public boolean userKudos(String blogId) {

        // 获取当前用户---用户登录后才可以点赞和收藏
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        Long userId = principal.getUser().getId();

        System.out.println(blogId);

        boolean save = userKudosService.save(UserKudos.builder()
                .userId(userId)
                .blogId(Long.valueOf(blogId))
                .build());
        if(save){
            return save;
        }else{
            return false;
        }
    }

    @Override
    public boolean userStar(String blogId) {
        // 获取当前用户---用户登录后才可以点赞和收藏
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        Long userId = principal.getUser().getId();

        System.out.println(blogId);

        return userStarService.save(UserStar.builder()
                .blogId(Long.valueOf(blogId))
                .userId(userId)
                .build());

    }

    @Override
    public Long addComment(CommentDTO commentDTO) {
        Long commentId = blogFeign.writeComment(Long.valueOf(commentDTO.getUserId()), Long.valueOf(commentDTO.getBlogId()), commentDTO.getContext());
        if(!Objects.isNull(commentId)){
            // 博客服务添加成功
            return commentId;
        }
        // 博客评论添加失败 撤回
        return null;
    }

    @Override
    public Long addReply(ReplyDTO replyDTO) {
        Long replyId = blogFeign.replyComment(Long.valueOf(replyDTO.getUserId()), Long.valueOf(replyDTO.getCommentId()), replyDTO.getContext());
        if(!Objects.isNull(replyId)){
            return replyId;
        }

        return null;
    }

    @Override
    @Transactional // 原子性 保证回滚
    public Boolean kudosComment(String commentId,Byte bytes) {
        // 获取当前用户---用户登录后才可以点赞和收藏
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        Long userId = principal.getUser().getId();

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
    public Boolean kudosReply(String replyId,Byte bytes) {
        // 获取当前用户---用户登录后才可以点赞和收藏
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        Long userId = principal.getUser().getId();


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
    public BlogUserInfoDTO getBlogUserInfo(String userId){

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

        // 判断当前用户是否登录
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.isAuthenticated()){
            // 用户登录
            UsernamePasswordAuthenticationToken authenticationToken = (UsernamePasswordAuthenticationToken) authentication;
            LoginUser principal = (LoginUser) authenticationToken.getPrincipal();
            Long currentUserId = principal.getUser().getId();

            // 判断当前用户是否关注此作者
            UserAttention userAttention = userAttentionService.getOne(Wrappers.<UserAttention>lambdaQuery()
                    .eq(UserAttention::getUserId, currentUserId)
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
    public Boolean starBlogAuthor(String authorId) {

        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        Long userId = principal.getUser().getId();

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
    public Boolean cancelStarBlogAuthor(String authorId) {

        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        Long userId = principal.getUser().getId();

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
    public Map<String, Object> changeUserInfo(ChangeUserDTO changeUserDTO) {
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();

        User one = getOne(Wrappers.<User>lambdaQuery()
                .eq(User::getId, principal.getUser().getId())
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
    public List<String> getUserSearch() {
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        User user = principal.getUser();

        return searchService.list(Wrappers.<Search>lambdaQuery()
                .eq(Search::getUserId, user.getId())
                .orderByDesc(Search::getLatelyTime) // 按照最近时间排序
        ).stream().map(Search::getContent).toList();


    }

    @Override
    @Transactional
    public boolean addUserSearch(List<String> names) {
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        User user = principal.getUser();

        Date date=new Date();

        // 获取用户搜索记录
        List<String> list = searchService.list(Wrappers.<Search>lambdaQuery().eq(Search::getUserId, user.getId())).stream().map(Search::getContent).toList();


        List<Search> searches=new ArrayList<>();
        for (String name : names) {
            name=name.trim();
            if(name.isEmpty()||list.contains(name)){
                continue; // 为空的记录可以跳过
            }

            Search build = Search.builder()
                    .userId(user.getId())
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
    public boolean deleteUserSearchByName(String name) {

        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        User user = principal.getUser();


        if(name.trim().isEmpty()){
            log.error("参数为空");
            return false;
        }

        return searchService.update(Wrappers.<Search>lambdaUpdate()
                .eq(Search::getUserId, user.getId())
                .eq(Search::getContent, name)
                .set(Search::getIsUserDelete, 1)
        );

    }

    @Override
    public boolean deleteUserAllSearch() {
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        User user = principal.getUser();


        return searchService.update(Wrappers.<Search>lambdaUpdate()
                .eq(Search::getUserId, user.getId())
                .set(Search::getIsUserDelete, 1)
        );
    }
}
