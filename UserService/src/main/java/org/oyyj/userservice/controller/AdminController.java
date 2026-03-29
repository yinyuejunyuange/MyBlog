package org.oyyj.userservice.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.oyyj.mycommon.annotation.RequestRole;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.RoleEnum;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.userservice.Feign.BlogFeign;
import org.oyyj.userservice.pojo.SysRole;
import org.oyyj.userservice.pojo.User;
import org.oyyj.userservice.service.ISysRoleService;
import org.oyyj.userservice.service.IUserService;
import org.oyyj.userservice.vo.DashboardTitleVO;
import org.oyyj.userservice.vo.UserInfoForAdminVO;
import org.oyyj.userservice.vo.user.User12MonthVO;
import org.oyyj.userservice.vo.user.UserDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

/**
 * 管理员操作的相关接口
 */
@RestController
@RequestMapping("/myBlog/admin")
public class AdminController {


    @Autowired
    private IUserService  userService;

    @Autowired
    private ISysRoleService  roleService;

    @Autowired
    private BlogFeign blogFeign;

    /**
     * 添加角色
     * @param roleName
     * @return
     */
    @RequestRole(role = {RoleEnum.SUPER_ADMIN})
    @PostMapping("role")
    public ResultUtil<String> addRole(@RequestParam("roleName")String roleName){
        SysRole sysRole = new SysRole();
        sysRole.setRoleName(roleName);
        roleService.save(sysRole);
        return ResultUtil.success("添加成功");
    }

    /**
     * 修改角色名称
     * @param roleId
     * @param roleName
     * @return
     */
    @RequestRole(role = {RoleEnum.SUPER_ADMIN})
    @PutMapping("role")
    public ResultUtil<String> updateRole(@RequestParam("roleId")String roleId ,@RequestParam("roleName") String roleName){
        SysRole sysRole = new SysRole();
        sysRole.setRoleName(roleName);
        sysRole.setId(Long.parseLong(roleId));
        roleService.saveOrUpdate(sysRole);
        return ResultUtil.success("修改成功");
    }

    /**
     * 冻结角色
     * @param roleId
     * @return
     */
    @RequestRole(role = {RoleEnum.SUPER_ADMIN})
    @PutMapping("freezeRole")
    public ResultUtil<String> freezeRole(@RequestParam("roleId")String roleId){
        SysRole sysRole = new SysRole();
        sysRole.setId(Long.parseLong(roleId));
        sysRole.setIsStop(YesOrNoEnum.YES.getCode());
        roleService.updateById(sysRole);
        return ResultUtil.success("修改成功");
    }

    /**
     * 解冻角色
     * @param roleId
     * @return
     */
    @RequestRole(role = {RoleEnum.SUPER_ADMIN})
    @PutMapping("unFreezeRole")
    public ResultUtil<String> unFreezeRole(@RequestParam("roleId")String roleId){
        SysRole sysRole = new SysRole();
        sysRole.setId(Long.parseLong(roleId));
        sysRole.setIsStop(YesOrNoEnum.NO.getCode());
        roleService.updateById(sysRole);
        return ResultUtil.success("修改成功");
    }

