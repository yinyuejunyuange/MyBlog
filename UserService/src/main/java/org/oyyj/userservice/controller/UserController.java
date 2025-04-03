package org.oyyj.userservice.controller;

import com.alibaba.nacos.api.naming.pojo.healthcheck.impl.Http;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.oyyj.userservice.DTO.*;
import org.oyyj.userservice.Feign.AnnouncementFeign;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.pojo.JWTUser;
import org.oyyj.userservice.pojo.LoginUser;
import org.oyyj.userservice.pojo.User;
import org.oyyj.userservice.pojo.UserReport;
import org.oyyj.userservice.service.IUserReportService;
import org.oyyj.userservice.service.IUserService;
import org.oyyj.userservice.utils.ResultUtil;
import org.oyyj.userservice.vo.AdminUpdateUserReportVO;
import org.oyyj.userservice.vo.AnnouncementUserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.security.sasl.AuthenticationException;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/myBlog/user")
public class UserController {


    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private IUserService userService;
    @Autowired
    private ServletContext servletContext;

    @Autowired
    private AnnouncementFeign announcementFeign;

    @Autowired
    private IUserReportService userReportService;
    @Autowired
    private BlogFeign blogFeign;

    @Value("${user.header-url}")
    private String userHeadUrl;



    // 用户登录
    @PostMapping("/login")
    public Map<String,Object> UserLogin(@RequestBody UserDTO userDTO) throws JsonProcessingException {

        JWTUser login = userService.login(userDTO.getUsername(), userDTO.getPassword());
        return ResultUtil.successMap(login,"登录成功");
    }

    // 用户注册
    @PostMapping("/register")
    public Map<String,Object> UserRegister(@RequestBody RegisterDTO registerDTO) throws IOException {
        JWTUser jwtUser = userService.registerUser(registerDTO);
        if(Objects.isNull(jwtUser)){
            return ResultUtil.failMap("用户名重复 请重新注册");
        }
        return ResultUtil.successMap(jwtUser,"注册成功 已登录");
    }


    // 用户登出
    @GetMapping("/logout")
    public Map<String,Object> UserLogout() {
        userService.LoginOut();
        return ResultUtil.successMap(null,"退出成功");
    }


    // 用户存储 头像
    @RequestMapping("/makeHead")
    public Map<String,Object> makeUserHead(@RequestParam("file")MultipartFile file) throws IOException {
        String fileName= UUID.randomUUID().toString().substring(0,10)+file.getOriginalFilename();
        // String filePath= ResourceUtils.getURL("classpath:").getPath()+"static/image/"+fileName;
        String filePath= "H:/10516/Test/image/"+fileName;

        FileUtils.copyInputStreamToFile(file.getInputStream(),new File(servletContext.getContextPath()+"/"+filePath));


        // 从上下文环境获取到数据
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();

        // 存储用户头像
        boolean update = userService.update(Wrappers.<User>lambdaUpdate().eq(User::getId,principal.getUser().getId())
                .set(User::getImageUrl, fileName));// 存储用户头像
        if(update){
            return ResultUtil.successMap("http://localhost:8080/myBlog/user/getHead/"+fileName,"存储成功");
        }

        return ResultUtil.failMap("存储失败");

    }


    // 获取用户头像的方法
    @GetMapping("/getHead/{fileName}")
    public void getUserHead(@PathVariable("fileName") String fileName , HttpServletResponse response) throws IOException {
        // String filePath= ResourceUtils.getURL("classpath:").getPath()+"static/image/"+fileName;
        //String filePath= "H:/10516/Test/image/"+fileName;
        String filePath= userHeadUrl+fileName;
        String encodedFileName = URLEncoder.encode(filePath, StandardCharsets.UTF_8); // 避免有中文名 设置字符
        System.out.println("head path:"+filePath);

        File file=new File(filePath);
        if(!file.exists()){
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return ;
        }

        // 设置响应头
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+encodedFileName+"\"");

