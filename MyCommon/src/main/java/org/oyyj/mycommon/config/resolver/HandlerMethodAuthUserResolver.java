package org.oyyj.mycommon.config.resolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.RequestHeadItems;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 自定义参数解析器 从请求头中获取 参数 组成数据
 */
@Component
public class HandlerMethodAuthUserResolver implements HandlerMethodArgumentResolver {


    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 检查参数是否含有注解
        return parameter.hasParameterAnnotation(RequestUser.class);
    }

    @Override
    @Nullable
    public Object resolveArgument(MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  @Nullable WebDataBinderFactory binderFactory) throws Exception {
        RequestUser parameterAnnotation = parameter.getParameterAnnotation(RequestUser.class);
        if (parameterAnnotation == null) {
            return null; // 再次校验
        }

        boolean required = parameterAnnotation.required();

        return tryGetAuthUser(webRequest, required);
    }

    /**
     * 尝试获取到参数
     * @param webRequest 可以从中获取到原生的HTTPServletRequest
     * @param required 是否必须
     * @return
     */
    private LoginUser tryGetAuthUser(NativeWebRequest webRequest , boolean required) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if(request == null){
            throw new RuntimeException("当前请求上下文不存在HttpServletRequest");
        }
        int isUserLogin = 1;
        // 获取需要的信息
        String userId = request.getHeader(RequestHeadItems.X_USER_ID);
        if(userId == null && required ){
            throw new RuntimeException("用户无权操作");
        }
        if(userId == null){
            isUserLogin = 0;
        }
        String userName = request.getHeader(RequestHeadItems.X_USER_NAME);
        String userPermission = request.getHeader(RequestHeadItems.X_USER_PERMISSION);
        String userRole = request.getHeader(RequestHeadItems.X_USER_ROLE);
        String ip = request.getHeader(RequestHeadItems.X_REAL_IP);


        List<String> permissions = null;
        List<String> roles = null;
        try {
            if(userPermission != null){
                permissions = mapper.readValue(userPermission, new TypeReference<List<String>>() {
                });
            }else{
                permissions = new ArrayList<>();
            }
            if(userRole != null){
                roles = mapper.readValue(userRole, new TypeReference<List<String>>() {
                });
            }else{
                roles = new ArrayList<>();
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        LoginUser authUser = new LoginUser(userId != null ? Long.parseLong(userId) : null,
                userName,
                null,
                permissions,
                roles,
                isUserLogin);
        authUser.setIp(ip);

        return authUser;
    }

}
