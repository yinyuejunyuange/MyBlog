package org.oyyj.adminservice.service.impl;

import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.adminservice.mapper.AdminRoleMapper;
import org.oyyj.adminservice.pojo.AdminRole;
import org.oyyj.adminservice.service.IAdminRoleService;
import org.springframework.stereotype.Service;

@Service
public class AdminRoleServiceImpl extends MppServiceImpl<AdminRoleMapper, AdminRole> implements IAdminRoleService {
}
