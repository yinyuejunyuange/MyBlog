package org.oyyj.mycommonbase.utils;

import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommonbase.common.auth.AESProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
public class AESUtil {

    // AES-CBC 固定配置
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES_NO_IV_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String AES_KEY_ALGORITHM = "AES";
    private static final int FULL_IV_LENGTH = 16; // 完整IV必须16字节

    @Autowired
    private AESProperties aesProperties;

    /**
     * 生成动态IV后缀（长度=16-固定前缀长度）
     * @return Base64编码的动态IV后缀
     */
    public String generateDynamicIvSuffix() {
        // 动态后缀长度 = 16 - 固定前缀长度
        int suffixLength = FULL_IV_LENGTH - aesProperties.getIvPrefixLength();
        byte[] suffix = new byte[suffixLength];
        new SecureRandom().nextBytes(suffix);
        return Base64.getEncoder().encodeToString(suffix);
    }

    /**
     * 拼接生成完整IV（YML固定前缀 + 传输动态后缀）
     * @param dynamicIvSuffix Base64编码的动态IV后缀
     * @return 完整IV的IvParameterSpec对象
     */
    private IvParameterSpec getFullIv(String dynamicIvSuffix) {
        // 1. 解码YML固定前缀
        byte[] prefixBytes = Base64.getDecoder().decode(aesProperties.getIvPrefix());
        // 校验固定前缀长度是否匹配配置
        if (prefixBytes.length != aesProperties.getIvPrefixLength()) {
            throw new IllegalArgumentException("YML配置的IV前缀长度与实际解码后不一致");
        }

        // 2. 解码传输的动态后缀
        byte[] suffixBytes = Base64.getDecoder().decode(dynamicIvSuffix);
        // 校验动态后缀长度
        int expectedSuffixLength = FULL_IV_LENGTH - aesProperties.getIvPrefixLength();
        if (suffixBytes.length != expectedSuffixLength) {
            throw new IllegalArgumentException("动态IV后缀长度错误，期望" + expectedSuffixLength + "字节，实际" + suffixBytes.length + "字节");
        }

        // 3. 拼接前缀+后缀得到完整16字节IV
        byte[] fullIv = new byte[FULL_IV_LENGTH];
        System.arraycopy(prefixBytes, 0, fullIv, 0, prefixBytes.length);
        System.arraycopy(suffixBytes, 0, fullIv, prefixBytes.length, suffixBytes.length);

        return new IvParameterSpec(fullIv);
    }

    /**
     * 加密：生成动态IV后缀 + 拼接完整IV + 加密
     * @param plainText 明文
     * @return 数组：[0]密文（Base64），[1]动态IV后缀（Base64，仅需传输这个）
     */
    public String[] encrypt(String plainText) {
        try {
            // 1. 生成动态IV后缀
            String dynamicIvSuffix = generateDynamicIvSuffix();
            // 2. 获取完整IV
            IvParameterSpec fullIv = getFullIv(dynamicIvSuffix);
            // 3. 解码AES密钥
            byte[] keyBytes = Base64.getDecoder().decode(aesProperties.getKey());
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);

            // 4. 加密
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, fullIv);
            byte[] encryptBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String cipherText = Base64.getEncoder().encodeToString(encryptBytes);

            // 返回「密文 + 动态IV后缀」（仅传输这两个）
            return new String[]{cipherText, dynamicIvSuffix};
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * 解密：接收密文+动态IV后缀 → 拼接完整IV → 解密
     * @param cipherText 密文（Base64）
     * @param dynamicIvSuffix 传输的动态IV后缀（Base64）
     * @return 明文
     */
    public String decrypt(String cipherText, String dynamicIvSuffix) {
        try {
            // 1. 获取完整IV（YML前缀+传输后缀）
            IvParameterSpec fullIv = getFullIv(dynamicIvSuffix);
            // 2. 解码AES密钥
            byte[] keyBytes = Base64.getDecoder().decode(aesProperties.getKey());
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);

            // 3. 解密
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, fullIv);
            byte[] decryptBytes = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decryptBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * 加密文件流 ASE IV来自与文件名等等
     */
    public InputStream encryptInputStream(InputStream inputStream,byte[] fileIv) {
        try {
            // 获取到配置中的base64
            byte[] keyBytes = Base64.getDecoder().decode(aesProperties.getKey());
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);
            // 加密
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(fileIv));
            return new CipherInputStream(inputStream, cipher);
        } catch (NoSuchAlgorithmException e) {
            log.error("加密算法不存在",e);
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            log.error("加密填充方式错误",e);
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            log.error("密钥错误",e);
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            log.error("IV初始化失败",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 解密 IV来自文件名
     * @param inputStream
     * @param fileIv
     * @return
     */
    public InputStream decryptInputStream(InputStream inputStream,byte[] fileIv) {

        try {
            byte[] keyBytes = Base64.getDecoder().decode(aesProperties.getKey());
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,secretKey,new IvParameterSpec(fileIv));
            return new CipherInputStream(inputStream, cipher);
        } catch (NoSuchAlgorithmException e) {
            log.error("解密算法不存在",e);
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            log.error("解密填充方式错误",e);
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            log.error("密钥错误",e);
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            log.error("IV初始化失败",e);
            throw new RuntimeException(e);
        }
    }


}
