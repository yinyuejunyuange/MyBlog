package org.oyyj.adminservice.service.impl;

import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.adminservice.mapper.RoleMenuMapper;
import org.oyyj.adminservice.pojo.RoleMenu;
import org.oyyj.adminservice.service.IRoleMenuService;
import org.springframework.stereotype.Service;

@Service
public class RoleMenuServiceImpl extends MppServiceImpl<RoleMenuMapper, RoleMenu> implements IRoleMenuService {
}
