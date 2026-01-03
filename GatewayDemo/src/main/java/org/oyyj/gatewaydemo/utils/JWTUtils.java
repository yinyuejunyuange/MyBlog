package org.oyyj.gatewaydemo.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.gatewaydemo.pojo.auth.AuthUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;


import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
public class JWTUtils {
    /*@ApiModelProperty("盐")*/
    private static final String SALT_KEY="oyyjblogzlyhelppqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$"; // 64 的字节长度 满足 hs512算法
//    @ApiModelProperty("令牌有效期")
    private static final long TOKEN_VALIDITY= 60*60*24*7;
//    @ApiModelProperty("权限密钥")
    private static final String AUTHORIZATION_HEADER = "AUTH";
//    @ApiModelProperty("Base64 密钥 ")
private static final Key SECRET_KEY = Keys.hmacShaKeyFor(SALT_KEY.getBytes(StandardCharsets.UTF_8));
    /*
    * 2. Base64.getEncoder()
        通过调用Base64.getEncoder()，你获取一个Base64编码器，用于将数据编码为Base64格式。
        3. SALT_KEY.getBytes(StandardCharsets.UTF_8)
        SALT_KEY是一个字符串（假设它在类的其它地方定义）。
        getBytes(StandardCharsets.UTF_8)将SALT_KEY字符串转换为一个字节数组，使用UTF-8字符集进行编码。
        4. encodeToString(...)
        encodeToString(...)方法接受字节数组作为参数，并将其转换为一个Base64编码的字符串。
        最终结果是一个字符串，表示SALT_KEY内容的Base64编码形式。
    * */

    /*
    * 生成token
    * @Param userId 用户Id
    * @Param clientId 用于区别 客户端，移动端 网页端
    * @Param role 角色权限
    * */
    public static String createToken(AuthUser loginUser, String clientId  , String role) throws JsonProcessingException {

        ObjectMapper objectMapper=new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String s = objectMapper.writeValueAsString(loginUser);
        return Jwts.builder()
                .setSubject(String.valueOf(loginUser.getUserId())) // 代表JWT主体 即所有人 设置JWT的主体为用户ID，表示JWT的主要内容。
                .setIssuer("")// 代表JWT的签发主体 通常可以填入应用名称或标识。
                .setIssuedAt(new Date()) // 是一个时间戳 代表JWT的签发时间 设置JWT的签发时间为当前时间。
                .setAudience(clientId) // 代表JWT接收对象 设置JWT的接收者为clientId，用于区分不同的客户端。
                .claim("role",role)
                .claim("userInfo",s) //使用claim方法添加自定义字段到JWT中，包含用户的角色和用户ID。
               // .setExpiration(validity)// 签名: 使用HS512算法和SECRET_KEY对JWT进行签名，确保JWT的完整性和真实性。
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)//过期时间: 设置JWT的过期时间为之前计算的validity。 --更正 使用redis 方便刷新令牌
                .compact(); // 完成构建
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage()+":"+"无效token："+token);
        }
        return false;
    }

    /**
     * 解析token 获取 userId
     */
    public static AuthUser parseTokenAndGetUserId(String token) {
        try {
            // 解析 JWT token
            ObjectMapper objectMapper=new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);

            // 获取 JWT 中的主体信息，即 userId
            Claims claims = claimsJws.getBody();
            String userJson= (String) claims.get("userInfo");
            return objectMapper.readValue(userJson, AuthUser.class);
        } catch (Exception e) {
            // 解析失败，可能是签名无效、Token 过期等原因
            e.printStackTrace();
            return null;
        }
    }



}
