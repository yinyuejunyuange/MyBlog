package org.oyyj.aichatdemo.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.oyyj.aichatdemo.dto.*;
import org.oyyj.aichatdemo.pojo.AIFile;
import org.oyyj.aichatdemo.service.IAIFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AnyThingLLMUtil {
    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(AnyThingLLMUtil.class);


    private final IAIFileService aiFileService;

    @Autowired
    public AnyThingLLMUtil(RestTemplateBuilder restTemplateBuilder,IAIFileService aiFileService) {
        this.restTemplate = restTemplateBuilder.build();
        this.aiFileService = aiFileService; // 通过构造函数注入

    }

    public  String anyThingLLM(String message,String slug) {

        String url="http://localhost:3001/api/v1/workspace/"+slug+"/chat";



        ObjectMapper objectMapper = new ObjectMapper();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpHeaders.set("Authorization", "Bearer 15TBMPR-3Z14J61-NGZ0NQ0-66ZNKA1"); // api密钥
        ChatDTO chatDTO = ChatDTO.builder()
                .message(message)
                .mode("query")
                .build();



        try {
            String s = objectMapper.writeValueAsString(chatDTO);
            HttpEntity<String> stringHttpEntity = new HttpEntity<>(s, httpHeaders);
            ResponseEntity<String> response = restTemplate.postForEntity(url, stringHttpEntity, String.class);


            String result=response.getBody();
            System.out.println("响应码"+response.getStatusCode());

            ResponseDTO responseDTO = objectMapper.readValue(result, ResponseDTO.class);
            return responseDTO.getTextResponse();

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }


    // 使用webClient替代 RestTempLate
    public SseEmitter anyThingLLMWithStream(String message, String slug) {

        ExecutorService executorService = Executors.newCachedThreadPool();

        SseEmitter sseEmitter = new SseEmitter(360_000L); // 超时时间60 秒  时间超过后 默认是已经完成
        String url="http://localhost:3001/api/v1/workspace/"+slug+"/stream-chat";

        ObjectMapper objectMapper = new ObjectMapper();

        WebClient webClient=WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization","Bearer 15TBMPR-3Z14J61-NGZ0NQ0-66ZNKA1")
                .build();

        ChatDTO chatDTO=ChatDTO.builder()
                .message(message)
                .mode("query")
                .build();

        executorService.submit(()->{
            try {
                webClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(chatDTO)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .doOnNext(line -> {
                            if(!line.isEmpty()){
                                try {
                                    StreamResponseDTO streamResponseDTO = objectMapper.readValue(line, StreamResponseDTO.class);
                                    String textResponse = streamResponseDTO.getTextResponse();
                                    if(textResponse==null){
                                        textResponse=" ";
                                    }
                                    sseEmitter.send(SseEmitter.event()
                                            .data(textResponse)
                                            .id(streamResponseDTO.getUuid())
                                            .build());
                                } catch (IOException e) {
                                    sseEmitter.completeWithError(e);
                                }
                            }
                        })
                        .doOnComplete(sseEmitter::complete)
                        .doOnError(sseEmitter::completeWithError)
                        .subscribe();
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
        });
        executorService.shutdown();
        return sseEmitter;
    }

    // 上传知识库到工作区
    public Mono<UploadEmbeddingsResponseDTO> addAnythingLLM(MultipartFile file, String slug ){


        ObjectMapper objectMapper = new ObjectMapper();
        String url="http://localhost:3001/api/v1/workspace/"+slug+"/update-embeddings";

        return uploadToAnythingLLMWithStream(file)
                .flatMap(block->{
                    UpdateEmbeddingsDTO updateEmbeddingsDTO = new UpdateEmbeddingsDTO();
                    updateEmbeddingsDTO.setAdds(Collections.singletonList(block));

                    // 创建webClient
                    WebClient webClient= WebClient.builder()
                            .baseUrl(url)
                            .defaultHeader("Authorization","Bearer 15TBMPR-3Z14J61-NGZ0NQ0-66ZNKA1")
                            .build();

                    return webClient.post()
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .bodyValue(updateEmbeddingsDTO)
                            .retrieve()
                            .bodyToMono(String.class)
                            .flatMap(response->{
                                try {
                                    UploadEmbeddingsResponseDTO uploadEmbeddingsResponseDTO = objectMapper.readValue(response, UploadEmbeddingsResponseDTO.class);
                                    logger.info("文件添加工作区成功：{}",uploadEmbeddingsResponseDTO);
                                    return Mono.just(uploadEmbeddingsResponseDTO);
                                } catch (JsonProcessingException e) {
                                    logger.error("文件解析失败：{}",e.getMessage());
                                    return Mono.error(e);
                                }
                            });
                })
                .onErrorResume(e->{
                    logger.error("添加工作区错误{}",e.getMessage());
                    return Mono.error(e);
                });

    }

    @Transactional
    public Mono<UploadEmbeddingsResponseDTO> addAnythingLLMWithRestTempLate(MultipartFile file, String slug, AIFile aiFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        String url = "http://localhost:3001/api/v1/workspace/" + slug + "/update-embeddings";

        // 将aiFile 的信息 存储到数据库中
        boolean save = aiFileService.save(aiFile);
        if(!save){
            logger.error("文件上传失败");
            return Mono.error(new RuntimeException("文件上传失败"));
        }
        return uploadToAnythingLLMWithStream(file)
                .flatMap(fileLocation -> {
                    try {
                        // 创建 RestTemplate
                        RestTemplate restTemplate = new RestTemplate();

                        // 创建请求头
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.set("Authorization", "Bearer 15TBMPR-3Z14J61-NGZ0NQ0-66ZNKA1");
                        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

                        // 创建更新嵌入的DTO
                        UpdateEmbeddingsDTO updateEmbeddingsDTO = new UpdateEmbeddingsDTO();
                        updateEmbeddingsDTO.setAdds(Collections.singletonList(fileLocation)); //生成一个list集合 多一个或者少一个都不行
                        updateEmbeddingsDTO.setDeletes(Collections.emptyList());

                        // 创建请求实体
                        HttpEntity<UpdateEmbeddingsDTO> requestEntity =
                                new HttpEntity<>(updateEmbeddingsDTO, headers);

                        // 发送请求
                        ResponseEntity<String> response = restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                requestEntity,
                                String.class
                        );

                        // 解析响应
                        if (response.getStatusCode().is2xxSuccessful()) {
                            UploadEmbeddingsResponseDTO uploadEmbeddingsResponseDTO =
                                    objectMapper.readValue(response.getBody(), UploadEmbeddingsResponseDTO.class);

                            aiFile.setIsUpload(1); // 修改 数据库设置文件上传成功
                            aiFile.setFileNameJson(fileLocation);
                            aiFileService.saveOrUpdate(aiFile);

                            logger.info("文件添加工作区成功：{}", uploadEmbeddingsResponseDTO);
                            return Mono.just(uploadEmbeddingsResponseDTO);
                        } else {
                            logger.error("添加工作区失败，状态码：{}", response.getStatusCode());
                            return Mono.error(new RuntimeException("添加工作区失败：" + response.getStatusCode()));
                        }
                    } catch (Exception e) {
                        logger.error("添加工作区错误", e);
                        return Mono.error(e);
                    }
                })
                .onErrorResume(e -> {
                    logger.error("添加工作区错误{}", e.getMessage());
                    return Mono.error(e);
                });
    }

    // 实现文件上传 并获取 json文件地址
    public Mono<String> uploadToAnythingLLMWithStream(MultipartFile file){

        ObjectMapper objectMapper = new ObjectMapper();

        WebClient webClient= WebClient.builder()
                .baseUrl("http://localhost:3001/api/v1/document/upload")
                .defaultHeader("Authorization","Bearer 15TBMPR-3Z14J61-NGZ0NQ0-66ZNKA1")
                .build();

        if(file==null||file.isEmpty()){
            logger.error("上传文件为空");
            return Mono.error(new IllegalArgumentException("文件不能为空"));
        }

//        // 如果确实需要额外的文件备份，可以这样安全地处理
//        try {
//            // 确保目标目录存在
//            Path targetDir = Paths.get("H:\\10516\\Test");
//            Files.createDirectories(targetDir);
//
//            // 使用原始文件名，避免硬编码
//            Path targetFile = targetDir.resolve(file.getOriginalFilename());
//
//            // 安全地复制文件
//            try (InputStream inputStream = file.getInputStream()) {
//                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
//                logger.info("文件备份成功：" + targetFile);
//            }
//        } catch (IOException e) {
//            logger.warn("文件备份失败", e);
//            // 这里不要抛出异常，只记录警告
//        }

        try {
            // 将MultipartFile转换成临时文件
            Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile.toFile());// 是将上传的文件内容从 MultipartFile 保存到指定的文件系统位置。

            FileSystemResource resource=new FileSystemResource(tempFile.toFile());


            return webClient.post()
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    //.bodyValue(BodyInserters.fromMultipartData("file",resource))
                    .body(BodyInserters.fromMultipartData("file", new FileSystemResource(tempFile.toFile())))
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response ->{
                        try {
                            logger.info("文件上传成功："+response);
                            UploadResponseDTO uploadResponseDTO = objectMapper.readValue(response, UploadResponseDTO.class);
                            String location=uploadResponseDTO.getDocuments().getFirst().getLocation();

                            // 删除临时文件
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException e) {
                                logger.warn("删除临时文件失败",e);
                                throw new RuntimeException(e);
                            }
                            return location;
                        } catch (JsonProcessingException e) {
                            logger.error("解释响应失败",e);
                            throw new RuntimeException(e);
                        }
                    })
                    .onErrorResume(error->{
                        logger.error("文件上传失败",error);
                        return Mono.error(new RuntimeException("文件上传失败",error));
                    });
        } catch (IOException e) {
            logger.error("文件处理失败",e);
            return Mono.error(new RuntimeException("文件处理失败",e));
        }

    }

