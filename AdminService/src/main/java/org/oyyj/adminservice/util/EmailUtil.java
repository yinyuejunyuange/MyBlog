package org.oyyj.adminservice.util;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class EmailUtil {

    @Autowired
    private RedisUtil redisUtil;

    @Resource
    private  JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private  String from; // 发件人


    public  String sendEncode(String email){
        // 创建 邮箱信息
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);

        // 设置收件人
        message.setTo(email);

        // 设置主题
        message.setSubject("书易验证码");

        // 随机生成一个六位验证码
        StringBuilder encodeBuilder = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            int number = random.nextInt(10);
            encodeBuilder.append(number);
        }

        // 生成36字符
        String uuid = UUID.randomUUID().toString();

        String encode = encodeBuilder.toString();


        // 将uuid和encode对应起来存入redis中 设置时间2分钟

        redisUtil.set(uuid,encode,2, TimeUnit.MINUTES);

        message.setText(encode);

        mailSender.send(message);

        return uuid;
    }
}
