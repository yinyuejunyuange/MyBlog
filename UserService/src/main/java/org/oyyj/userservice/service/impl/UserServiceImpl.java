package org.oyyj.userservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import org.bouncycastle.jcajce.provider.symmetric.AES;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.userservice.DTO.CommentDTO;
import org.oyyj.userservice.DTO.RegisterDTO;
import org.oyyj.userservice.DTO.ReplyDTO;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.mapper.SysRoleMapper;
import org.oyyj.userservice.mapper.UserMapper;
import org.oyyj.userservice.pojo.*;
import org.oyyj.userservice.service.*;
import org.oyyj.userservice.utils.RedisUtil;
import org.oyyj.userservice.utils.ResultUtil;
import org.oyyj.userservice.utils.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

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
            resourcePath="image/man.jpg";
        }else{
            resourcePath="image/woman.jpg";
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

        return blogFeign.writeBlog(blogDTO);
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
}
