package org.oyyj.blogservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.config.ApiCacheManager;
import org.oyyj.blogservice.config.RabbitMqConfig;
import org.oyyj.blogservice.config.Service.IApiConfigService;
import org.oyyj.blogservice.config.pojo.ApiConfig;
import org.oyyj.blogservice.config.pojo.EnhancedCorrelationData;
import org.oyyj.blogservice.config.pojo.RabbitMqMessage;
import org.oyyj.blogservice.dto.VIPMapVO;
import org.oyyj.blogservice.util.ResultUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/vipCache")
public class VIPCatchController {

    @Autowired
    private IApiConfigService apiConfigService;

    @Autowired
    private ApiCacheManager apiCacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String VIP_START_TIME = "vip:startTime";
    private static final String VIP_END_TIME = "vip:endTime";
    private static final String VIP_IS_ACTIVE = "vip:isActive";
    private static final String VIP_NUM = "vip:num";

    // 库存信息 缓存值
    private static final String VIP_PRIORITY = "vip:priority";
    // 用户抢夺成功记录
    private static final String VIP_CATCH_SET = "vip:catch:set";
    private static final String VIP_LOAD_KEY = "vip:load:key";


    @Autowired
    private RedissonClient redissonClient;

    /**
     * 增加或者修改一个键
     *
     * @param vipMapVO
     * @return
     * @throws InterruptedException
     */
    @PutMapping("/addOrUpdate")
    public Map<String, Object> addOrUpdate(@RequestBody VIPMapVO vipMapVO) throws InterruptedException {
        ApiConfig one = apiConfigService.getOne(Wrappers.<ApiConfig>lambdaQuery()
                .eq(ApiConfig::getName, vipMapVO.getConfigName())
        );
        if (Objects.isNull(one)) {
            apiConfigService.save(ApiConfig.builder()
                    .name(vipMapVO.getConfigName())
                    .value(String.valueOf(vipMapVO.getValue()))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );
            return ResultUtil.successMap(null, "操作成功");
        } else {
            apiCacheManager.put(vipMapVO.getConfigName(), String.valueOf(vipMapVO.getValue()));
            return ResultUtil.successMap(null, "操作成功");
        }
    }

    /**
     * 获取VIP资格  实现逻辑 初始化库存到redis中  使用预删除缓存 的策略  先删除缓存 在操作数据库
     *
     * @return
     */
    @PutMapping("/VIPCacheInfo")
    public Map<String, Object> VIPCacheInfo(@RequestParam("userId") Long userId) throws InterruptedException {
        // 获取开始时间  结束时间 以及是否开启
//        String startTime = apiCacheManager.get(VIP_START_TIME);
//        String endTime = apiCacheManager.get(VIP_END_TIME);
        String isActive = apiCacheManager.get(VIP_IS_ACTIVE);

        if ("1".equals(isActive) && !isUserRepeat(userId)) {
            // 获取库存数量
            reloadVIPCache();
            Long decrement = redisTemplate.opsForValue().decrement(VIP_PRIORITY);
            if(decrement ==null|| decrement < 0){
                return ResultUtil.successMap(null, "名额被抢完了 下次在继续吧");
            }
            redisTemplate.opsForSet().add(VIP_CATCH_SET, userId);
            // 成功获取库存  通过mq 实现异步的数据库扣减一个 关联表加上关联
            sendDelVipNum(userId);
            return ResultUtil.successMap(null, "恭喜你成功获取资格");
        } else {
            return ResultUtil.successMap(null, "活动已经结束");
        }

    }

    private void reloadVIPCache() throws InterruptedException {
        if (!redisTemplate.hasKey(VIP_PRIORITY)) {
            // 还没有被加载 加载库存
            RLock lock = redissonClient.getLock(VIP_LOAD_KEY);

            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    if(!redisTemplate.hasKey(VIP_PRIORITY)){
                        String vipNum = apiCacheManager.get(VIP_NUM);
                        int vipNumber = Integer.parseInt(vipNum);
                        // 初始化库存
                        redisTemplate.opsForValue().set(VIP_PRIORITY, vipNumber);
                        return ;
                    }else{
                        redisTemplate.opsForValue().get(VIP_PRIORITY);
                    }
                } catch (NumberFormatException e) {
                    throw new RuntimeException(e);
                }finally {
                    lock.unlock();
                }
            }else{
                for (int i = 0; i < 3; i++) {
                    if (lock.tryLock(1, TimeUnit.SECONDS)) {
                        try {
                            if(!redisTemplate.hasKey(VIP_PRIORITY)){
                                String vipNum = apiCacheManager.get(VIP_NUM);
                                int vipNumber = Integer.parseInt(vipNum);
                                // 初始化库存
                                redisTemplate.opsForValue().set(VIP_PRIORITY, vipNumber);
                                return;
                            }else{
                                redisTemplate.opsForValue().get(VIP_PRIORITY);
                            }

                        } catch (NumberFormatException e) {
                            throw new RuntimeException(e);
                        }finally {
                            lock.unlock();
                        }
                    }
                }
                throw new RuntimeException("无法加锁");
            }
        }
    }

    // 检查当前用户是否重复获取
    private boolean isUserRepeat(Long userId) throws InterruptedException {

        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(VIP_CATCH_SET, userId));// 不需要初始化

