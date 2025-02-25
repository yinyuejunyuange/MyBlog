package org.oyyj.userservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.oyyj.userservice.mapper.SysRoleMapper;
import org.oyyj.userservice.pojo.SysRole;
import org.oyyj.userservice.service.ISysRoleService;
import org.springframework.stereotype.Service;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
}
