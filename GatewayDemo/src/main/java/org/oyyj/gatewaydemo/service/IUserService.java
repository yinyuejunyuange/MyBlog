package org.oyyj.gatewaydemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.oyyj.gatewaydemo.pojo.User;
import org.oyyj.gatewaydemo.pojo.dto.RegisterDTO;
import org.oyyj.gatewaydemo.pojo.vo.JWTUserVO;


import java.io.IOException;
import java.util.Map;


public interface IUserService extends IService<User> {

    JWTUserVO login(String username, String password) throws JsonProcessingException;

    // 登出
    void LoginOut();

    JWTUserVO registerUser(RegisterDTO registerDTO) throws IOException;

}
