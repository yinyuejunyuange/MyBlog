package org.oyyj.userservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Select;

import org.oyyj.userservice.DTO.AIFileDTO;
import org.oyyj.userservice.DTO.BlogDTO;
import org.oyyj.userservice.Feign.AIChatFeign;
import org.oyyj.userservice.service.IAsyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

@Service

public class AsyncServiceImpl implements IAsyncService {

    @Autowired
    private AIChatFeign aiChatFeign;

    @Override
    @Async("asyncTaskExecutor")
    public void upLoadBlogToAI(BlogDTO blogDTO) {
        // 将用户生成的文档转换成txt格式的文件存储下来并上传到ai中
        String filePath="H:/MyBlogFiles/";

        String fileName=blogDTO.getTitle().replaceAll(" ","_")+".txt";
        File file=new File(filePath,fileName); // 生成一个本地文件

        // 向文件中写数据
        try {
            FileWriter writer=new FileWriter(file);

            writer.write("博客标题\n"+blogDTO.getTitle()+"\n\n");
            writer.write("博客作者\n"+blogDTO.getUserName()+"\n\n");
            writer.write("博客简介\n"+blogDTO.getIntroduce()+"\n\n");
            writer.write("博客内容\n"+blogDTO.getText()+"\n\n");

            writer.flush(); // 让所有缓存全部存储到文件中

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 将File转换成Multipartfile
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            MockMultipartFile multiPartFile = new MockMultipartFile("file", bytes);

            AIFileDTO build = AIFileDTO.builder()
                    .blogId(Long.valueOf(blogDTO.getId()))
                    .fileAddress(filePath + fileName)
                    .isUpload(0)
                    .isDelete(0)
                    .build();

            ObjectMapper objectMapper = new ObjectMapper();
            String s = objectMapper.writeValueAsString(build);

            // 调用接口 上传文件到工作区
            aiChatFeign.uploadFileToWorkShape(multiPartFile,"myllm",s);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
