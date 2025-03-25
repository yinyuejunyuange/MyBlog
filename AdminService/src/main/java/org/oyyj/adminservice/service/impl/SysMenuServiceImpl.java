package org.oyyj.adminservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.adminservice.dto.AddMenuDTO;
import org.oyyj.adminservice.dto.MenuAdminDTO;
import org.oyyj.adminservice.dto.MenuDTO;
import org.oyyj.adminservice.dto.ModifyMenuDTO;
import org.oyyj.adminservice.mapper.*;
import org.oyyj.adminservice.pojo.*;
import org.oyyj.adminservice.service.IAdminService;
import org.oyyj.adminservice.service.IRoleMenuService;
import org.oyyj.adminservice.service.ISysMenuService;
import org.oyyj.adminservice.util.ResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.transform.Result;
import java.util.*;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {


    @Autowired
    private IRoleMenuService roleMenuService;

    @Autowired
    private AdminMapper adminMapper; // 避免依赖循环

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    private static final Logger log= LoggerFactory.getLogger(SysMenuServiceImpl.class);

    @Override
    public List<MenuDTO> adminMenu( Long roleId ) {

        // 通过角色id获取 菜单列表
        List<Long> list = roleMenuService.list(Wrappers.<RoleMenu>lambdaQuery().eq(RoleMenu::getRoleId, roleId))
                .stream().map(RoleMenu::getMenuId).toList();

        List<MenuDTO> result = new ArrayList<>();

        for (Long l : list) {
            SysMenu one = getOne(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getId, l));
            if(Objects.isNull(one)){
                continue;
            }
            MenuDTO build = MenuDTO.builder()
                    .id(String.valueOf(one.getId()))
                    .menuName(one.getName())
                    .menuUrl(one.getUrl())
                    .parentId(String.valueOf(one.getParentId()))
                    .sort(one.getSort())
                    .children(getChildrenMenuDTO(one.getId()))
                    .build();
            result.add(build);
        }
        return result;
    }

    @Override
    public List<MenuAdminDTO> adminMenuAdmin() {

        try {
            return  list(Wrappers.<SysMenu>lambdaQuery().isNull(SysMenu::getParentId)).stream().map(i -> {
                MenuAdminDTO build = MenuAdminDTO.builder()
                        .id(String.valueOf(i.getId()))
                        .name(i.getName())
                        .url(i.getUrl())
                        .createTime(i.getCreateTime())
                        .createBy(adminMapper.selectOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getId, i.getCreateBy())).getName())
                        .updateTime(i.getUpdateTime())
                        .updateBy(adminMapper.selectOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getId, i.getUpdateBy())).getName())
                        .sort(i.getSort())
                        .children(getChildrenMenuAdminDTO(i.getId()))
                        .build();
                List<Long> list = roleMenuMapper.selectList(Wrappers.<RoleMenu>lambdaQuery()
                        .eq(RoleMenu::getMenuId, i.getId())).stream().map(RoleMenu::getRoleId).toList();
                if(!list.isEmpty()){
                    build.setAdminTypes(roleMapper.selectList(Wrappers.<Role>lambdaQuery().in(Role::getId,list))
                            .stream().map(Role::getAdminType).toList());
                }
                return build;
            }).toList();
        } catch (Exception e) {
            log.error("菜单查询失败 ：{}",e.getMessage());
            throw new RuntimeException(e);
        }


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> modifyMenu(ModifyMenuDTO modifyMenuDTO) {

        LambdaUpdateWrapper<SysMenu> lqw = new LambdaUpdateWrapper<>();
        lqw.eq(SysMenu::getId, Long.valueOf(modifyMenuDTO.getId()));

        if(Objects.nonNull(modifyMenuDTO.getName())&&!modifyMenuDTO.getName().isEmpty()){
            lqw.set(SysMenu::getName, modifyMenuDTO.getName());
        }

        if(Objects.nonNull(modifyMenuDTO.getUrl())&&!modifyMenuDTO.getUrl().isEmpty()){
            lqw.set(SysMenu::getUrl, modifyMenuDTO.getUrl());
        }

        if(Objects.nonNull(modifyMenuDTO.getSort())){
            lqw.set(SysMenu::getSort, modifyMenuDTO.getSort());
        }

        boolean update = update(lqw);
        if(update){
            // 关联 管理员类型

            // 需要先清空 管理

            boolean remove = roleMenuService.remove(Wrappers.<RoleMenu>lambdaQuery().eq(RoleMenu::getMenuId, modifyMenuDTO.getId()));

            if(remove){
                // 然后在增加
                if(!modifyMenuDTO.getAdminTypes().isEmpty()){
                    List<RoleMenu> list=new ArrayList<>();

                    for (String adminType : modifyMenuDTO.getAdminTypes()) {
                        RoleMenu roleMenu = new RoleMenu();
                        roleMenu.setMenuId(Long.valueOf(modifyMenuDTO.getId()));
                        Long roleId = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getAdminType, adminType)).getId();
                        roleMenu.setRoleId(roleId);
                        roleMenu.setIsValid(1);
                        list.add(roleMenu);
                    }

                    boolean saveBatch = roleMenuService.saveBatch(list);
                    if(saveBatch){
                        return ResultUtil.successMap(null,"修改成功");
                    }
                }
            }else{
                throw new RuntimeException("无法清空关联");
            }
        }
        return ResultUtil.failMap("修改失败");
    }

    @Override
    public Map<String, Object> modifySonMenu(ModifyMenuDTO modifyMenuDTO) {
        LambdaUpdateWrapper<SysMenu> lqw = new LambdaUpdateWrapper<>();
        lqw.eq(SysMenu::getId, Long.valueOf(modifyMenuDTO.getId()));

        if(Objects.nonNull(modifyMenuDTO.getName())&&!modifyMenuDTO.getName().isEmpty()){
            lqw.set(SysMenu::getName, modifyMenuDTO.getName());
        }

        if(Objects.nonNull(modifyMenuDTO.getUrl())&&!modifyMenuDTO.getUrl().isEmpty()){
            lqw.set(SysMenu::getUrl, modifyMenuDTO.getUrl());
        }

        if(Objects.nonNull(modifyMenuDTO.getSort())){
            lqw.set(SysMenu::getSort, modifyMenuDTO.getSort());
        }
        boolean update = update(lqw);
        if(update){
            // 关联 管理员类型
            return ResultUtil.successMap(null,"修改成功");
        }
        return ResultUtil.failMap("修改失败");
    }

    @Override
    public Map<String, Object> addMenu(AddMenuDTO addMenuDTO) {

        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();

        Date date = new Date();
        SysMenu sysMenu = new SysMenu();
        sysMenu.setName(addMenuDTO.getName());
        sysMenu.setUrl(addMenuDTO.getUrl());
        sysMenu.setIsDelete(0);
        sysMenu.setSort(addMenuDTO.getSort());
        sysMenu.setCreateTime(date);
        sysMenu.setCreateBy(principal.getAdmin().getId());
        sysMenu.setUpdateTime(date);
        sysMenu.setUpdateBy(principal.getAdmin().getId());

        boolean save = save(sysMenu);
        if(save){
            // 关联 管理员类型
            if(!addMenuDTO.getAdminTypes().isEmpty()){
                List<RoleMenu> list=new ArrayList<>();

                for (String adminType : addMenuDTO.getAdminTypes()) {
                    RoleMenu roleMenu = new RoleMenu();
                    roleMenu.setMenuId(sysMenu.getId());
                    Long roleId = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getAdminType, adminType)).getId();
                    roleMenu.setRoleId(roleId);
                    roleMenu.setIsValid(1);
                    list.add(roleMenu);
                }
                boolean saveBatch = roleMenuService.saveBatch(list);
                if(saveBatch){
                    return ResultUtil.successMap(null,"新增成功");
                }else{
                    log.error("新增表单失败：关联管理员类型失败");
                    throw new RuntimeException("新增失败");
                }
            }

            return ResultUtil.successMap(null,"新增成功");
        }else{
            log.error("新增表单失败: 新增菜单失败");
            throw new RuntimeException("新增失败");
        }

    }

    @Override
    public Map<String, Object> addSonMenu(AddMenuDTO addMenuDTO) {

        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();

        Date date = new Date();
        SysMenu sysMenu = new SysMenu();
        sysMenu.setName(addMenuDTO.getName());
        sysMenu.setUrl(addMenuDTO.getUrl());
        sysMenu.setIsDelete(0);
        sysMenu.setSort(addMenuDTO.getSort());
        sysMenu.setCreateTime(date);
        sysMenu.setCreateBy(principal.getAdmin().getId());
        sysMenu.setUpdateTime(date);
        sysMenu.setUpdateBy(principal.getAdmin().getId());
        sysMenu.setParentId(Long.parseLong(addMenuDTO.getParentId()));

        boolean save = save(sysMenu);
        if(save){
            // 新增子菜单不需要 关联管理员类型
            return ResultUtil.successMap(null,"新增成功");

        }else{
            log.error("新增表单失败: 新增菜单失败");
            throw new RuntimeException("新增失败");
        }
    }

    private List<MenuDTO> getChildrenMenuDTO(Long parentId){

        return  list(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, parentId))
                .stream().map(i -> MenuDTO.builder()
                        .id(String.valueOf(i.getId()))
                        .menuName(i.getName())
                        .menuUrl(i.getUrl())
                        .parentId(String.valueOf(i.getParentId()))
                        .sort(i.getSort())
                        .children(getChildrenMenuDTO(i.getId()))  // 递归调用获取 以当前id为父id的项
                        .build()).toList();


    }

    private List<MenuAdminDTO> getChildrenMenuAdminDTO(Long parentId){
        return  list(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, parentId))
                .stream().map(i->MenuAdminDTO.builder()
                        .id(String.valueOf(i.getId()))
                        .name(i.getName())
                        .url(i.getUrl())
                        .createTime(i.getCreateTime())
                        .createBy(adminMapper.selectOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getId,i.getCreateBy())).getName())
                        .updateTime(i.getUpdateTime())
                        .updateBy(adminMapper.selectOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getId,i.getUpdateBy())).getName())
                        .sort(i.getSort())
                        .children(getChildrenMenuAdminDTO(i.getId()))
                        .build()).toList();
    }

}
