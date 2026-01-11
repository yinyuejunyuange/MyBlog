package org.oyyj.mycommon.utils;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.config.MinioConfig;
import org.oyyj.mycommonbase.common.auth.AESProperties;
import org.oyyj.mycommonbase.utils.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * minio 文件上传工具
 */
@Component
@Slf4j
public class MinioUtil {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private AESUtil  aesUtil;

    @Autowired
    private MinioConfig minioConfig;

    @Value("${minio.image-bucket-name}")
    private String imageBucketName;

    @Value("${minio.document-bucket-name}")
    private String documentBucketName;

    @Autowired
    private AESProperties aesProperties;

    /**
     * 图片相关桶 的功能
     */
    public String uploadImageBucket(MultipartFile file ,Long userId)  {

        // 1. 初始化8字节数组
        byte[] randomIvBytes = new byte[aesProperties.getIvPrefixLength()];

        // 2. 安全随机数填充数组
        new SecureRandom().nextBytes(randomIvBytes);

        String fileMd5 = MD5Util.generateMD5Code(file);

        String contentType = file.getContentType() == null ? "image/jpeg" : file.getContentType();
        long totalFileSize = file.getSize();
        try (InputStream inputStream = file.getInputStream()) {
            // 加密InputStream
            String objectName = userId+"_"+fileMd5+"_"+ Arrays.toString(randomIvBytes);
            InputStream encryptInputStream = aesUtil.encryptInputStream(inputStream, randomIvBytes);
            if(minioConfig.getMaxFileSize()*1024*1024 >= totalFileSize){
                // 不大于最高限制 直接全部上传
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(imageBucketName)
                                .object(objectName)
                                .stream(encryptInputStream, totalFileSize,-1)
                                .contentType(contentType)
                                .build()
                );
            }else{
                // 断续上传

            }

            // todo 将fileMd5存储到数据库中做记录
            return objectName;
        }catch (IOException e){
            log.error("文件获取IO流失败：{}",e.getMessage(),e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("minio文件上传失败：{}",e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

}