    /**
     * 解冻角色
     * @param roleId
     * @return
     */
    @RequestRole(role = {RoleEnum.SUPER_ADMIN})
    @DeleteMapping("role")
    public ResultUtil<String> deleteRole(@RequestParam("roleId")String roleId){
        roleService.remove(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getId, Long.parseLong(roleId))
        );
        return ResultUtil.success("删除成功");
    }

    /**
     * 冻结用户
     * @param userId
     * @return
     */
    @RequestRole(role = {RoleEnum.SUPER_ADMIN,RoleEnum.ADMIN})
    @PutMapping("freezeUser")
    public ResultUtil<String> freezeUser(@RequestParam("userId")String userId){

        // 检查是否是管理员
        List<String> userRoleInfo = userService.getUserRoleInfo(Long.parseLong(userId));
        if(userRoleInfo.contains(RoleEnum.SUPER_ADMIN)||userRoleInfo.contains(RoleEnum.ADMIN)){
            return ResultUtil.fail("当前用户无权操作");
        }
        userService.update(Wrappers.<User>lambdaUpdate()
                .eq(User::getId, Long.parseLong(userId))
                .set(User::getIsFreeze, YesOrNoEnum.YES.getCode())
        );
        return ResultUtil.success("用户冻结成功");
    }

    /**
     * 解冻用户
     * @param userId
     * @return
     */
    @RequestRole(role = {RoleEnum.SUPER_ADMIN,RoleEnum.ADMIN})
    @PutMapping("unFreezeUser")
    public ResultUtil<String> unFreezeUser(@RequestParam("userId")String userId){

        // 检查是否是管理员
        List<String> userRoleInfo = userService.getUserRoleInfo(Long.parseLong(userId));
        if(userRoleInfo.contains(RoleEnum.SUPER_ADMIN)||userRoleInfo.contains(RoleEnum.ADMIN)){
            return ResultUtil.fail("当前用户无权操作");
        }

        userService.update(Wrappers.<User>lambdaUpdate()
                .eq(User::getId, Long.parseLong(userId))
                .set(User::getIsFreeze, YesOrNoEnum.NO.getCode())
        );
        return ResultUtil.success("用户冻结成功");
    }

    /**
     * 冻结用户
     * @param userId
     * @return
     */
    @RequestRole(role = {RoleEnum.SUPER_ADMIN})
    @PutMapping("freezeAdmin")
    public ResultUtil<String> freezeAdmin(@RequestParam("userId")String userId){
        // 检查是否是管理员
        List<String> userRoleInfo = userService.getUserRoleInfo(Long.parseLong(userId));
        if(userRoleInfo.contains(RoleEnum.SUPER_ADMIN)){
            return ResultUtil.fail("当前用户无权操作");
        }

        userService.update(Wrappers.<User>lambdaUpdate()
                .eq(User::getId, Long.parseLong(userId))
                .set(User::getIsFreeze, YesOrNoEnum.YES.getCode())
        );
        return ResultUtil.success("用户冻结成功");
    }

    /**
     * 解冻用户
     * @param userId
     * @return
     */
    @RequestRole(role = {RoleEnum.SUPER_ADMIN})
    @PutMapping("unFreezeAdmin")
    public ResultUtil<String> unFreezeAdmin(@RequestParam("userId")String userId){

        List<String> userRoleInfo = userService.getUserRoleInfo(Long.parseLong(userId));
        if(userRoleInfo.contains(RoleEnum.SUPER_ADMIN)){
            return ResultUtil.fail("当前用户无权操作");
        }

        userService.update(Wrappers.<User>lambdaUpdate()
                .eq(User::getId, Long.parseLong(userId))
                .set(User::getIsFreeze, YesOrNoEnum.NO.getCode())
        );
        return ResultUtil.success("用户冻结成功");
    }

    /**
     * 用户分页
     * @param userName
     * @param startTime
     * @param endTime
     * @param isUserFreeze
     * @param pageNum
     * @param pageSize
     * @return
     */
    // 缺少查看 分页查看用户以及管理员信息
    @RequestRole(role = {RoleEnum.SUPER_ADMIN,RoleEnum.ADMIN})
    @GetMapping("/userPage")
    public ResultUtil<Page<UserInfoForAdminVO>> userListInfo(@RequestParam(value = "userName",required = false) String userName,
                                                             @RequestParam(value = "startTime",required = false) Date startTime,
                                                             @RequestParam(value = "endTime",required = false) Date endTime,
                                                             @RequestParam(value = "isUserFreeze",required = false) Integer isUserFreeze,
                                                             @RequestParam("currentPage") Integer currentPage,
                                                             @RequestParam("pageSize") Integer pageSize){
        return ResultUtil.success(userService.getUserInfoForAdmin(userName,startTime,endTime,isUserFreeze,currentPage,pageSize));
    }

    /**
     * 管理员分页
     * @param userName
     * @param startTime
     * @param endTime
     * @param isUserFreeze
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestRole(role = {RoleEnum.SUPER_ADMIN})
    @GetMapping("/adminPage")
    public ResultUtil<Page<UserInfoForAdminVO>> adminListInfo(@RequestParam(value = "userName",required = false) String userName,
                                                             @RequestParam(value = "startTime",required = false) Date startTime,
                                                             @RequestParam(value = "endTime",required = false) Date endTime,
                                                             @RequestParam(value = "isUserFreeze",required = false) Integer isUserFreeze,
                                                             @RequestParam("currentPage") Integer currentPage,
                                                             @RequestParam("pageSize") Integer pageSize){
        return ResultUtil.success(userService.getAdminPage(userName,startTime,endTime,isUserFreeze,currentPage,pageSize));
    }

    /**
     * 查看用户详情
     * @param userId
     * @return
     */
    @RequestRole(role = {RoleEnum.ADMIN,RoleEnum.SUPER_ADMIN})
    @GetMapping("/userDetail")
    public ResultUtil<UserDetailDTO> userDetail(@RequestParam("userId") String userId){
        return ResultUtil.success(userService.getUserDetail(userId));
    }

    /**
     * 仪表盘 最上层的数据统计
     *
     * @return
     */
    @RequestRole(role = {RoleEnum.ADMIN,RoleEnum.SUPER_ADMIN})
    @GetMapping("/dashBoardTitle")
    public ResultUtil<DashboardTitleVO> dashBoardTitle(){
        return userService.getDashboardTitle();
    }

    /**
     * 近12个月的用户增长量
     * @return
     */
    @RequestRole(role = {RoleEnum.ADMIN,RoleEnum.SUPER_ADMIN})
    @GetMapping("/user12Month")
    public ResultUtil<User12MonthVO> user12MonthVOResultUtil(){
        return userService.user12MonthVOResultUtil();
    }



}
