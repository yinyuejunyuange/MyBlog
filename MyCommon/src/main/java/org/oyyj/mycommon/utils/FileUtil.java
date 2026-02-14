package org.oyyj.mycommon.utils;

import cn.hutool.core.io.resource.UrlResource;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.util.FilteredObjectInputStream;
import org.oyyj.mycommon.config.MinioConfig;
import org.oyyj.mycommon.pojo.UploadMetadata;
import org.oyyj.mycommon.service.IUploadMetadataService;
import org.oyyj.mycommonbase.common.auth.AESProperties;
import org.oyyj.mycommonbase.utils.AESUtil;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 文件工具类
 */
@Component
@Slf4j
public class FileUtil {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private MinioConfig minioConfig;

    @Value("${minio.image-bucket-name}")
    private String imageBucketName;

    @Value("${minio.document-bucket-name}")
    private String documentBucketName;

    @Value("${minio.file-chunk-bucket-name}")
    private String fileChunkBucketName;

    @Value("${minio.image-bucket-name}")
    private String imageBucket;

    @Value("${minio.buckets}")
    private String buckets;

    @Value("${minio.temp-file-path}")
    private String tempFilePath;

    @Value("${minio.local-file-path}")
    private String localFilePath;

    @Autowired
    private AESProperties aesProperties;

    @Autowired
    private IUploadMetadataService uploadMetadataService;

