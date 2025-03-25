package org.oyyj.adminservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.oyyj.adminservice.dto.AddAdminDTO;
import org.oyyj.adminservice.dto.MenuDTO;
import org.oyyj.adminservice.pojo.Admin;
import org.oyyj.adminservice.pojo.JWTAdmin;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.security.sasl.AuthenticationException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IAdminService extends IService<Admin> {
    JWTAdmin login(String phone, String password,String encode,String uuid) throws AuthenticationException, JsonProcessingException;

    List<MenuDTO> getMenuDTO() throws AuthenticationException;

    Map<String,Object> updateAdmin(String adminId,Integer isFreeze);

    Map<String,Object> addAdmin(AddAdminDTO addAdminDTO) throws Exception;

    Map<String,Object> getManagers(String name,String phone,String adminType,String createBy,Date startTime,Date endTime,
                                          Integer isFreeze,Integer currentPage);

}
