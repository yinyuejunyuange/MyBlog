package org.oyyj.adminservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.oyyj.adminservice.dto.MenuDTO;
import org.oyyj.adminservice.pojo.Admin;
import org.oyyj.adminservice.pojo.JWTAdmin;

import javax.security.sasl.AuthenticationException;
import java.util.List;

public interface IAdminService extends IService<Admin> {
    JWTAdmin login(String phone, String password,String encode,String uuid) throws AuthenticationException, JsonProcessingException;

    List<MenuDTO> getMenuDTO() throws AuthenticationException;
}
