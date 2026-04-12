package org.oyyj.mycommon.config.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.http.HttpStatus; // 关键：导入Spring标准的HttpStatus
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody; // 关键：添加ResponseBody

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@ControllerAdvice
@ResponseBody // 核心：必须加，否则响应体为空
@Slf4j
public class GlobalExceptionHandle {

    /**
     * 数据库唯一键冲突异常处理
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResultUtil<?> handleSqlDuplicateException(SQLIntegrityConstraintViolationException e) {
        log.error("数据库唯一键冲突：", e);
        String msg = "数据重复";
        try {
            msg += "：" + e.getMessage().split("'")[1] + "（该内容已存在）";
        } catch (Exception ignored) {
        }
        return ResultUtil.fail(HttpStatus.BAD_REQUEST.value(), msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResultUtil<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("请求参数异常：{}", e.getMessage());
        return ResultUtil.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResultUtil<?> handleException(Exception e) {
        log.error("系统异常：", e);
        if (e instanceof ErrorResponse errorResponse) {
            int statusCode = errorResponse.getStatusCode().value();
            return ResultUtil.fail(statusCode >= 500 ? 500 : 400, e.getMessage());
        }
        return ResultUtil.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误");
    }

}