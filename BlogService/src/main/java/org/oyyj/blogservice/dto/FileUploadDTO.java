package org.oyyj.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadDTO {

    private String fileNo;

    private Long chunkNum;

    private String fileFullMd5;

    private MultipartFile file;

    private String md5;

    private Long allChunks;

}
