package org.oyyj.mycommon.config.handle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 拦截 Spring Boot 默认的 /error 请求，将 404 等未进入 Controller 的异常统一为 ResultUtil 格式
 */
@RestController
public class CustomErrorController implements ErrorController {

    private static final String ERROR_PATH = "/error";

    @RequestMapping(ERROR_PATH)
    public ResultUtil<?> handleError(HttpServletRequest request, HttpServletResponse response) {
        // 强制 HTTP 状态码返回 200，错误码通过 JSON 体中的 code 传递给前端
        response.setStatus(HttpStatus.OK.value());

        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String message = (String) request.getAttribute("jakarta.servlet.error.message");

        if (statusCode == null) {
            return ResultUtil.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误");
        }


        if (message == null || message.isEmpty()) {
            message = statusCode >= 500 ? "服务器内部错误" : "请求处理失败";
        }

        return ResultUtil.fail(statusCode >= 500 ? 500 : 400, message);
    }
}