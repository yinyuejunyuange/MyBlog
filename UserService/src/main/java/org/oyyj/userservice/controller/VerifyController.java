package org.oyyj.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.oyyj.userservice.utils.RedisUtil;
import org.oyyj.userservice.utils.ResultUtil;
import org.oyyj.userservice.utils.VerifyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
//测试Controller
@RestController
@RequestMapping("/myBlog/user/verify")
public class VerifyController {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 生成验证码的接口
     *
     * @param response Response对象
     * @param request  Request对象
     * @throws Exception
     */
    @GetMapping("/getCode")
    public void getCode(HttpServletResponse response, HttpServletRequest request) throws Exception {

        // 利用图片工具生成图片
        // 返回的数组第一个参数是生成的验证码，第二个参数是生成的图片
        Object[] objs = VerifyUtil.newBuilder()
                .setWidth(120)   //设置图片的宽度
                .setHeight(35)   //设置图片的高度
                .setSize(6)      //设置字符的个数
                .setLines(5)    //设置干扰线的条数
                .setFontSize(25) //设置字体的大小
                .setTilt(true)   //设置是否需要倾斜
                .setBackgroundColor(Color.LIGHT_GRAY) //设置验证码的背景颜色
                .build()         //构建VerifyUtil项目
                .createImage();  //生成图片

        String token= UUID.randomUUID().toString(); // 生成一个临时token
        // 打印验证码
        System.out.println(objs[0]);

        // 设置redis值的序列化方式

        // 在redis中保存一个验证码最多尝试次数
        // 这里采用的是先预设一个上限次数，再以reidis decrement(递减)的方式来进行验证
        // 这样有个缺点，就是用户只申请验证码，不验证就走了的话，这里就会白白占用5分钟的空间，造成浪费了
        // 为了避免以上的缺点，也可以采用redis的increment（自增）方法，只有用户开始在做验证的时候设置值，
        //    超过多少次错误，就失效；避免空间浪费
//        redisTemplate.opsForValue().set(("VERIFY_CODE_" + id), "3", 5 * 60, TimeUnit.SECONDS);
        redisUtil.set(token, objs[0],60, TimeUnit.SECONDS); //保存 1分钟

        // 将token返回给前端
        response.setHeader("verifytoken", token);

        // 将图片输出给浏览器
        BufferedImage image = (BufferedImage) objs[1];
        response.setContentType("image/png");
        OutputStream os = response.getOutputStream();
        ImageIO.write(image, "png", os);
    }

    /**
     * 业务接口包含了验证码的验证
     *
     * @param code    前端传入的验证码
     * @param request Request对象
     * @return
     */
    @GetMapping("/checkCode")
    public Map<String,Object> checkCode(String code, HttpServletRequest request) {
        // 从请求中 获取 token
        String token = request.getHeader("verifyToken");

        String s = (String) redisUtil.get(token);

        if(s.isEmpty()){
            return ResultUtil.failMap("不合法请求");
        }

        // 校验验证码
        if (null == s || null == code || !s.equalsIgnoreCase(code)) {
            System.out.println("验证码错误!");
            return ResultUtil.failMap("验证码错误!");

        }

        // 验证通过之后手动将验证码失效
       redisUtil.delete(token);

        System.out.println("验证码正确!");
        return ResultUtil.successMap(null,"验证码正确");


    }
}
