package org.oyyj.userservice.utils;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;

/**
 * MD5加密工具包
 */
public class Md5Util {

    // 盐
    private static String SALT="oyyj0715";

    private static String md5Hex(String src) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bs = md5.digest(src.getBytes());
            return new String(new Hex().encode(bs));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getMd5(String pwd, String username) {
        // 通过username 设置盐
        String userSalt=SALT+username+"@#528";
        //  加盐加密
        String pwdSalt=md5Hex(pwd+userSalt);
        // 将盐混入到密码当中
        char[] cs=new char[pwdSalt.length()];
        for(int i=0;i<pwdSalt.length();i++){
            if(i%4==0){
                cs[i]=userSalt.charAt(i%10);
            }else{
                cs[i]=pwdSalt.charAt(i);
            }
        }
        // System.out.println("长度："+pwdSalt.length());

        return new String(cs); // 不嫩使用cs.toString() 会返回哈希值
    }

    // 验证
    public static boolean verifySaltPwd(String pwd, String username,String password) {
        String md5 = getMd5(pwd, username);
        return md5.equals(password);
    }
}
