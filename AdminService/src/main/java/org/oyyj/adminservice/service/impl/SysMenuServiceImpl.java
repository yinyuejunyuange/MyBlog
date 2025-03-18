package org.oyyj.adminservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.adminservice.dto.MenuDTO;
import org.oyyj.adminservice.mapper.SysMenuMapper;
import org.oyyj.adminservice.pojo.LoginAdmin;
import org.oyyj.adminservice.pojo.RoleMenu;
import org.oyyj.adminservice.pojo.SysMenu;
import org.oyyj.adminservice.service.IRoleMenuService;
import org.oyyj.adminservice.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {


    @Autowired
    private IRoleMenuService roleMenuService;

    @Override
    public List<MenuDTO> adminMenu( Long roleId ) {

        // 通过角色id获取 菜单列表
        List<Long> list = roleMenuService.list(Wrappers.<RoleMenu>lambdaQuery().eq(RoleMenu::getRoleId, roleId))
                .stream().map(RoleMenu::getMenuId).toList();

        List<MenuDTO> result = new ArrayList<>();

        for (Long l : list) {
            SysMenu one = getOne(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getId, l));
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
}
