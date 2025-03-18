package org.oyyj.adminservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.oyyj.adminservice.dto.*;
import org.oyyj.adminservice.feign.BlogFeign;
import org.oyyj.adminservice.feign.UsersFeign;
import org.oyyj.adminservice.pojo.Admin;
import org.oyyj.adminservice.pojo.JWTAdmin;
import org.oyyj.adminservice.pojo.LoginAdmin;
import org.oyyj.adminservice.service.IAdminService;
import org.oyyj.adminservice.util.EmailUtil;
import org.oyyj.adminservice.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.security.sasl.AuthenticationException;
import java.util.*;


@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private IAdminService adminService;

    @Autowired
    private BlogFeign blogFeign;

    @Autowired
    private UsersFeign usersFeign;

    @Autowired
    private EmailUtil emailUtil;

    // 实现登录时 通过邮箱进行验证码验证
    @PostMapping("/login")
    public Map<String,Object> adminLogin(@RequestBody LoginDTO loginDTO) {
        try {
            JWTAdmin login = adminService.login(loginDTO.getPhone(), loginDTO.getPassword(), loginDTO.getEncode(), loginDTO.getUuid());
            if(Objects.isNull(login)){
                return ResultUtil.failMap("登录失败");
            }

            return ResultUtil.successMap(login,"登录成功");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 获取 邮箱验证
    @GetMapping("/getEncode")
    public Map<String,Object> getEncode(@RequestParam("phone") String phone) {

        Admin one = adminService.getOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getPhone, phone));

        String uuid = emailUtil.sendEncode(one.getEmail());
        if(Objects.isNull(uuid)){
            return ResultUtil.failMap("验证码发送失败");
        }
        return ResultUtil.successMap(uuid,"验证码发送成功");
    }


    // 仪表盘
        // 获取总用户数
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    @GetMapping("/getUserNum")
    public Map<String,Object> getUserNum(){
        Long userNum = usersFeign.getUserNum();
        return ResultUtil.successMap(userNum,"查询成功");
    }

        // 获取 总博客数
        @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    @GetMapping("/getBlogNum")
    public Map<String,Object> getBlogNum(){
        Long blogNum = blogFeign.getBlogNum();
        return ResultUtil.successMap(blogNum,"查询成功");
    }

        // 获取 总评论数
    @GetMapping("/getAllMessage")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> getAllMessage() {

        Long allMessage = blogFeign.getAllMessage();
        return ResultUtil.successMap(allMessage,"总评论数");
    }
        // 获取 总举报数

        // 获取 各个分类 博客的数量
    @GetMapping("/getAllTypeNum")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> getAllTypeNum() {
        Map<String, Long> allTypeNum = blogFeign.getAllTypeNum();
        List<String> typeList=new ArrayList<>();
        List<Long> typeNumList=new ArrayList<>();
        Set<String> keySet = allTypeNum.keySet();
        keySet.forEach(k->{
            if(!typeList.contains(k)){
                typeList.add(k);
                typeNumList.add(allTypeNum.get(k));
            }
        });
        Map<String,Object> map=new HashMap<>();
        map.put("typeNumList",typeNumList);
        map.put("typeList",typeList);
        return ResultUtil.successMap(map,"查询超过");
    }

        // 获取 每天 博客增加量
    @GetMapping("/getIncreaseBlog")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> getIncreaseBlog(){
        Map<String, Long> increaseBlog = blogFeign.getIncreaseBlog();
        List<String> dates=new ArrayList<>();
        List<Long> numbers=new ArrayList<>();

        increaseBlog.forEach((k,v)->{
            dates.add(k);
            numbers.add(v);
        });

        IncreaseDTO build = IncreaseDTO.builder()
                .dates(dates)
                .numbers(numbers)
                .build();

        return ResultUtil.successMap(build,"查询成功");
    }

    // 查询用户
    @GetMapping("/getUserInfoList")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> getUserInfoList(@RequestParam(value = "name",required = false) String name,
                                              @RequestParam(value = "email",required = false) String email,
                                              @RequestParam(value = "startDate",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
                                              @RequestParam(value = "endDate",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,  // spring框架默认是不支持前端 date的iso类型  所以要设置
                                              @RequestParam(value = "status",required = false) String status,
                                              @RequestParam(value = "currentPage") Integer currentPage) throws JsonProcessingException {

        String userInfoList = usersFeign.getUserInfoList(name, email, startDate, endDate, status, currentPage);
        ObjectMapper objectMapper = new ObjectMapper();
        AdminUserPageDTO adminUserPageDTO = objectMapper.readValue(userInfoList, AdminUserPageDTO.class);
        return ResultUtil.successMap(adminUserPageDTO,"查询成功");
    }

    // 修改用户状态
    @PutMapping("/updateUserStatus")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> updateUserStatus(@RequestBody UpdateStatusDTO updateStatusDTO) throws JsonProcessingException {
        try {
            Map<String,Object> map=new HashMap<>();
            map.put("status",updateStatusDTO.getStatus());
            map.put("userId",updateStatusDTO.getUserId());

            return usersFeign.updateUserStatus(map);
        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    // 逻辑删除用户
    @DeleteMapping("/deleteUser")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> deleteUser(@RequestParam(value = "userId") String userId) throws JsonProcessingException {
        Boolean result = usersFeign.deleteUser(Long.valueOf(userId));
        if(result){
            return ResultUtil.successMap(result,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }

    // 分页查询博客

    @GetMapping("/getBlogList")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> getBlogList(@RequestParam(value = "blogName",required = false) String blogName,
                                          @RequestParam(value = "authorName",required = false) String authorName,
                                          @RequestParam(value = "startDate",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
                                          @RequestParam(value = "endDate",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,  // spring框架默认是不支持前端 date的iso类型  所以要设置
                                          @RequestParam(value = "status",required = false) String status,
                                          @RequestParam(value = "currentPage") Integer currentPage) throws JsonProcessingException {

        ObjectMapper objectMapper=new ObjectMapper();
        String blogListAdmin = blogFeign.getBlogListAdmin(blogName, authorName, startDate, endDate, status, currentPage);
        PageBlogDTO pageBlogDTO = objectMapper.readValue(blogListAdmin, PageBlogDTO.class);
        if(Objects.nonNull(pageBlogDTO)){
            return ResultUtil.successMap(pageBlogDTO,"查询成功");
        }else{
            return ResultUtil.failMap("查询失败");
        }
    }

    // 修改博客状态
    @PutMapping("/updateBlogStatus")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> updateBlogStatus(@RequestBody UpdateBlogStatusDTO update) throws JsonProcessingException {
        Map<String,Object> map=new HashMap<>();
        map.put("status",update.getStatus());
        map.put("blogId",update.getBlogId());

        Boolean b = blogFeign.updateBlogStatus(map);

        if(b){
            return ResultUtil.successMap(null,"修改成功");
        }else{
            return ResultUtil.failMap("修改失败");
        }

    }

    // 删除博客
    @DeleteMapping("/deleteBlog")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> deleteBlog(@RequestParam(value = "blogId") String blogId) throws JsonProcessingException {
        Boolean b = blogFeign.deleteBlog(Long.valueOf(blogId));
        if(b){
            return  ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }
    }

}
