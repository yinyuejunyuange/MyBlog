package org.oyyj.aichatdemo.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class SourceAuthenticationFilter extends OncePerRequestFilter {

    private final static String FROMWORD="FROM_GATEWAY";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            if(!FROMWORD.equals(request.getHeader("from"))&&request.getHeader("source").isEmpty()){
                // 请求既不是由gateway转发而来 又不是其他请求发送而来
                throw new RuntimeException("来源错误");
            }else{
                filterChain.doFilter(request, response);
            }
        } catch (RuntimeException e) {
            String message = e.getMessage();
            Map<String,Object> errors = new HashMap<>();
            errors.put("code",401);
            errors.put("msg",message);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            OutputStream out = response.getOutputStream();
            ObjectMapper objectMapper=new ObjectMapper();
            objectMapper.writeValue(out, errors);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

    }
}
