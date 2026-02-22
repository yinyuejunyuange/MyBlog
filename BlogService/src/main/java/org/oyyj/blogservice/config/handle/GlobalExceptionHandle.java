package org.oyyj.blogservice.config.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus; // 关键：导入Spring标准的HttpStatus
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

    // 通用运行时异常处理
    @ExceptionHandler(RuntimeException.class)
    public void handleRateLimitException(RuntimeException e, HttpServletResponse response) {
        log.error("运行时异常：", e); // 打印完整栈，不要只打message
        writeJsonResponse(response, HttpStatus.FORBIDDEN.value(), e.getMessage());
    }

    // 数据库唯一键冲突异常处理
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public void handleSqlDuplicateException(SQLIntegrityConstraintViolationException e, HttpServletResponse response) {
        log.error("数据库唯一键冲突：", e);
        String msg = "数据重复：" + e.getMessage().split("'")[1] + "（该内容已存在）"; // 优化提示，只取重复的字段值
        writeJsonResponse(response, HttpStatus.BAD_REQUEST.value(), msg);
    }

    /**
     * 封装JSON响应写入逻辑，避免重复代码，统一处理异常
     */
    private void writeJsonResponse(HttpServletResponse response, int status, String message) {
        // 1. 重置响应（防止之前的响应头干扰）
        response.reset();
        // 2. 设置响应头
        response.setStatus(status);
        response.setContentType("application/json;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        // 3. 构建响应体
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", status);
        responseBody.put("message", message);

        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(writer, responseBody); // 直接写入，避免字符串转换出错
            writer.flush();
        } catch (IOException ex) {
            log.error("写入JSON响应失败：", ex);
        } finally {
            // 4. 确保流关闭，避免内存泄漏和响应截断
            if (writer != null) {
                writer.close();
            }
        }
    }
}