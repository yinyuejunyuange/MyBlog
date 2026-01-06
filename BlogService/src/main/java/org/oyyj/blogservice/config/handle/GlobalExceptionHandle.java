package org.oyyj.blogservice.config.handle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jodd.net.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 接受全局异常
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandle {

    @ExceptionHandler()
    public void handleRateLimitException(RuntimeException e, HttpServletResponse response) throws IOException {
        log.error(e.getMessage());
        response.setStatus(HttpStatus.error403().status());
        response.setContentType("application/json;charset=utf-8");
        response.setCharacterEncoding("utf-8");

        Map<String,Object> responseBody = new HashMap<>();
        responseBody.put("message",e.getMessage());
        responseBody.put("code",HttpStatus.error400().status());

        ObjectMapper mapper = new ObjectMapper();
        String s = mapper.writeValueAsString(responseBody);
        response.getWriter().write(s);
        response.getWriter().flush();
    }

}