//        if(!redisTemplate.hasKey(VIP_CATCH_SET)){
//            // 初始化 集合
//            RLock lock = redissonClient.getLock("vip:cache:set:lock");
//            if(lock.tryLock(1,TimeUnit.SECONDS)){
//                try {
//                    if(redisTemplate.hasKey(VIP_CATCH_SET)){
//                        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(VIP_CATCH_SET, userId));
//                    }else{
//                        // 初始化 集合
//                        redisTemplate.opsForSet().add(VIP_CATCH_SET);
//                        return Boolean.FALSE;
//                    }
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }finally {
//                    lock.unlock();
//                }
//            }else{
//                for (int i = 0; i < 3; i++) {
//                    if (lock.tryLock(1, TimeUnit.SECONDS)) {
//                        try {
//                            if(redisTemplate.hasKey(VIP_CATCH_SET)){
//                                return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(VIP_CATCH_SET, userId));
//                            }else{
//                                // 初始化 集合
//                                redisTemplate.opsForSet().add(VIP_CATCH_SET);
//                                return Boolean.FALSE;
//                            }
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }finally {
//                            lock.unlock();
//                        }
//                    }
//
//                }
//            }
//        }else{
//
//        }
//        return false;
    }


    /**
     * 扣减库存 并绑定到用户
     */
    public void sendDelVipNum(Long userId){
        RabbitMqMessage message = new RabbitMqMessage(String.valueOf(userId),null);
        EnhancedCorrelationData enhancedCorrelationData = new EnhancedCorrelationData(UUID.randomUUID().toString(), message.toString());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.VIP_NUM_EXCHANGE,
                RabbitMqConfig.VIP_NUM_ROUTING_KEY,
                message,
                enhancedCorrelationData
        );
    }

    /**
     * 接受消息 扣除数据
     */
    @RabbitListener(queues = RabbitMqConfig.VIP_NUM_QUEUE)
    public void handleVipNum(RabbitMqMessage message,
                             Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {


        log.info("接受到消息队列中的数据");
        // 从Redis中获取id 查看是否已经处理过了
        Object o = redisTemplate.opsForValue().get("vip:num:"+message.getMessageId());
        if(o != null){
            // 已经处理完了
            if(channel.isOpen()){
                channel.basicAck(deliveryTag,false);
            }
            redisTemplate.opsForValue().set("vip:num:"+ message.getMessageId(), 1,1,TimeUnit.HOURS);// 延时
            return ;
        }

        boolean isSuccess = true;
        // 从redis中获取重试次数
        Object repeat = redisTemplate.opsForValue().get("vip:num:"+ message.getMessageId()+":repeat");
        if(repeat == null || (int)repeat<3){
            try {
                 apiConfigService.subVipNum(Long.parseLong(message.getKey()));
            } catch (Exception e) {
                log.error("处理失败："+e.getMessage());
                isSuccess = false;
            }finally {
                if(isSuccess){
                    if(channel.isOpen()){
                        channel.basicAck(deliveryTag,false); // 之确认当前信息 避免重复确认
                    }
                    redisTemplate.opsForValue().set("vip:num:"+ message.getMessageId(), 1,1,TimeUnit.HOURS);
                }else{
                    int repeatNum = (repeat == null)? 1 :(int)repeat+1;
                    redisTemplate.opsForValue().set("vip:num:"+ message.getMessageId()+":repeat", repeatNum,1,TimeUnit.HOURS);
                    channel.basicNack(deliveryTag,false,true);
                }
            }
        }else{
            // 重复三次不再放入队列 进入死信队列中
            EnhancedCorrelationData ed=new EnhancedCorrelationData(UUID.randomUUID().toString(),message.toString());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.DXL_INVALIDATION_EXCHANGE,
                    RabbitMqConfig.DXL_INVALIDATION_ROUTING_KEY,
                    message,
                    ed
            ); //  存储到死信队列中
            if (channel.isOpen()) {
                channel.basicAck(deliveryTag,false);
            }

        }
    }


    /**
     * 使用TTL+死信实现 定时任务
     */
    @GetMapping("/markTask")
    public Map<String, Object> markTask(){
        String string = UUID.randomUUID().toString();
        RabbitMqMessage message = new RabbitMqMessage(string,null);
        EnhancedCorrelationData ed = new EnhancedCorrelationData(string,string);
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.DELAY_EXCHANGE,
                RabbitMqConfig.DELAY_ROUTING_KEY,
                message,
                msg ->{
                    msg.getMessageProperties().setExpiration(String.valueOf(10000));
                    return msg;
                }, // 使用的是 消息的后置处理器 postProcessMessage   设置延时10秒
                ed
        );
        return ResultUtil.successMap(null,"发送成功");
    }


    @RabbitListener(queues = "#{rabbitMqConfig.getDXLInvalidationQueue()}")
    public void handleDLXInfo(RabbitMqMessage message ,
                              Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        System.out.println("成功收到延时消息 ："+message.toString());
        if(channel.isOpen()){
            channel.basicAck(deliveryTag,false);
        }

    }

}
