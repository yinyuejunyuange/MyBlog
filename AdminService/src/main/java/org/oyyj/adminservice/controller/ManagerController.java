package org.oyyj.adminservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.oyyj.adminservice.dto.AddAdminDTO;
import org.oyyj.adminservice.dto.AdminDTO;
import org.oyyj.adminservice.dto.PageDTO;
import org.oyyj.adminservice.pojo.Admin;
import org.oyyj.adminservice.pojo.AdminRole;
import org.oyyj.adminservice.pojo.LoginAdmin;
import org.oyyj.adminservice.pojo.Role;
import org.oyyj.adminservice.service.IAdminRoleService;
import org.oyyj.adminservice.service.IAdminService;
import org.oyyj.adminservice.service.IRoleService;
import org.oyyj.adminservice.util.RSAUtil;
import org.oyyj.adminservice.util.ResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用于管理所有管理员的控制器 只有super_admin可以访问
 */
@RestController
@RequestMapping("/admin/manager")
public class ManagerController {

    @Autowired
    private IAdminService adminService;

    @Autowired
    private IAdminRoleService adminRoleService;

    @Autowired
    private IRoleService roleService;


    private static final Logger log= LoggerFactory.getLogger(ManagerController.class);


    // 查询 管理员
    @PreAuthorize("hasAuthority('super_admin')")
    @GetMapping("/getManagers")
    public Map<String,Object> getManagers(@RequestParam(value = "name",required = false) String name,
                                          @RequestParam(value = "phone",required = false)String phone,
                                          @RequestParam(value = "adminType",required = false)String adminType,
                                          @RequestParam(value = "createBy",required = false)String createBy,
                                          @RequestParam(value = "startTime",required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startTime,
                                          @RequestParam(value = "endTime",required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endTime,
                                          @RequestParam(value = "isFreeze",required = false)Integer isFreeze,
                                          @RequestParam("currentPage") Integer currentPage){

        return adminService.getManagers(name,phone,adminType,createBy,startTime,endTime,isFreeze,currentPage);

    }

    // 新增管理员
    @PreAuthorize("hasAuthority('super_admin')")
    @PostMapping("/addAdmin")
    public Map<String,Object> addAdmin(@RequestBody AddAdminDTO addAdminDTO) throws Exception {
        return adminService.addAdmin(addAdminDTO);
    }

    // 修改管理员状态（冻结 与解冻）
    @PreAuthorize("hasAuthority('super_admin')")
    @PutMapping("/updateAdmin")
    public Map<String,Object> updateAdmin(@RequestParam("adminId") String adminId,
                                          @RequestParam("isFreeze") Integer isFreeze ){
        return adminService.updateAdmin(adminId,isFreeze);
    }

    // 删除管理员
    @PreAuthorize("hasAuthority('super_admin')")
    @DeleteMapping("/deleteAdmin")
    public Map<String,Object>  deleteAdmin(@RequestParam("adminId") String adminId){

        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();


        boolean remove = adminService.remove(Wrappers.<Admin>lambdaQuery().eq(Admin::getId, adminId));
        if(remove){
            log.info("管理员 {} 删除管理员 {} ",principal.getAdmin(),adminId);
            return ResultUtil.successMap(null,"删除成功");
        }else{
            log.info("管理员{}删除失败 ",principal.getAdmin());
            return ResultUtil.failMap("删除失败");
        }
    }




}
