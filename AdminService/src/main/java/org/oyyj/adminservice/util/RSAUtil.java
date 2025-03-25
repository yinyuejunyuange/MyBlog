package org.oyyj.adminservice.util;


import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

public class RSAUtil {

    public static String Encryption(String content,String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] decodeBase64 = Base64.decodeBase64(publicKey);
        PublicKey rsa = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodeBase64));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE,rsa);
        String outStr  = Base64.encodeBase64String(cipher.doFinal(content.getBytes()));
        return outStr;
    }

    public static String Decryption(String content, String privateKey)throws Exception{
        byte[] publicKeyByte = Base64.decodeBase64(content.getBytes("UTF-8"));
        byte[] privateKeyByte = Base64.decodeBase64(privateKey.getBytes("UTF-8"));
        RSAPrivateKey generatePublic = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyByte));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, generatePublic);
        return new String(cipher.doFinal(publicKeyByte));
    }

//    public static String Encryption(String content, String privateKey) throws Exception {
//        byte[] privateKeyBytes = Base64.decodeBase64(privateKey);
//        PrivateKey rsaPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
//        Cipher cipher = Cipher.getInstance("RSA");
//        cipher.init(Cipher.ENCRYPT_MODE, rsaPrivateKey);
//        byte[] encryptedBytes = cipher.doFinal(content.getBytes());
//        return Base64.encodeBase64String(encryptedBytes);
//    }
//
//    public static String Decryption(String content, String publicKey) throws Exception {
//        byte[] encryptedBytes = Base64.decodeBase64(content.getBytes("UTF-8"));
//        byte[] publicKeyBytes = Base64.decodeBase64(publicKey.getBytes("UTF-8"));
//        PublicKey rsaPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
//        Cipher cipher = Cipher.getInstance("RSA");
//        cipher.init(Cipher.DECRYPT_MODE, rsaPublicKey);
//        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
//        return new String(decryptedBytes);
//    }

    /**
     * 随机生成密钥对
     * return 对一个是 public 第二个 private
     * @throws NoSuchAlgorithmException
     */
    public static Map<String,String> genKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        //私钥
        PrivateKey privateK = keyPair.getPrivate();
        //公钥
        PublicKey publicK = keyPair.getPublic();

        //PrivateKey2String
        String privateKey = new String(Base64.encodeBase64(privateK.getEncoded()));
        //PublicKey2String
        String publicKey = new String(Base64.encodeBase64(publicK.getEncoded()));
        System.out.println("公钥："+publicKey);
        System.out.println("私钥："+privateKey);


        Map<String,String> map=new HashMap<>();
        map.put("public",publicKey);
        map.put("private",privateKey);
        return map;
    }

}
