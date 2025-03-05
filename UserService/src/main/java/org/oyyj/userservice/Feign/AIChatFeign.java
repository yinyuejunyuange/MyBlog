package org.oyyj.userservice.Feign;

import org.oyyj.userservice.config.FeignAIChatConfig;
import org.oyyj.userservice.config.FeignFileConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(value = "AIChatDemoService",
        configuration = FeignAIChatConfig.class,
        fallbackFactory = FeignFileConfig.class)// 设置配置 实现每次访问是都设置好请求头
public interface AIChatFeign {

    // 上传文档
    @PostMapping(value = "/llm/uploadFileToWorkShape",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    void uploadFileToWorkShape(@RequestPart("file") MultipartFile file, @RequestParam("slug") String slug , @RequestParam("aiFileJson") String aiFileJson);

}