        Files.copy(file.toPath(),response.getOutputStream());
        response.getOutputStream().flush();
    }

    @GetMapping(value = "/getUserName")
    public Map<Long,String> getAllUserName(HttpServletRequest request){
        String source = request.getHeader("source");
        //System.out.println(source+":================");
        if(source==null||!source.equals("BLOGSERVICE")){
            log.error("请求来源错误");
            return null;
        }
        Map<Long,String> maps=userService.list().stream().collect(Collectors.toMap(User::getId, User::getName));
        System.out.println(maps);
        return userService.list().stream().collect(Collectors.toMap(User::getId, User::getName));
    }

    //给其他服务调用以获取用户名

    @GetMapping("/getNameInIds")
    public Map<Long,String> getUserNameInIds(@RequestParam("ids") List<String>ids, HttpServletRequest request){
        String source = request.getHeader("source");
        //System.out.println(source+":================");
        if(source==null||!source.equals("BLOGSERVICE")){
            log.error("请求来源错误");
            return null;
        }
        if(ids.isEmpty()){
            return null;
        }
        Map<Long, String> collect = userService.list(Wrappers.<User>lambdaQuery()
                        .in(User::getId, ids.stream().map(Long::valueOf).toList()))
                .stream().collect(Collectors.toMap(User::getId, User::getName));

        System.out.println(collect);
        return collect;
    }

    /**
     * 给其他服务调用 以获取用户头像
     * @param ids
     * @param request
     * @return
     */
    @GetMapping("/getImageInIds")
    public Map<Long,String> getUserImageInIds(@RequestParam("ids") List<String>ids, HttpServletRequest request){
        String source = request.getHeader("source");
        //System.out.println(source+":================");
        if(source==null||!source.equals("BLOGSERVICE")){
            log.error("请求来源错误");
            return null;
        }
        if(ids.isEmpty()){
            return null;
        }

        Map<Long, String> collect = userService.list(Wrappers.<User>lambdaQuery()
                        .in(User::getId, ids.stream().map(Long::valueOf).toList()))
                .stream().collect(Collectors.toMap(User::getId, User::getImageUrl));

        System.out.println(collect);
        return collect;
    }


    @GetMapping("/getBlogUserInfo")
    public Map<String,Object> getBlogUserInfo(@RequestParam("userId")String userId){
        BlogUserInfoDTO blogUserInfo = userService.getBlogUserInfo(userId);
        if(blogUserInfo==null){
            return ResultUtil.failMap("参数不合法");
        }
        return ResultUtil.successMap(blogUserInfo,"数据查询成功");
    }

    // 用户关注作者
    @PutMapping("/starBlogAuthor")
    public Map<String,Object> starBlogAuthor(@RequestParam("authorId")String authorId){

        Boolean b = userService.starBlogAuthor(authorId);
        if(b){
            return ResultUtil.successMap(b,"关注成功");
        }else{
            return ResultUtil.failMap("关注失败");
        }
    }

    @PutMapping("/cancelStarBlogAuthor")
    public Map<String,Object> cancelStarBlogAuthor(@RequestParam("authorId")String authorId){

        Boolean b = userService.cancelStarBlogAuthor(authorId);
        if(b){
            return ResultUtil.successMap(b,"取消关注成功");
        }else{
            return ResultUtil.failMap("操作失败");
        }
    }

