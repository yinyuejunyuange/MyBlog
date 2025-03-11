package org.oyyj.adminservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.adminservice.mapper.PermissionMapper;
import org.oyyj.adminservice.pojo.Permission;
import org.oyyj.adminservice.service.IPermissionService;
import org.springframework.stereotype.Service;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {
}