//    // 使用webClient替代 RestTempLate
//    public  SseEmitter anyThingLLMWithStream(String message,String slug) {
//
//        SseEmitter sseEmitter = new SseEmitter(360_000L); // 超时时间60 秒
//        String url="http://localhost:3001/api/v1/workspace/"+slug+"/stream-chat";
//
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        WebClient webClient=WebClient.builder()
//                .baseUrl(url)
//                .defaultHeader("Authorization","Bearer 15TBMPR-3Z14J61-NGZ0NQ0-66ZNKA1")
//                .build();
//
//        ChatDTO chatDTO=ChatDTO.builder()
//                .message(message)
//                .mode("query")
//                .build();
//
//
//        // 使用 Flux 处理异步流
//        Flux<String> responseFlux=webClient.post()
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.TEXT_EVENT_STREAM)
//                .bodyValue(chatDTO)
//                .retrieve()
//                .bodyToFlux(String.class);
//
//        // 取消订阅钩子
//        Disposable disposable= responseFlux
//                        .doOnNext(line -> {
//                            if(!line.isEmpty()){
//                                try {
//                                    StreamResponseDTO streamResponseDTO = objectMapper.readValue(line, StreamResponseDTO.class);
//                                    String textResponse = streamResponseDTO.getTextResponse();
//                                    if(textResponse==null){
//                                        textResponse=" ";
//                                    }
//                                    // 使用 synchronized 避免并发问题
//                                    synchronized (sseEmitter) {
//                                        sseEmitter.send(SseEmitter.event()
//                                                .data(textResponse)
//                                                .id(streamResponseDTO.getUuid())
//                                                .build());
//                                    }
//                                } catch (IOException e) {
//                                    sseEmitter.completeWithError(e);
//                                }
//                            }
//                        })
//                        .doOnComplete(()->{
//                            try {
//                                sseEmitter.complete();
//                            } catch (Exception e) {
//                                System.out.println("忽略 已经完成的异常");
//                            }
//                        })
//                        .doOnError(e->{
//                            try {
//                                sseEmitter.completeWithError(e);
//                            } catch (Exception ex) {
//                                System.out.println("忽略 已经完成的异常");
//                            }
//                        })
//                        .subscribe();
//
//        // 添加完成和错误的监听
//        sseEmitter.onCompletion(disposable::dispose);
//        sseEmitter.onError(e->disposable.dispose());
//
//        return sseEmitter;
//    }

}
