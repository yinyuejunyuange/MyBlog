package org.oyyj.gatewaydemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.oyyj.gatewaydemo.pojo.User;
import org.oyyj.gatewaydemo.pojo.dto.LoginDTO;
import org.oyyj.gatewaydemo.pojo.dto.PasswordDTO;
import org.oyyj.gatewaydemo.pojo.dto.RegisterDTO;
import org.oyyj.gatewaydemo.pojo.vo.JWTUserVO;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;


import java.io.IOException;
import java.util.Map;


public interface IUserService extends IService<User> {

    Mono<JWTUserVO> login(String username, String password) throws JsonProcessingException;

    Mono<JWTUserVO> loginAdmin(String username, String password) throws JsonProcessingException;

    // 登出
    Mono<Void> LoginOut();

    Mono<JWTUserVO> registerUser(RegisterDTO registerDTO) throws IOException;

    Mono<ResultUtil<String>> registerAdmin(LoginDTO registerDTO , ServerHttpRequest request);

    /**
     * 用户修改密码
     * @param passwordDTO
     * @param request
     * @return
     */
    Mono<ResultUtil<String>> updatePassword(PasswordDTO passwordDTO, ServerHttpRequest request);

}
