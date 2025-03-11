package org.oyyj.adminservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.adminservice.mapper.AdminMapper;
import org.oyyj.adminservice.pojo.Admin;
import org.oyyj.adminservice.service.IAdminService;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {
}
