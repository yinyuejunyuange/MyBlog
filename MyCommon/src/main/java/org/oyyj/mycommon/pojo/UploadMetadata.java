package org.oyyj.mycommon.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 断点续传定义类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadMetadata {
    // 文件唯一标识
    private String fileMd5;
    // MINIO 桶名
    private String bucketName;
    // MINIO 对象名
    private String objectName;
    // 分片上传 upload id
    private String uploadId;
    // 分片大小（字节 >= 5MB  MINIO 官方限制）
    private long partSize;
    // 文件总大小
    private long totalFileSize;
    // 已上传分片
    private Map<Integer,String> uploadedParts;
    // 8字节IV
    private String ivBase64;
    // 创建时间
    private long createTime;
}
