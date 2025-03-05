package org.oyyj.aichatdemo.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.oyyj.aichatdemo.dto.UploadEmbeddingsResponseDTO;
import org.oyyj.aichatdemo.pojo.AIFile;
import org.oyyj.aichatdemo.service.IAIFileService;
import org.oyyj.aichatdemo.utils.AnyThingLLMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("/llm")
public class LlmChatController {

    private static final Logger log = LoggerFactory.getLogger(LlmChatController.class);

    @Autowired
    private IAIFileService aiFileService;

    @GetMapping("/chat")
    public String getLlmChat(String message,String slug){
        AnyThingLLMUtil anyThingLLMUtil = new AnyThingLLMUtil(new RestTemplateBuilder(), aiFileService);

        String s = anyThingLLMUtil.anyThingLLM(message, slug);
        return s;
    }

    @GetMapping("/streamChat")
    public SseEmitter getStreamLlmChat(String message,String slug){
        AnyThingLLMUtil anyThingLLMUtil = new AnyThingLLMUtil(new RestTemplateBuilder(),aiFileService);
        return anyThingLLMUtil.anyThingLLMWithStream(message,slug);
    }

    @PostMapping("/testUploadFile")
    public String testUploadFile(MultipartFile file){
        AnyThingLLMUtil anyThingLLMUtil = new AnyThingLLMUtil(new RestTemplateBuilder(),aiFileService);
        Mono<String> stringMono = anyThingLLMUtil.uploadToAnythingLLMWithStream(file);
        return stringMono.block(); // 阻塞获取对应的值
    }

    @PostMapping("/uploadFileToWorkShape")
    public Boolean uploadFileToWorkShape(@RequestParam("file") MultipartFile file,
                                         @RequestParam("slug") String slug ,
                                         @RequestParam("aiFileJson") String aiFileJson,
                                         HttpServletRequest request){

        if(!Objects.equals(request.getHeader("source"), "USERSERVICE")){
            log.error("请求来源不正确");
            return false;
        }
        AIFile aiFile = new AIFile();

        // 将json字符串 转换成 AIFile文件
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            aiFile=objectMapper.readValue(aiFileJson,AIFile.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        AnyThingLLMUtil anyThingLLMUtil=new AnyThingLLMUtil(new RestTemplateBuilder(),aiFileService);
        Mono<UploadEmbeddingsResponseDTO> uploadEmbeddingsResponseDTOMono = anyThingLLMUtil.addAnythingLLMWithRestTempLate(file, slug,aiFile);
        UploadEmbeddingsResponseDTO block = uploadEmbeddingsResponseDTOMono.block();
        Long id = null;
        if (block != null) {
            id = block.getWorkspace().getId();
        }else{
            return false;
        }
        if(!String.valueOf(id).isEmpty()){
            return true;
        }
        return false;
    }


}
