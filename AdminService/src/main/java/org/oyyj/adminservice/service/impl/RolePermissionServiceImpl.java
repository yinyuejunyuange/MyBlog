package org.oyyj.adminservice.service.impl;

import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.adminservice.mapper.RolePermissionMapper;
import org.oyyj.adminservice.pojo.RolePermission;
import org.oyyj.adminservice.service.IRolePermissionService;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionServiceImpl extends MppServiceImpl<RolePermissionMapper, RolePermission> implements IRolePermissionService {
}
