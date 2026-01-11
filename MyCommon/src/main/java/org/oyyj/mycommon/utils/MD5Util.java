package org.oyyj.mycommon.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5哈希工具
 */
@Slf4j
public class MD5Util {

    /**
     * 对 File 进行MD5哈希
     */
    public static String generateMD5Code(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, bytesRead);
            }
            byte[] digest = md5.digest(); // 获取原始md5哈希
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        }catch (IOException e){
            log.error("文件获取输入流失败：{}",e.getMessage(),e);
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            log.error("获取文件MD5哈希失败：{}",e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

}
