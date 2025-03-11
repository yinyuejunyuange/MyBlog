package org.oyyj.adminservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.adminservice.mapper.RoleMapper;
import org.oyyj.adminservice.pojo.Role;
import org.oyyj.adminservice.service.IRoleService;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {
}
