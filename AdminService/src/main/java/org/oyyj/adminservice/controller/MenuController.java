package org.oyyj.adminservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.oyyj.adminservice.dto.AddMenuDTO;
import org.oyyj.adminservice.dto.MenuAdminDTO;
import org.oyyj.adminservice.dto.MenuDTO;
import org.oyyj.adminservice.dto.ModifyMenuDTO;
import org.oyyj.adminservice.mapper.RoleMenuMapper;
import org.oyyj.adminservice.pojo.LoginAdmin;
import org.oyyj.adminservice.pojo.Role;
import org.oyyj.adminservice.pojo.RoleMenu;
import org.oyyj.adminservice.pojo.SysMenu;
import org.oyyj.adminservice.service.IAdminService;
import org.oyyj.adminservice.service.ISysMenuService;
import org.oyyj.adminservice.util.ResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.security.sasl.AuthenticationException;
import java.rmi.MarshalledObject;
import java.util.*;

@RestController
@RequestMapping("/admin/menu")
public class MenuController {

    private static final Logger log = LoggerFactory.getLogger(MenuController.class);
    @Autowired
    private IAdminService adminService;

    @Autowired
    private ISysMenuService sysMenuService;
    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @GetMapping("/getMenu")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> getMenu() throws AuthenticationException {
        List<MenuDTO> menuDTO = adminService.getMenuDTO();
        return ResultUtil.successMap(menuDTO,"查询成功");
    }
    @GetMapping("/getMenuForManage")
    @PreAuthorize("hasAuthority('super_admin')")
    public Map<String,Object> getMenuForManage() throws AuthenticationException {
        List<MenuAdminDTO> menuAdminDTOS = sysMenuService.adminMenuAdmin();
        return ResultUtil.successMap(menuAdminDTOS,"查询成功");
    }

    // 新增菜单
    @PostMapping("/addMenu")
    @PreAuthorize("hasAuthority('super_admin')")
    @Transactional(rollbackFor = Exception.class) // 只要有异常就回滚
    public Map<String,Object> addMenu(@RequestBody AddMenuDTO addMenuDTO) throws AuthenticationException {
        return sysMenuService.addMenu(addMenuDTO);
    }


    // 修改  可见类型 路径 菜单名称 排序 名称
    @PutMapping("/modifyMenu")
    @PreAuthorize("hasAuthority('super_admin')")
    public Map<String,Object> modifyMenu(@RequestBody ModifyMenuDTO modifyMenuDTO) throws AuthenticationException {
        return sysMenuService.modifyMenu(modifyMenuDTO);

    }

    // 修改 子菜单 可见类型 路径 菜单名称 排序 名称
    @PutMapping("/modifySonMenu")
    @PreAuthorize("hasAuthority('super_admin')")
    public Map<String,Object> modifySonMenu(@RequestBody ModifyMenuDTO modifyMenuDTO) throws AuthenticationException {
        return sysMenuService.modifySonMenu(modifyMenuDTO);

    }

    // 删除 菜单
    @DeleteMapping("/deleteMenu")
    @PreAuthorize("hasAuthority('super_admin')")
    public Map<String,Object> deleteMenu(@RequestParam("id") String id) throws AuthenticationException {
        boolean remove = sysMenuService.remove(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getId, id));
        if(remove){
            return ResultUtil.successMap(null,"删除成功");
        }
        return ResultUtil.failMap("删除失败");
    }

    // 添加子菜单
    @PostMapping("/addSonMenu")
    @PreAuthorize("hasAuthority('super_admin')")
    public Map<String,Object> addSonMenu(@RequestBody AddMenuDTO addMenuDTO) throws AuthenticationException {
        return sysMenuService.addSonMenu(addMenuDTO);
    }

}