    /**
     * 初始化创建所有表
     */
    @PostConstruct
    public void initBuckets(){
        List<String> bucketsList = Arrays.asList(buckets.split(","));
        for (String bucket : bucketsList) {
            bucket = bucket.trim();
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(bucket)
                                .build()
                );

                if(!exists){
                    minioClient.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(bucket)
                                    .build()
                    );
                    log.info("初始化bucket：{}",bucket);
                }else{
                    log.info("bucket:{} 已经创建",bucket);
                }
            } catch (Exception e) {
                log.error("bucket{} 检查或创建失败",bucket);
                throw new RuntimeException(e.getMessage());
            }
        }



    }


    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadChunk(String fileNo,
                                           Long chunkNum,
                                           String fileFullMd5,
                                           MultipartFile file,
                                           String md5,
                                           Long allChunks){
        // 检查是否重复上传
        Boolean exists = uploadMetadataService.checkChunkExists(fileNo, Math.toIntExact(chunkNum));
        if(exists){
            log.info("文件分片已存在 跳过上传 fileNo:{},chunkNum:{}",fileNo,chunkNum);
            return ResultUtil.successMap(chunkNum,"分片已存在");
        }

        if(file == null || file.isEmpty()){
            log.info("文件分片不可为空 跳过上传 fileNo:{},chunkNum:{}",fileNo,chunkNum);
            return ResultUtil.successMap(chunkNum,"分片不可为空");
        }

        // 检查文件md5是否相同
        try {
            String chunkMd5 = calculateFileMd5(file.getInputStream());
            if(!chunkMd5.equals(md5)){
                log.info("文件分片md5不正确 跳过上传 fileNo:{},chunkNum:{}",fileNo,chunkNum);
                return ResultUtil.successMap(chunkNum,"分片md5不正确");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (InputStream inputStream = file.getInputStream()) {
            // 存储
            String objectName = fileNo+"_"+chunkNum;
            UploadMetadata uploadMetadata = new UploadMetadata();
            uploadMetadata.setChunkNum(chunkNum);
            uploadMetadata.setFileNo(fileNo);
            uploadMetadata.setFileType(UploadMetadata.MetaDataEnum.CHUNK.getValue());
            uploadMetadata.setBucketName(fileChunkBucketName);
            uploadMetadata.setFileMd5(fileFullMd5);
            uploadMetadata.setChunkMd5(md5);
            uploadMetadata.setPartSize(file.getSize());
            uploadMetadata.setTotalFileChunks(allChunks);
            boolean save = uploadMetadataService.save(uploadMetadata);
            if(save){
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(fileChunkBucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }
        } catch (IOException e) {
            log.error("文件{} 分片{} 解析失败{}",fileNo,chunkNum,e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("文件{} 分片{} 文件上传失败{}",fileNo,chunkNum,e.getMessage());
            throw new RuntimeException(e);
        }
        return ResultUtil.successMap(chunkNum+1,"");
    }

    /**
     * 文件合并
     * @param fileNo 文件信息
     * @param totalFileChunks 分片总数
     * @param orgFileName 原始文件名称（包含文件名称）
     * @return 文件合并后的路径
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> mergeChunk(String fileNo, Long totalFileChunks,String orgFileName){

        // 验证是否缺失
        List<UploadMetadata> list = uploadMetadataService.list(Wrappers.<UploadMetadata>lambdaQuery()
                .eq(UploadMetadata::getFileNo, fileNo)
                .eq(UploadMetadata::getFileType, UploadMetadata.MetaDataEnum.CHUNK.getValue())
        );
        List<Long> chunkList = list.parallelStream().map(UploadMetadata::getChunkNum).toList();
        List<Long> missingNumbers = findMissingNumbers(totalFileChunks, chunkList);

        if(!missingNumbers.isEmpty()){
            return ResultUtil.successMap(missingNumbers,"存在缺失文件片");
        }

        if (totalFileChunks <= 0) {
            throw new IllegalArgumentException("分片总数必须大于 0");
        }
        File finalDirFile = new File(localFilePath);
        try {
            if (!finalDirFile.exists()) {
                Files.createDirectories(finalDirFile.toPath());
            }
        } catch (IOException e) {
            log.error("文件唯一标识：{}， 文件总片数：{} 目录生成错误：{}",fileNo,totalFileChunks,e.getMessage());
            throw new RuntimeException(e);
        }
        String tempFileName = fileNo + "_" + orgFileName;
        // 步骤5：流式追加合并本地分片为完整文件
        File finalFile = new File(finalDirFile, tempFileName);
        try (FileOutputStream finalFileOs = new FileOutputStream(finalFile, false);// 覆盖模式创建新文件
             BufferedOutputStream localBos = new BufferedOutputStream(finalFileOs)) {

            byte[] buffer = new byte[1024 * 1024]; // 1MB 缓冲区，平衡效率与内存占用
            for (int i = 0; i < totalFileChunks; i++) {
                String objectName = fileNo + "_" + i;
                GetObjectResponse fileResponse = null;
                try {
                    fileResponse = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(fileChunkBucketName)
                                    .object(objectName)
                                    .build()
                    );
                } catch (Exception e) {
                    log.error("文件唯一标识：{} 总片数{} 错误：{}", fileNo, totalFileChunks, e.getMessage());
                    throw new RuntimeException(e);
                }
                try (BufferedInputStream bis = new BufferedInputStream(fileResponse)) {
                    int readLen;
                    while ((readLen = bis.read(buffer)) != -1) {
                        localBos.write(buffer, 0, readLen);
                    }
                } catch (IOException e) {
                    log.error("文件唯一标识{} 报错{}", fileNo, e.getMessage());
                    throw new RuntimeException(e);
                }
            }
            localBos.flush(); // 刷新缓冲区，确保所有数据写入
        }catch (Exception e){
            log.error("文件唯一标识：{}， 文件总片数：{}， 错误：{}",fileNo,totalFileChunks,e.getMessage());
            return ResultUtil.failMap("文件合并错误");
        }


        String fileMd5 = list.getFirst().getFileMd5();
        if(!verifyFileIntegrity(finalFile,fileMd5)){
            log.error("文件唯一标识：{}， 文件总片数：{}， 文件MD5哈希验证失败  ",fileNo,totalFileChunks);
            // 清空 数据库以及minio存储的切片
            return ResultUtil.failMap("文件完整性验证失败错误");
        }

        try (FileInputStream fileInputStream = new FileInputStream(finalFile)) {

            UploadMetadata uploadMetadata = new UploadMetadata();
            uploadMetadata.setFileNo(fileNo);
            uploadMetadata.setFileType(UploadMetadata.MetaDataEnum.WHOLE.getValue());
            uploadMetadata.setBucketName(documentBucketName);
            uploadMetadata.setFileMd5(fileMd5);
            uploadMetadata.setPartSize(finalFile.length());
            uploadMetadata.setTotalFileChunks(totalFileChunks);
            boolean save = uploadMetadataService.save(uploadMetadata);
            if(save){
                // 存储到minio文件中
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(documentBucketName)
                                .stream(fileInputStream,finalFile.length(),-1)
                                .object(orgFileName)
                                .contentType(getFileType(tempFileName))
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("文件唯一标识：{}, 文件总片数 {}， 完整文件保存失败：{}",fileNo,totalFileChunks,e.getMessage());
        }
        try {
            // 步骤6：清理本地临时分片文件（可选，如需保留分片可注释）
            cleanLocalTempChunkFiles(finalDirFile,tempFileName);
        } catch (IOException e) {
            log.error("文件唯一标识：{}， 文件总片数：{}， 临时目录清理 错误：{}",fileNo,totalFileChunks,e.getMessage());
            throw new RuntimeException(e);
        }
        return ResultUtil.successMap(null,"文件上传成功");
    }

    /**
     * 查询上传了的切片信息
     * @param fileNo
     * @return
     */

    public Map<String,Object> listUploadChunks(String fileNo){
        List<Long> existsChunks = uploadMetadataService.getExistsChunks(fileNo);
        return ResultUtil.successMap(existsChunks,"查询成功");
    }


    /**
     * 计算1缺失的数据
     * @param n
     * @param list
     * @return
     */
    private List<Long> findMissingNumbers(long n, List<Long> list) {
        // 步骤1：创建 Hashset，存入两个列表的所有元素（自动去重，支持 O(1) 查询）
        Set<Long> existSet = new HashSet<>();
        // 存入 list1（忽略 null，避免空指针）
        if (list != null) {
            existSet.addAll(list);
        }

        // 步骤2：遍历 1~n，查询是否在集合中，不在即为缺失值
        List<Long> missingNumbers = new ArrayList<>();
        for (long i = 0; i < n; i++) {
            if (!existSet.contains(i)) {
                missingNumbers.add(i);
            }
        }


        // 步骤3：返回结果
        return missingNumbers;
    }

    /**
     * 清理本地临时分片目录
     * @param tempDirFile 临时分片目录
     * @throws IOException IO 异常
     */
    private void cleanLocalTempChunkFiles(File tempDirFile,String fileNo) throws IOException {
        if (tempDirFile.exists() && tempDirFile.isDirectory()) {
            File[] files = tempDirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    Files.deleteIfExists(file.toPath());
                }
            }
            Files.deleteIfExists(tempDirFile.toPath());
        }
        // todo MQ执行 清除 minio多余数据



        uploadMetadataService.remove(Wrappers.<UploadMetadata>lambdaQuery()
                .eq(UploadMetadata::getFileNo, fileNo)
                .eq(UploadMetadata::getFileType, UploadMetadata.MetaDataEnum.CHUNK.getValue())
        );

    }


    /**
     * 计算文件MD5
     */
    private String calculateFileMd5(InputStream is) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            byte[] digest = md.digest();
            return bytesToHex(digest);
        } finally {
            is.close();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private boolean verifyFileIntegrity(File file,String expectedMd5) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String md5 = calculateFileMd5(fileInputStream);
            log.info("前端传递md5:{},后端传递md5:{}",expectedMd5,md5);
            return expectedMd5.equals(md5);
        }catch (Exception e){
            log.error("文件验证失败",e);
            return false;
        }
    }

    private String getFileType(String fileName) {
        // 根据文件扩展名判断内容类型
        String extension = fileName.toLowerCase();

        if (extension.endsWith(".pdf")) {
            return "application/pdf";
        } else if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (extension.endsWith(".png")) {
            return "image/png";
        } else if (extension.endsWith(".doc")) {
            return "application/msword";
        } else if (extension.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (extension.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (extension.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (extension.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        } else if (extension.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (extension.endsWith(".txt")) {
            return "text/plain";
        } else if (extension.endsWith(".zip")) {
            return "application/zip";
        } else if (extension.endsWith(".rar")) {
            return "application/x-rar-compressed";
        } else {
            // 默认类型
            return "application/octet-stream";
        }


    }


    public void getHeadImgUrl(String objectName, HttpServletResponse response) {
        try {
            GetObjectResponse object = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(imageBucketName)
                            .object(objectName)
                            .build()
            );

            response.setContentType("image/jpeg");
            IOUtils.copy(object, response.getOutputStream());
        } catch (Exception e) {
            log.error("获取头像{}文件错误",objectName,e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 上传图片
     * @param multipartFile 文件信息
     * @param objectName 对象名称
     * @return
     */
    public Boolean uploadImage(MultipartFile multipartFile, String objectName) {
        try (InputStream inputStream = multipartFile.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(imageBucketName)
                            .object(objectName)
                            .stream(inputStream, multipartFile.getSize(),-1)
                            .contentType(multipartFile.getContentType())
                            .build()
            );
        } catch (IOException e) {
            log.error("文件输入流写入失败 文件名称{}",objectName,e);
            throw new RuntimeException(e);
        } catch (Exception e){
            log.error("文件上传失败 文件名称{}",objectName,e);
            throw new RuntimeException(e);
        }
        return true;
    }

}
