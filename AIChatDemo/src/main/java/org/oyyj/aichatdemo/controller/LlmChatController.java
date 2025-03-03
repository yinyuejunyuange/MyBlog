package org.oyyj.aichatdemo.controller;


import org.oyyj.aichatdemo.dto.UploadEmbeddingsResponseDTO;
import org.oyyj.aichatdemo.utils.AnyThingLLMUtil;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/llm")
public class LlmChatController {

    @GetMapping("/chat")
    public String getLlmChat(String message,String slug){
        AnyThingLLMUtil anyThingLLMUtil = new AnyThingLLMUtil(new RestTemplateBuilder());

        String s = anyThingLLMUtil.anyThingLLM(message, slug);
        return s;
    }

    @GetMapping("/streamChat")
    public SseEmitter getStreamLlmChat(String message,String slug){
        AnyThingLLMUtil anyThingLLMUtil = new AnyThingLLMUtil(new RestTemplateBuilder());
        return anyThingLLMUtil.anyThingLLMWithStream(message,slug);
    }

    @PostMapping("/testUploadFile")
    public String testUploadFile(MultipartFile file){
        AnyThingLLMUtil anyThingLLMUtil = new AnyThingLLMUtil(new RestTemplateBuilder());
        Mono<String> stringMono = anyThingLLMUtil.uploadToAnythingLLMWithStream(file);
        return stringMono.block(); // 阻塞获取对应的值
    }

    @PostMapping("/uploadFileToWorkShape")
    public Boolean uploadFileToWorkShape(MultipartFile file,String slug){
        AnyThingLLMUtil anyThingLLMUtil=new AnyThingLLMUtil(new RestTemplateBuilder());
        Mono<UploadEmbeddingsResponseDTO> uploadEmbeddingsResponseDTOMono = anyThingLLMUtil.addAnythingLLMWithRestTempLate(file, slug);
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