//    // 用户改变个人信息
//
    @PostMapping("/changeUserInfo")
    public Map<String,Object> changeUserInfo(@RequestBody ChangeUserDTO changeUserDTO){
        return userService.changeUserInfo(changeUserDTO);
    }


    /**
     * 热门搜索
     */
    @GetMapping("/getHotSearch")
    public Map<String,Object> getHotSearch(){
        return userService.getHotSearch();
    }

    /**
     * 用户的搜索
     */
    @GetMapping("/getUserSearch")
    public Map<String,Object> getUserSearch(){
        List<String> userSearch = userService.getUserSearch();
        return ResultUtil.successMap(userSearch,"查询成功");
    }

    // 用户删除自己的搜索记录
    @DeleteMapping("deleteUserSearchByName")
    public Map<String,Object> deleteUserSearchByName( @RequestParam("name") String name){
        boolean b = userService.deleteUserSearchByName(name);
        if(b){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }

    @DeleteMapping("deleteUserAllSearch")
    public Map<String,Object> deleteUserAllSearch( ){
        boolean b = userService.deleteUserAllSearch();
        if(b){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }

    // 获取所有用户个数
    @GetMapping("/getUserNum")
    public Long getUserNum( HttpServletRequest request){
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求 来源不正确");
            throw new RuntimeException("来源不正确");
        }
        return (long) userService.list().size();
    }

    // 根据条件查询用户

    @GetMapping("/getUserInfoList")
    public String getUserInfoList(@RequestParam(value = "name",required = false) String name,
                                              @RequestParam(value = "email",required = false) String email,
                                              @RequestParam(value = "startDate",required = false) Date startDate,
                                              @RequestParam(value = "endDate",required = false) Date endDate,
                                              @RequestParam(value = "status",required = false) String status,
                                              @RequestParam(value = "currentPage") Integer currentPage,
                                              HttpServletRequest request) throws AuthenticationException, JsonProcessingException {

        IPage<User> userIPage=new Page<>(currentPage,10);

        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求 来源错误");
            throw new AuthenticationException("请求 来源错误");
        }

        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>lambdaQuery();
        if(Objects.nonNull(name)){
            queryWrapper.like(User::getName,name);
        }
        if(Objects.nonNull(email)){
            queryWrapper.like(User::getEmail,email);
        }
        if(Objects.nonNull(startDate)){
            queryWrapper.ge(User::getCreateTime,startDate);
        }
        if(Objects.nonNull(endDate)){
            queryWrapper.le(User::getCreateTime,endDate);
        }
        if(Objects.nonNull(status)){
            switch (status){
                case "冻结":
                    queryWrapper.eq(User::getIsFreeze,1);
                    break;
                case "正常":
                    queryWrapper.eq(User::getIsFreeze,2);
                    break;
                case "禁止":
                    queryWrapper.eq(User::getIsFreeze,3);
                    break;
                default:
                    log.error("查询用户的状态字段异常");
                    break;
            }
        }


        List<AdminUserDTO> list = userService.list(userIPage, queryWrapper).stream().map(i -> {
            AdminUserDTO build = AdminUserDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .name(i.getName())
                    .imageUrl("http://localhost:8080/myBlog/user/getHead/" + i.getImageUrl())
                    .email(i.getEmail())
                    .createTime(i.getCreateTime())
                    .updateTime(i.getUpdateTime())
                    .build();
            switch (i.getIsFreeze()) {
                case 1:
                    build.setStatus("冻结");
                    break;
                case 2:
                    build.setStatus("正常");
                    break;
                case 3:
                    build.setStatus("禁止");
                    break;
                default:
                    log.error("数据库用户状态字段异常 ");
                    break;
            }
            return build;
        }).toList();

        AdminUserPageDTO adminUserPageDTO = new AdminUserPageDTO();
        adminUserPageDTO.setCurrentPage(currentPage);
        adminUserPageDTO.setTotalPage(userIPage.getTotal());
        adminUserPageDTO.setUsers(list);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(adminUserPageDTO);
    }


    // 修改用户数据
    @PutMapping("/updateUserStatus")
    public Map<String,Object> updateUserStatus(@RequestBody Map<String,Object> userUpdateMap,HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求 来源错误");
            throw new AuthenticationException("请求 来源错误");
        }

        String userId = String.valueOf(userUpdateMap.get("userId"));
        String status = String.valueOf(userUpdateMap.get("status"));
        int isFreeze;
        switch (status) {
            case "冻结":
                isFreeze = 1;
                break;
            case "正常":
                isFreeze = 2;
                break;
            case "禁止":
                isFreeze = 3;
                break;
            default:
                log.error("请求字段异常 : "+status);
                throw new AuthenticationException("请求字段异常");

        }

        boolean update = userService.update(Wrappers.<User>lambdaUpdate().eq(User::getId, Long.valueOf(userId))
                .set(User::getIsFreeze, isFreeze));
        if(update){
            return  ResultUtil.successMap(null,"修改成功");
        }else{
            return  ResultUtil.failMap("修改失败");
        }

    }

    @DeleteMapping("/deleteUser")
    public Boolean deleteUser(@RequestParam("userId")Long userId ,HttpServletRequest request) throws AuthenticationException {

        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求 来源错误");
            throw new AuthenticationException("请求 来源错误");
        }

        return userService.remove(Wrappers.<User>lambdaQuery().eq(User::getId, userId));
    }

    @GetMapping("/getUserIdByName")
    public String getUserIdByName(@RequestParam("userName") String userName, HttpServletRequest request) throws AuthenticationException {

        if(!"BLOGSERVICE".equals(request.getHeader("source"))){
            log.error("请求 来源错误");
            throw new AuthenticationException("请求 来源错误");
        }

        Long userId = userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getName, userName)).getId();
        return String.valueOf(userId);
    }


    @GetMapping("/isUserExist")
    public Date isUserExist(@RequestParam("userId")Long userId ,HttpServletRequest request) throws AuthenticationException {
        if(!"TASKSERVICE".equals(request.getHeader("source"))){
            log.error("请求 来源错误");
            throw new AuthenticationException("请求 来源错误");
        }

        User one = userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getId, userId));
        if(Objects.isNull(one)){
            return null;
        }else{
            return one.getCreateTime();
        }

    }

    @GetMapping("/getAnnouncement")
    public Map<String,Object> getAnnouncement(@RequestParam("currentIndex") Integer currentIndex){
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();

        List<AnnouncementUserVO> announcementByUser = announcementFeign.getAnnouncementByUser(principal.getUser().getId(), currentIndex,principal.getUser().getCreateTime());
        List<AnnouncementUserDTO> list = announcementByUser.stream().map(i -> AnnouncementUserDTO.builder()
                .id(String.valueOf(i.getId()))
                .isUserRead(i.getIsUserRead())
                .title(i.getTitle())
                .content(i.getContent())
                .updateTime(i.getUpdateTime())
                .build()).toList();

        return ResultUtil.successMap(list,"查询成功");
    }

    @PutMapping("/readAnnouncement")
    public Map<String,Object> readAnnouncement(@RequestParam(value = "announcementId" )String announcementId) throws AuthenticationException {
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        boolean b = announcementFeign.readAnnouncement(principal.getUser().getId(), Long.parseLong(announcementId));
        if(b){
            return ResultUtil.successMap(b,"操作成功");
        }else{
            return ResultUtil.failMap("操作失败");
        }
    }

    @GetMapping("/getIdsLikeName")
    public List<Long> getIdsLikeName(@RequestParam("name") String name,HttpServletRequest request){
        if(!"BLOGSERVICE".equals(request.getHeader("source"))){
            log.error("获取到来源不正确的请求");
            throw new IllegalArgumentException("请求来源不正确");
        }

        return userService.list(Wrappers.<User>lambdaQuery().like(User::getName, name)).stream().map(User::getId).toList();

    }


    // 用户举报 用户
    @PutMapping("/reportUser")
    public Map<String,Object> reportUser(@RequestBody ReportUserDTO reportUserDTO ) throws AuthenticationException {
        return  userReportService.reportUser(reportUserDTO);
    }

    // 管理员查询举报信息
    @GetMapping("/getUserReports")
    public PageDTO<UserReportForAdminDTO> getUserReports(@RequestParam("currentPage") Integer currentPage,
                                                         @RequestParam(value = "adminName",required = false) String adminName,
                                                         @RequestParam(value = "status",required = false) Integer status,
                                                      HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("数据来源不正确");
            throw new AuthenticationException("数据来源不正确");
        }

        return userReportService.reportUserForAdmin(currentPage,adminName,status);
    }

    // 管理员修改 举报信息
    @PutMapping("/updateUserReport")
    public Map<String,Object> updateUserReport(@RequestBody AdminUpdateUserReportVO adminUpdateUserReportVO
            ,HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }

        return userReportService.updateUserReport(adminUpdateUserReportVO);

    }

    // 管理员删除 举报信息

    @DeleteMapping("/deleteUserReport")
    public Map<String ,Object> deleteUserReport(@RequestParam("userReportId")String userReportId,
                                                HttpServletRequest request) throws AuthenticationException {
        if(!"ADMINSERVICE".equals(request.getHeader("source"))){
            log.error("请求来源不正确");
            throw new AuthenticationException("请求来源不正确");
        }
        boolean remove = userReportService.remove(Wrappers.<UserReport>lambdaQuery()
                .eq(UserReport::getId, Long.parseLong(userReportId)));
        if(remove){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }


    // 用户举报 博客
    @PutMapping("/reportBlog")
    public Map<String,Object> reportBlog(@RequestBody BlogReportDTO blogReportDTO ) throws AuthenticationException {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        blogReportDTO.setUserId(String.valueOf(principal.getUser().getId()));
        return  blogFeign.reportBlogs(blogReportDTO);
    }

    // 用户举报 评论
    @PutMapping("/reportComment")
    public Map<String,Object> reportComment(@RequestBody CommentReportDTO commentReportDTO ) throws AuthenticationException {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        commentReportDTO.setUserId(String.valueOf(principal.getUser().getId()));
        return  blogFeign.reportComments(commentReportDTO);
    }
    // 用户举报
    @PutMapping("/reportReply")
    public Map<String,Object> reportReply(@RequestBody ReplyReportDTO replyReportDTO ) throws AuthenticationException {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser principal = (LoginUser) authentication.getPrincipal();
        replyReportDTO.setUserId(String.valueOf(principal.getUser().getId()));
        return  blogFeign.reportReply(replyReportDTO);
    }



}
