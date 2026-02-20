package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.oyyj.blogservice.common.BlogEnum.PublishEnum;
import org.oyyj.blogservice.common.BlogEnum.TimedModeEnum;
import org.oyyj.blogservice.config.common.cf.ItemCF;
import org.oyyj.blogservice.config.common.cf.UserCF;
import org.oyyj.blogservice.config.mqConfig.sender.RabbitMqEsSender;
import org.oyyj.blogservice.config.mqConfig.sender.RabbitMqPublishSender;
import org.oyyj.blogservice.config.mqConfig.sender.RabbitMqUserBehaviorSender;
import org.oyyj.blogservice.config.pojo.BlogActivityLevel;
import org.oyyj.blogservice.dto.*;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.blogservice.mapper.TypeTableMapper;
import org.oyyj.blogservice.mapper.UserBehaviorMapper;
import org.oyyj.blogservice.pojo.*;
import org.oyyj.blogservice.service.*;
import org.oyyj.blogservice.service.es.EsBlogService;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.blogservice.vo.blogs.BlogSearchVO;
import org.oyyj.blogservice.vo.blogs.CommendBlogsByAuthor;
import org.oyyj.mycommon.common.BehaviorEnum;
import org.oyyj.mycommon.common.EsBlogWork;
import org.oyyj.mycommon.pojo.dto.UserBlogInfoDTO;
import org.oyyj.mycommon.utils.FileUtil;
import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.oyyj.mycommonbase.utils.ObjectMapUtil;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import reactor.util.retry.Retry;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.oyyj.mycommon.utils.TransUtil.formatNumber;

@Slf4j
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Value("${spring.cloud.nacos.discovery.cluster-name}")
    private String instanceName;

    @Autowired
    private TypeTableMapper typeTableMapper;

    @Autowired
    private ItemCF itemCF;

    @Autowired
    private UserCF userCF;

    @Autowired
    private IBlogTypeService blogTypeService;

    @Autowired
    private UserFeign userFeign;

    @Autowired
    private ICommentService commentService;

    @Autowired
    private IReplyService replyService;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private IBackstopStrategyService backstopStrategyService; // 注入兜底服务

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RabbitMqUserBehaviorSender  rabbitMqUserBehaviorSender;

    @Autowired
    private IUserBehaviorService userBehaviorService; // 注意 循环依赖

    @Autowired
    private RabbitMqEsSender rabbitMqEsSender;

    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private EsBlogService esBlogService;

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private SnowflakeUtil snowflakeUtil;

    @Autowired
    private RabbitMqPublishSender publishSender;
    @Autowired
    private RabbitMqPublishSender rabbitMqPublishSender;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBlog(Blog blog) {
        boolean save = save(blog);
        if(save){
            Long id = blog.getId();
            if (blog.getTypeList() != null && !blog.getTypeList().isEmpty()) {
                // 相关联的类型
                List<Long> listIds = blog.getTypeList().stream().map(Long::valueOf).toList();
                List<TypeTable> typeTables = typeTableMapper.selectList(Wrappers.<TypeTable>lambdaQuery().in(TypeTable::getId, listIds));

                List<BlogType> list = typeTables.stream().map(i -> BlogType.builder().blogId(id).typeId(i.getId()).build()).toList(); // 流式处理
                boolean saveTypes = blogTypeService.saveBatch(list);
                if(saveTypes){
                    // 发布MQ消息
                    RabbitMqEsSender.EsMqDTO esMqDTO = new RabbitMqEsSender.EsMqDTO();
                    esMqDTO.setBlogId(String.valueOf(blog.getId()));
                    esMqDTO.setTitle(blog.getTitle());
                    esMqDTO.setContent(blog.getContext());
                    esMqDTO.setEsBlogWork(EsBlogWork.SAVE);
                    rabbitMqEsSender.sendEsMessage(esMqDTO);
                }

            }
        }
        return save;

    }

    @Override
    public boolean saveBlog(BlogDTO blogDTO , LoginUser loginUser) {
        Date date = new Date();
        Blog blog = Blog.builder()
                .title(blogDTO.getTitle())
                .context(blogDTO.getContext())
                .userId(loginUser.getUserId())
                .author(loginUser.getUserName())
                .createTime(date)
                .updateTime(date)
                .typeList(blogDTO.getTypeList())
                .introduce(blogDTO.getIntroduce())
                .isDelete(0)
                .build();

        // 判断是否时延时任务
        if(blogDTO.getPublishMode() == null || !blogDTO.getPublishMode().equals(PublishEnum.TIMED.getValue())){
            if(blogDTO.getPublishMode().equals(PublishEnum.PUBLISH.getValue())){
                blog.setStatus(2);
                blog.setPublishTime(date);
            }else{
                blog.setStatus(1);
            }
            boolean save = save(blog);
            if(save){
                Long id = blog.getId();
                if (blog.getTypeList() != null && !blog.getTypeList().isEmpty()) {
                    // 相关联的类型
                    List<Long> listIds = blog.getTypeList().stream().map(Long::valueOf).toList();
                    List<TypeTable> typeTables = typeTableMapper.selectList(Wrappers.<TypeTable>lambdaQuery().in(TypeTable::getId, listIds));

                    List<BlogType> list = typeTables.stream().map(i -> BlogType.builder().blogId(id).typeId(i.getId()).build()).toList(); // 流式处理
                    boolean saveTypes = blogTypeService.saveBatch(list);
                    if(saveTypes){
                        // 发布MQ消息
                        RabbitMqEsSender.EsMqDTO esMqDTO = new RabbitMqEsSender.EsMqDTO();
                        esMqDTO.setBlogId(String.valueOf(blog.getId()));
                        esMqDTO.setTitle(blog.getTitle());
                        esMqDTO.setContent(blog.getContext());
                        esMqDTO.setEsBlogWork(EsBlogWork.SAVE);
                        rabbitMqEsSender.sendEsMessage(esMqDTO);
                    }

                }
            }
        }else if(blogDTO.getPublishMode().equals(PublishEnum.TIMED.getValue())){
            // 分别设置定时发布和 延时发布
            if(blogDTO.getTimedType() == null){
                log.warn("延时发布所选择的延时类型 不可未空 数据如下：{} 用户ID：{}",blogDTO ,loginUser.getUserId());
                return false;
            }

            blog.setStatus(1);
            if(blogDTO.getTimedType().equals(TimedModeEnum.SCHEDULE.getValue())){
                Date publishTime = blogDTO.getPublishTime();
                Date now = new Date();
                // 15 天的毫秒数
                long fifteenDaysMillis = 15L * 24 * 60 * 60 * 1000;
                boolean valid =
                        publishTime != null
                                && publishTime.after(now)
                                && publishTime.getTime() - now.getTime() <= fifteenDaysMillis;
                if(!valid){
                    log.warn("定时信息设置无效");
                    return  false;
                }

                if(blogDTO.getPublishTime()== null){
                    log.warn("定时信息不可为空");
                    return  false;
                }

                if(save(blog)){
                    Long id = blog.getId();
                    if (blog.getTypeList() != null && !blog.getTypeList().isEmpty()) {
                        // 相关联的类型
                        List<Long> listIds = blog.getTypeList().stream().map(Long::valueOf).toList();
                        List<TypeTable> typeTables = typeTableMapper.selectList(Wrappers.<TypeTable>lambdaQuery().in(TypeTable::getId, listIds));

                        List<BlogType> list = typeTables.stream().map(i -> BlogType.builder().blogId(id).typeId(i.getId()).build()).toList(); // 流式处理
                        boolean saveTypes = blogTypeService.saveBatch(list);
                        if(saveTypes){
                            Boolean b = redisUtil.zAdd(RedisPrefix.BLOG_PUBLISH_ZSET+instanceName, blog.getId(), blogDTO.getPublishTime().getTime());
                            if (!b) {
                                log.warn("博客定时发布信息 失败 用户ID ：{}，博客信息：{}",loginUser.getUserId(),blogDTO);
                                return  false;
                            }
                        }

                    }
                }else{
                    log.warn("博客定时发布信息保存 失败 用户ID ：{}，博客信息：{}",loginUser.getUserId(),blogDTO);
                    return  false;
                }
            }else if(blogDTO.getTimedType().equals(TimedModeEnum.DELAY.getValue())){
                if(blogDTO.getDelayMinutes() <= 0 ){
                    log.warn("博客延时发布时间必须大于等于0 用户ID ：{}，博客信息：{}",loginUser.getUserId(),blogDTO);
                    return  false;
                }
                long executeTime = System.currentTimeMillis() + blogDTO.getDelayMinutes() * 60 * 1000L;
                Date publishDate = new Date(executeTime);
                blog.setPublishTime(publishDate);
                if(save(blog)){
                    Long id = blog.getId();
                    if (blog.getTypeList() != null && !blog.getTypeList().isEmpty()) {
                        // 相关联的类型
                        List<Long> listIds = blog.getTypeList().stream().map(Long::valueOf).toList();
                        List<TypeTable> typeTables = typeTableMapper.selectList(Wrappers.<TypeTable>lambdaQuery().in(TypeTable::getId, listIds));

                        List<BlogType> list = typeTables.stream().map(i -> BlogType.builder().blogId(id).typeId(i.getId()).build()).toList(); // 流式处理
                        boolean saveTypes = blogTypeService.saveBatch(list);
                        if(saveTypes){

                            Boolean b = redisUtil.zAdd(RedisPrefix.BLOG_PUBLISH_ZSET+instanceName, blog.getId(), executeTime);
                            if (!b) {
                                log.warn("博客延时发布信息 失败 用户ID ：{}，博客信息：{}",loginUser.getUserId(),blogDTO);
                                return  false;
                            }
                        }

                    }

                }else{
                    log.warn("博客延时发布信保存失败 用户ID ：{}，博客信息：{}",loginUser.getUserId(),blogDTO);
                    return  false;
                }
            }
        }else{
            // mode状态异常 重新处理
            log.error("mode状态异常 数据{}",blogDTO);
            return false;
        }
        return true;
    }




    @Override
    public ReadDTO ReadBlog(Long id,LoginUser loginUser) {
        // 获取 blog的主要信息
        return getBlogInfo(id, loginUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readBlogValid(Long id, LoginUser loginUser) throws Exception {
        Boolean call = RetryConfig.LOCK_RETRYER.call(() -> userBehaviorService.incrementReadCount(id, loginUser.getUserId()));
        if(call != null && call){
            log.info("用户博客有效阅读 用户行为与博客阅读数+1");
            return;
        }
        // 兜底 发送消息
        rabbitMqUserBehaviorSender.sendUserBehaviorMessage(BehaviorEnum.VIEW.getCode() , id,loginUser.getUserId());
    }

    // 从缓存中获取数据 并使用redisson处理缓存击穿
    public ReadDTO getBlogInfo( Long id, LoginUser loginUser) {
        String countKey = RedisPrefix.BLOG_INGO + id;
        // 1. 先查缓存
        Map<String, String> hashWithString = redisUtil.getHashWithString(countKey);
        if (!hashWithString.isEmpty()) {
            try {
                // 增加延期
                redisUtil.resetExpire(countKey,1800L); // 信息延期30分钟
                redisUtil.resetExpire(RedisPrefix.BLOG_READ_COUNT+id,1800L); // 同时初始时or修改后的记录数 也延期
                return ObjectMapUtil.toBean(ReadDTO.class, hashWithString);
            } catch (Exception e) {
                log.warn("反序列化博客缓存失败，blogId: {}", id + ":" + e);
                // 缓存失效 直接删除
                redisTemplate.delete(countKey);
            }
        }
        ReadDTO result =  null;
        // redis中没有存储缓存 查询当前博客的查询次数
        Integer viewTimes = redisUtil.getInteger(RedisPrefix.BLOG_VIEW_COUNT + id);

        if( viewTimes == null || viewTimes < 1000){
            // 查询数据库
            result = getReadDTO( id, loginUser);
            if(viewTimes == null){
                redisUtil.set(RedisPrefix.BLOG_VIEW_COUNT + id, 1,30,TimeUnit.MINUTES); // 初始化 --数据量不大允许重复 数据一致要求不高
            }else{
                redisUtil.incr(RedisPrefix.BLOG_VIEW_COUNT + id); // 加一
            }
            return result;
        }
        return loadHotBlog(id,loginUser,countKey);
    }

    /**
     * 尝试讲热门文章加载到redis中
     *
     * @param id
     * @param loginUser
     * @param countKey
     * @return
     */
    private ReadDTO loadHotBlog(Long id, LoginUser loginUser,String countKey){
        // 缓存未命中  准备获取分布式锁
        String lockKey = RedisPrefix.LOCK_BLOG_INGO+id; // 同时锁住
        RLock lock = redissonClient.getLock(lockKey);

        ReadDTO result;
        try {
            // 启动看门狗  最大等待1秒 1秒内获取锁 就true 反之false  使用看门狗
            boolean isLocked = lock.tryLock(1,-1, TimeUnit.SECONDS);
            if(isLocked){
                try {
                    // 再次检查避免 再处理锁的时候获取到数据了
                    Map<String, String> hashWithString = redisUtil.getHashWithString(countKey);
                    if(Objects.nonNull(hashWithString) && !hashWithString.isEmpty()){
                        return ObjectMapUtil.toBean(ReadDTO.class, hashWithString);
                    }
                    // 查询数据库
                    result = getReadDTO( id, loginUser);
                    if(result != null){
                        // 将整体作为一个HASH存储到redis中
                        Map<String, String> map = ObjectMapUtil.toMap(ReadDTO.class, result);
                        redisUtil.setHashWithString(countKey, map,30, TimeUnit.MINUTES);
                        // 同时增加当前的阅读量记录 便于后续同步到数据库时进行检查
                        redisUtil.set(RedisPrefix.BLOG_READ_COUNT + id, result.getWatch(),30,TimeUnit.MINUTES);
                    }else{
                        redisUtil.setHashWithString(countKey, Map.of(),30, TimeUnit.MINUTES);
                    }
                } catch (Exception e) {
                    log.error("获取博客内容并上传至redis中报错，blogId:{}",id,e);
                    // 降级 直查询数据库
                    result = getReadDTO( id, loginUser);
                }finally {
                    if(lock.isHeldByCurrentThread()){
                        lock.unlock();
                    }
                }
            }else{
                // 重试+兜底
                for (int i = 0; i < 3; i++) {
                    Map<String, String> hashWithString = redisUtil.getHashWithString(countKey);
                    if(Objects.nonNull(hashWithString) && !hashWithString.isEmpty()){
                        return ObjectMapUtil.toBean(ReadDTO.class, hashWithString);
                    }
                }
                // 降级策略 直接查询数据库
                result = getReadDTO( id, loginUser);
            }

        } catch (InterruptedException e) {
            log.error("获取博客内容并上传至redis中报错，blogId:{}",id,e);
            // 降级 直查询数据库
            result = getReadDTO( id, loginUser);
        }
        return result;
    }

    // 初始化博客内容
    /**
     * 从数据库中获取博客内容等信息
     * @param id
     * @param loginUser
     * @return
     */
    public ReadDTO getReadDTO(Long id, LoginUser loginUser) {
        Blog one = getOne(Wrappers.<Blog>lambdaQuery().eq(Blog::getId, id));
        if (Objects.isNull(one)) {
            return null;
        }

        // 获取与其相关的类型type
        List<BlogType> list = blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, id));

        // 封装
        // todo 测试
        // Map<Long, String> userNameMap = userFeign.getUserNameMap();
        ReadDTO readDTO = ReadDTO.builder()
                .id(String.valueOf(id))
                .userId(String.valueOf(one.getUserId()))
               // .userName(userNameMap.get(one.getUserId()))
                .userName(one.getAuthor())
                .publishTime(one.getPublishTime())
                .title(one.getTitle())
                .Introduce(one.getIntroduce())
                .context(one.getContext())
                .createTime(one.getCreateTime())
                .updateTime(one.getUpdateTime())
                .star(String.valueOf(one.getStar()))
                .commentNum(String.valueOf(one.getCommentNum()))
                .kudos(String.valueOf(one.getKudos()))
                .watch(String.valueOf(one.getWatch()))
                .build();

        if (!Objects.isNull(list)) {
            List<TypeTable> typesByBlogId = typeTableMapper.findTypesByBlogId(id);
            readDTO.setTypeList(typesByBlogId);
        }

        if(Objects.nonNull(loginUser) && Objects.nonNull(loginUser.getUserId())){
            Boolean userKudos = userFeign.isUserKudos(id, String.valueOf(loginUser.getUserId()));
            readDTO.setIsUserKudos(Objects.nonNull(userKudos)?userKudos:false);
            Boolean userStar = userFeign.isUserStar(id, loginUser.getUserId());
            readDTO.setIsUserStar(Objects.nonNull(userStar)?userStar:false);
        }
        return readDTO;
    }



    // 定期从redis中得到数据存储到数据库中 保证数据的一致性   同时也避免高并发导致的数据问题
    /**
     * 定期同步Redis阅读数到数据库
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void syncReadCountToDB() {

        RLock lock = redissonClient.getLock(RedisPrefix.BLOG_READ_LOCK);

        try {
            // 加锁 更新
            boolean isLocked = lock.tryLock(1, -1, TimeUnit.SECONDS);
            if(isLocked){

                Integer integer = redisUtil.getInteger(RedisPrefix.BLOG_READ_UPDATE);
                if(Objects.nonNull(integer)){
                    // 其他实例已经完成更新了
                    return;
                }
                // 获取所有数据增加时的旧数据
                Set<String> keys =  scanRedisKeys(RedisPrefix.BLOG_READ_COUNT + "*");
                List<BlogReadDTO> readDTOList = new ArrayList<>();
                List<Long> blogIds = keys.stream().map(item -> {
                    String substring = item.substring(RedisPrefix.BLOG_READ_COUNT.length());
                    return Long.parseLong(substring);
                }).toList();

                if(blogIds.isEmpty()){
                    return;
                }
                Map<Long, Long> blogWatchMap = list(Wrappers.<Blog>lambdaQuery()
                        .in(Blog::getId, blogIds)
                        .select(Blog::getWatch, Blog::getId)
                ).stream().collect(Collectors.toMap(Blog::getId, Blog::getWatch));

                for (String key : keys) {
                    Long blogId = Long.parseLong(key.substring(RedisPrefix.BLOG_READ_COUNT.length())); // 分割时博客的ID
                    Long readCount = blogWatchMap.get(blogId);
                    String recordCountStr = redisTemplate.opsForValue().get(key);
                    if(recordCountStr == null){
                        continue;
                    }
                    Long recordCount = Long.parseLong(recordCountStr);
                    Map<String, String> hashWithString = redisUtil.getHashWithString(RedisPrefix.BLOG_INGO + blogId);
                    ReadDTO readDTO = ObjectMapUtil.toBean(ReadDTO.class, hashWithString);
                    String watch = readDTO.getWatch();
                    if(Objects.isNull(watch) || watch.isEmpty()){
                        continue;
                    }
                    // 计算偏差
                    int offset = Math.toIntExact(readCount - recordCount);
                    offset = Math.max(offset, 0);
                    int newCount = Integer.parseInt(watch) + offset;
                    readDTOList.add(new BlogReadDTO(blogId, newCount));
                    // 更新记录
                    redisUtil.set(RedisPrefix.BLOG_READ_COUNT+blogId,newCount);
                }

                int batchSize = 1000;
                int start = 0;
                int total = readDTOList.size();
                do{
                    List<BlogReadDTO> reads = new ArrayList<>(readDTOList.subList(start, Math.min(total, start + batchSize)));
                    if(!reads.isEmpty()){
                        blogMapper.updateBlogBatch(reads);
                    }
                    start+=batchSize;
                }while (start < readDTOList.size());
                // 更新 记录并设置过期时间
                redisUtil.set(RedisPrefix.BLOG_READ_UPDATE,1,2,TimeUnit.MINUTES);

            }else{
                // todo 使用演示队列 重试
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }

    }

    /**
     * 通过scan 非阻塞时的获取keys
     * @param pattern
     * @return
     */
    private Set<String> scanRedisKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        Closeable cursor = null;
        try {
            // 获取Redis连接
             cursor = redisTemplate.executeWithStickyConnection(redisConnection -> { // 粘性链接 回调函数会复用这条链接
                RedisKeyCommands keyCommands = redisConnection.keyCommands(); // redis key相关命令
                ScanOptions scanOptions = ScanOptions.scanOptions()
                        .match(pattern) // 匹配前缀
                        .count(1000) // 每次扫描1000条
                        .build();
                return keyCommands.scan(scanOptions);
            });
            Cursor<String> stringCursor = (Cursor<String>) cursor;
            log.info("开始遍历Redis Key，匹配模式：{}", pattern);
            while (stringCursor.hasNext()) {
                try {
                    String key = stringCursor.next();
                    if (key != null && !key.trim().isEmpty()) {
                        keys.add(key);
                    }
                } catch (Exception e) {
                    log.error("读取单条Redis Key失败，跳过该Key", e);
                }
            }
            log.info("Redis Key遍历完成，匹配模式：{}，共获取{}条Key", pattern, keys.size());
            cursor.close();
            return keys;
        } catch (Exception e) {
            log.error("Redis SCAN遍历Key异常，匹配模式：{}", pattern, e);
        } finally {
            // 强制关闭游标（cursor必然非null，或异常时cursor未赋值，此处判断仍保留以兼容极端场景）
            if (cursor != null) {
                try {
                    cursor.close();
                    log.debug("Redis SCAN游标已关闭，匹配模式：{}", pattern);
                } catch (IOException e) {
                    log.error("关闭Redis SCAN游标失败，匹配模式：{}", pattern, e);
                }
            }
        }
        return keys;
    }

    /**
     * 兜底推荐，确保redis挂掉 或者 推荐不出来时 是能根据 博客热度进行筛选
     * @param userId 基于行为作为筛选
     * @return
     */
    private List<String> safetyNetSelect(Long userId){
        List<BlogActivityLevel> blogActivityLevel = userBehaviorMapper.getBlogActivityLevel(userId);
        List<Long> result = new ArrayList<>(new ArrayList<>(blogActivityLevel).stream().map(BlogActivityLevel::getBlogId).toList());
        if( result.size() <= 20){
            // 用户浏览量较多其他用户浏览的商品 从数据库中随机抽取博客给用户
            // 生成动态随机种子（范围：1~Long.MAX_VALUE，避免固定值）
            long randomSeed = ThreadLocalRandom.current().nextLong();
            List<Long> blogIds = blogMapper.selectBlogIdRand(randomSeed);
            result.addAll(blogIds);
        }
        return  result.stream().map(String::valueOf).toList();
    }


    /**
     * 为用户推荐博客IDS
     * @param userId
     * @return
     */
    private List<String> recommendUserBlogs(Long userId) {
        // 先尝试从redis中获取之前推荐过的数据（量大不用重复计算）
        List<String> recommendList = redisUtil.getList(RedisPrefix.RECOMMEND_USER + userId);
        if(recommendList == null || recommendList.isEmpty() || recommendList.size()<=100 ){
            List<String> newRecommendBlogs = addNewRecommendBlogs(userId);
            // 随机抽取 20 条 返回 其余保存到redis中
            List<String> newRecommends = new ArrayList<>(newRecommendBlogs);
            List<String> randomElements = getRandomElements(newRecommends, 20);
            // 新增的数据中 删除
            newRecommends.removeAll(randomElements);
            // 其余保存到redis中
            redisUtil.setList(RedisPrefix.RECOMMEND_USER + userId, newRecommends);
            return randomElements;
        }else{
            // 数量较多 随机抽取20条后 然后将redis中的数据删除20条
            List<String> randomElements = getRandomElements(recommendList, 20);
            // 删除多余数据
            randomElements.forEach(randomElement -> {
                redisUtil.removeListItem(RedisPrefix.RECOMMEND_USER + userId,1,randomElement);
            });
            return randomElements;
        }
    }

    /**
     * 从列表中随机抽取指定数量元素
     * @param sourceList
     * @param count
     * @return
     */
    private List<String> getRandomElements(List<String> sourceList , int count){
        List<String> tempList = new ArrayList<>(sourceList);
        Collections.shuffle(tempList); // 打乱顺序
        int endIndex = Math.min(tempList.size(), count);
        return tempList.subList(0, endIndex);
    }

    /**
     * 重新计算并推荐给用户
     * @param userId
     * @return
     */
    private List<String> addNewRecommendBlogs(Long userId){

        List<Recommendation> recommendationsByItem = itemCF.recommendForUser(userId, 100);
        List<Recommendation> recommendationsByUser = userCF.recommendForUser(userId, 100);

        List<Long> resultList = new ArrayList<>(recommendationsByItem.stream().map(Recommendation::getBlogId).toList());
        resultList.addAll(recommendationsByUser.stream().map(Recommendation::getBlogId).toList());
        List<String> result = new ArrayList<>(resultList.stream().map(String::valueOf).toList());
        // 当为用户推荐时 数量较低 添加兜底保证每次查询能够返回给用户
        if(resultList.size()<20){
            List<String> safeNetList = safetyNetSelect(userId);
            result.addAll(safeNetList);
        }
        return result;
    }

    private BlogDTO toBlogDTO(Blog i,Map<String,List<String>> blogTypeMap,Map<Long, String> imageInIds){
        if(blogTypeMap==null){
            blogTypeMap = new HashMap<>();
        }
        if(imageInIds==null){
            imageInIds = new HashMap<>();
        }
        return BlogDTO.builder()
                .id(String.valueOf(i.getId()))
                .title(i.getTitle())
                .userId(String.valueOf(i.getUserId()))
                .userName(i.getAuthor())
                .introduce(i.getIntroduce())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .typeList( blogTypeMap.isEmpty()? List.of() : blogTypeMap.get(String.valueOf(i)))
                .star(numberStr(i.getStar()))
                .like(numberStr(i.getKudos()))
                .view(numberStr(i.getWatch()))
                .commentNum(numberStr(i.getCommentNum()))
                .userHead( imageInIds.isEmpty() ? "" : imageInIds.get(i.getUserId()))
                .build();
    }

    private BlogSearchVO toBlogSearchVO(Blog i,Map<String,List<String>> blogTypeMap,Map<Long, String> imageInIds){
        if(blogTypeMap==null){
            blogTypeMap = new HashMap<>();
        }
        if(imageInIds==null){
            imageInIds = new HashMap<>();
        }
        return BlogSearchVO.builder()
                .id(i.getId())
                .userId(String.valueOf(i.getUserId()))
                .userName(i.getAuthor())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .typeList( blogTypeMap.isEmpty()? List.of() : blogTypeMap.get(String.valueOf(i)))
                .star(numberStr(i.getStar()))
                .like(numberStr(i.getKudos()))
                .view(numberStr(i.getWatch()))
                .commentNum(numberStr(i.getCommentNum()))
                .userHead( imageInIds.isEmpty() ? "" : imageInIds.get(i.getUserId()))
                .build();
    }

    @Override
    public List<BlogDTO> getHomeBlogs(LoginUser loginUser) {
        List<String> blogIds = recommendUserBlogs(loginUser.getUserId());
        List<Long> ids = blogIds.stream().map(Long::valueOf).toList();
        if(ids.isEmpty()){
            return Collections.emptyList();
        }
        // 获取博客类别类别信息
        Map<String,List<String>> blogTypeMap = new ConcurrentHashMap<>();
        blogIds.forEach(id->{
            List<String> list = redisUtil.getList(RedisPrefix.ITEM_TYPE + id);
            blogTypeMap.put(id,list);
        });

        List<Blog> list = list(Wrappers.<Blog>lambdaQuery()
                .in(Blog::getId, ids)
        );
        List<String> userIds = list.stream().map(Blog::getUserId)
                .filter(Objects::nonNull).map(String::valueOf).toList();

        Map<Long, String> imageInIds = userFeign.getImageInIds(userIds);
        List<BlogDTO> result = list.stream().map(i -> toBlogDTO(i,blogTypeMap, imageInIds)).toList();
        return result;
    }

    /**
     * 处理数据转换 转换成 数字 or 数据K or 数据W
     * @param num
     * @return
     */
    private String numberStr(Long num){
        BigDecimal bigDecimal = new BigDecimal(num);
        if(bigDecimal.compareTo(new BigDecimal(1000))<0){
            return String.valueOf(num);
        }
        if(bigDecimal.compareTo(new BigDecimal(1000))>=0 && bigDecimal.compareTo(new BigDecimal(10000))<0 ){
            BigDecimal divide = bigDecimal.divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);
            return divide.toString()+"K";
        }
        if(bigDecimal.compareTo(new BigDecimal(10000))>=0){
            BigDecimal divide = bigDecimal.divide(new BigDecimal(10000), 2, RoundingMode.HALF_UP);
            return divide.toString()+"W";
        }
        return "0";
    }

    @Override
    public PageDTO<BlogDTO> getBlogByPage(int current, int pageSize, String type, LoginUser  loginUser) {
        IPage<Blog> page = new Page<>(current, pageSize);
        Map<Long, String> userNameMap = userFeign.getUserNameMap();
        if (type == null || type.isEmpty()) {
            List<BlogDTO> list = list(page).stream().map(i -> BlogDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .title(i.getTitle())
                    .userId(String.valueOf(i.getUserId()))
                    .userName(userNameMap.get(i.getUserId()))
                    .introduce(i.getIntroduce())
                    .createTime(i.getCreateTime())
                    .updateTime(i.getUpdateTime())
                    .status(i.getStatus())
                    .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                            .stream().map(j -> {
                                TypeTable typeTable = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                                return typeTable.getName();
                            }).toList())
                    .star(String.valueOf(i.getStar()))
                    .like(String.valueOf(i.getKudos()))
                    .view(String.valueOf(i.getWatch()))
                    .commentNum(String.valueOf(i.getCommentNum()))
                    .build()
            ).toList();

            PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
            blogDTOPageDTO.setPageList(list);
            blogDTOPageDTO.setPageSize(pageSize);
            blogDTOPageDTO.setPageNow(current);
            blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
            return blogDTOPageDTO;

        } else {
            TypeTable typeTable = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getName, type));
            List<Long> list = blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getTypeId, typeTable.getId()))
                    .stream().map(BlogType::getBlogId).toList();
            if (!list.isEmpty()) {
                List<BlogDTO> list2 = list(page, Wrappers.<Blog>lambdaQuery().in(Blog::getId, list)).stream().map(i -> BlogDTO.builder()
                        .id(String.valueOf(i.getId()))
                        .title(i.getTitle())
                        // 暂时不传递 文章内容
                        .userId(String.valueOf(i.getUserId()))
                        .userName(userNameMap.get(i.getUserId()))
                        .introduce(i.getIntroduce())
                        .createTime(i.getCreateTime())
                        .updateTime(i.getUpdateTime())
                        .status(i.getStatus())
                        .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                                .stream().map(j -> {
                                    TypeTable typeTables = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                                    return typeTables.getName();
                                }).toList())
                        .star(String.valueOf(i.getStar()))
                        .like(String.valueOf(i.getKudos()))
                        .view(String.valueOf(i.getWatch()))
                        .commentNum(String.valueOf(i.getCommentNum()))
                        .build()
                ).toList();
                PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
                blogDTOPageDTO.setPageList(list2);
                blogDTOPageDTO.setPageSize(pageSize);
                blogDTOPageDTO.setPageNow(current);
                blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
                return blogDTOPageDTO;
            } else {
                PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
                blogDTOPageDTO.setPageList(null);
                blogDTOPageDTO.setPageSize(pageSize);
                blogDTOPageDTO.setPageNow(current);
                blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
                return blogDTOPageDTO;
            }
        }
    }

    /**
     * 定时任务
     */
    @Override
    @Scheduled(cron = " 0/30 * * * * * ")
    public void publishDelayBlogs() {
        long currentTime = System.currentTimeMillis();
        Set<Object> objects = redisUtil.zGet(RedisPrefix.BLOG_PUBLISH_ZSET+instanceName, currentTime);
        if(objects == null || objects.isEmpty()){
            return;
        }

        List<Long> list = objects.stream().map(item -> {
            if (item instanceof Long) {
                return (Long) item;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).toList();
        if(list.isEmpty()){
            return;
        }
        // 获取已经发布成功的数据并删除部分数据
        List<Long> alPublishBlogIds = list(Wrappers.<Blog>lambdaQuery()
                .in(Blog::getId, list)
                .ne(Blog::getStatus, 1)
                .select(Blog::getId)
        ).stream().map(Blog::getId).toList();
        List<String> alPublishBlogIdStr = alPublishBlogIds.stream().map(String::valueOf).toList();
        // 清空过期数据
        redisUtil.zRem(RedisPrefix.BLOG_PUBLISH_ZSET+instanceName, alPublishBlogIdStr);

        list = list.stream().filter(item-> !alPublishBlogIds.contains(item)).toList();

        try {
            List<Long> finalList = list;
            Boolean call = RetryConfig.LOCK_RETRYER.call(() -> rabbitMqPublishSender.sendPublishMessage(finalList));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (RetryException e) {
            log.error("重试尝试失败",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 数据库兜底处理
     */
    @Override
    @Scheduled(cron = " 15 */5 * * * * ")
    public void publishErrorBlogs() {
        List<Blog> list = list(Wrappers.<Blog>lambdaQuery().eq(Blog::getStatus, 1)
                .le(Blog::getPublishTime, new Date())
                .select(Blog::getId, Blog::getStatus, Blog::getPublishTime)
        );

        List<Long> blogIds = list.stream().map(Blog::getId).toList();
        if(blogIds.isEmpty()){
            return;
        }
        update(Wrappers.<Blog>lambdaUpdate()
                .in(Blog::getId, blogIds)
                .set(Blog::getStatus, 2)
        );
    }



    /**
     * 获取用户的博客原创和所有的访问数数
     *
     * @param userId 用户id
     * @return 数量集合 1是原创数 2 是访问数 3 总访问数
     */
    @Override
    public List<Long> getUserBlogNum(Long userId) {
        List<Blog> list = list(Wrappers.<Blog>lambdaQuery()
                .eq(Blog::getUserId, userId));

        if (list.isEmpty()) {
            return null;
        }
        List<Long> resultList = new ArrayList<>();
        long sumKudos = list.stream().mapToLong(Blog::getKudos).sum(); // 总点赞数
        long sumWatch = list.stream().mapToLong(Blog::getWatch).sum();
        resultList.add((long) list.size());
        resultList.add(sumWatch);
        resultList.add(sumKudos);

        return resultList;


    }

    @Override
    public PageDTO<BlogDTO> getBlogByName(int current, int pageSize, String blogName) {


        if (Objects.isNull(blogName)) {
            blogName = " ";
        }
        IPage<Blog> page = new Page<>(current, pageSize);
        List<Blog> list = list(page, Wrappers.<Blog>lambdaQuery().like(Blog::getTitle, blogName));
        List<Long> userIdList = list.stream().map(Blog::getUserId).toList();
        List<String> userIds = userIdList.stream().map(String::valueOf).toList();

        // 获取用户信息
        Map<Long, String> nameInIds = userFeign.getNameInIds(userIds);
        List<BlogDTO> blogDTOS = list.stream().map(i -> BlogDTO.builder()
                .id(String.valueOf(i.getId()))
                .title(i.getTitle())
                .context(i.getContext())
                .userId(String.valueOf(i.getUserId()))
                .userName(nameInIds.get(i.getUserId()))
                .introduce(i.getIntroduce())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                        .stream().map(j -> {
                            TypeTable typeTables = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                            return typeTables.getName();
                        }).toList())
                .like(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .view(String.valueOf(i.getWatch()))
                .commentNum(String.valueOf(i.getCommentNum()))
                .build()
        ).toList();

        PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
        blogDTOPageDTO.setPageList(blogDTOS);
        blogDTOPageDTO.setPageSize(pageSize);
        blogDTOPageDTO.setPageNow(current);
        blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
        return blogDTOPageDTO;

    }

    /**
     * 根据作者ID查询
     * @param current 当前页数
     * @param pageSize 查询量
     * @param userId 作者ID
     * @param orderBy 排序依据的字段（发布时间  热度 ）
     * @param isDesc 正序or倒序
     * @return
     */
    @Override
    public PageDTO<BlogDTO> getBlogByUserId(int current, int pageSize, Long userId, List<String> typeList,String orderBy,String isDesc) {
        if (Objects.isNull(userId)) {
            return null;
        }
        IPage<Blog> page = new Page<>(current, pageSize);
        List<Blog> blogs ;
        List<BlogSearchDTO> blogSearchDTOS = blogMapper.selectBlogSearch(page, userId ,typeList,orderBy, isDesc);
        List<Long> blogIds =  blogSearchDTOS.stream().map(BlogSearchDTO::getBlogId).toList();
        if(blogIds.isEmpty()){
            return new PageDTO<BlogDTO>(List.of(),(int) page.getTotal(),current,pageSize);
        }
        blogs = list(Wrappers.<Blog>lambdaQuery()
                .in(Blog::getId, blogIds)
        );
        return getBlogDTOPageDTO(current, pageSize, page, blogs);
    }

    /**
     * 通过blogList得到前端视图集合
     * @param current
     * @param pageSize
     * @param page
     * @param blogs
     * @return
     */
    private PageDTO<BlogDTO> getBlogDTOPageDTO(int current, int pageSize, IPage<Blog> page, List<Blog> blogs ) {

        List<Long> blogIds = blogs.stream().map(Blog::getId).toList();

        List<BlogTypeDTO> blogTypeList = blogMapper.getBlogTypeList(blogIds);
        Map<Long, List<BlogTypeDTO>> blogTypesMap = blogTypeList.stream().collect(Collectors.groupingBy(BlogTypeDTO::getBlogId));

        List<BlogDTO> blogDTOS = blogs.stream().map(i -> BlogDTO.builder()
                .id(String.valueOf(i.getId()))
                .title(i.getTitle())
                .context(i.getContext())
                .userId(String.valueOf(i.getUserId()))
                .userName(i.getAuthor())
                .introduce(i.getIntroduce())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .typeList(blogTypesMap.containsKey(i.getId())
                        ? blogTypesMap.get(i.getId()).stream().map(BlogTypeDTO::getBlogType).collect(Collectors.toList())
                        : List.of())
                .like(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .view(String.valueOf(i.getWatch()))
                .commentNum(String.valueOf(i.getCommentNum()))
                .build()
        ).toList();

        PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
        blogDTOPageDTO.setPageList(blogDTOS);
        blogDTOPageDTO.setPageSize(pageSize);
        blogDTOPageDTO.setPageNow(current);
        blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
        return blogDTOPageDTO;
    }


    @Override
    public PageDTO<BlogDTO> getBlogByIds(int current, int pageSize, List<Long> blogs) {

        if (Objects.isNull(blogs)) {
            return null;
        }
        IPage<Blog> blogIPage = new Page<>(current, pageSize);
        List<Blog> blogList = list(blogIPage, Wrappers.<Blog>lambdaQuery().in(Blog::getId, blogs));

        List<Long> userList = blogList.stream().map(Blog::getUserId).toList(); // 获得用户信息
        List<String> users = userList.stream().map(String::valueOf).toList();
        Map<Long, String> nameInIds = userFeign.getNameInIds(users);// 获取用户名称

        List<BlogDTO> resultList = blogList.stream().map(i -> BlogDTO.builder()
                .id(String.valueOf(i.getId()))
                .title(i.getTitle())
                .context(i.getContext())
                .userId(String.valueOf(i.getUserId()))
                .userName(nameInIds.get(i.getUserId()))
                .introduce(i.getIntroduce())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                        .stream().map(j -> {
                            TypeTable typeTables = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                            return typeTables.getName();
                        }).toList())
                .like(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .view(String.valueOf(i.getWatch()))
                .commentNum(String.valueOf(i.getCommentNum()))
                .build()).toList();

        PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
        blogDTOPageDTO.setPageList(resultList);
        blogDTOPageDTO.setPageSize(pageSize);
        blogDTOPageDTO.setPageNow(current);
        blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(blogIPage.getTotal())));
        return blogDTOPageDTO;
    }

    @Override
    public List<BlogDTO> getHotBlogs() {

        List<Blog> hotBlog = blogMapper.getHotBlog();

        List<String> list = hotBlog.stream().map(Blog::getUserId).map(String::valueOf).toList();

        Map<Long, String> nameInIds = userFeign.getNameInIds(list);

        return hotBlog.stream().map(i -> BlogDTO.builder()
                .id(String.valueOf(i.getId()))
                .title(i.getTitle())
                .context(i.getContext())
                .userId(String.valueOf(i.getUserId()))
                .userName(nameInIds.get(i.getUserId()))
                .introduce(i.getIntroduce())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .like(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .view(String.valueOf(i.getWatch()))
                .commentNum(String.valueOf(i.getCommentNum()))
                .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                        .stream().map(j -> {
                            TypeTable typeTables = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                            return typeTables.getName();
                        }).toList())
                .build()).toList();
    }


    // 获取 每个月的增长数
    @Override
    public Map<String,Long> getIncreaseBlog() {
        Map<String,Long> result=new LinkedHashMap<>(); // 保证插入顺序
        List<String> monthList = getMonthList();
        Map<String, Long> collect = blogMapper.getIncreaseBlog().stream().collect(Collectors.toMap(IncreaseDTO::getMonth, IncreaseDTO::getRecord));

        for (String s : monthList) {
            result.put(s, collect.getOrDefault(s,0L));
        }
        return result;
    }

    private List<String> getMonthList() {

        List<String> result = new ArrayList<>();

        LocalDate localDate = LocalDate.now();
        int dayOfYear = localDate.getYear();
        int dayOfMonth = localDate.getMonthValue();

        // 获取最小的年份和月份
        String s = blogMapper.gerMinDate();

        String[] split = s.split("-");
        int minYear = Integer.parseInt(split[0]);
        int minMonth = Integer.parseInt(split[1]);

        while (minYear < dayOfYear) {
            // String date=String.valueOf(minYear)+"-"+String.valueOf(minMonth);
            StringBuilder date = new StringBuilder();
            date.append(minYear);
            date.append("-");
            date.append(String.format("%02d", minMonth));
            result.add(String.valueOf(date));
            if (minMonth == 12) {
                minYear++;
            } else {
                minMonth++;
            }
        }

        while (minMonth <= dayOfMonth) {
            StringBuilder date = new StringBuilder();
            date.append(minYear);
            date.append("-");
            date.append(String.format("%02d", minMonth));
            result.add(String.valueOf(date));
            minMonth++;
        }

        return result;
    }


    @Override
    public Map<String, Long> getAllTypeNum() {

        Map<String, Long> result = new LinkedHashMap<>();
        List<TypeTable> typeTables = typeTableMapper.selectList(Wrappers.<TypeTable>lambdaQuery());

        for (TypeTable typeTable : typeTables) {
            String type = typeTable.getName();
            Long typeId=typeTable.getId();

            int size = blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getTypeId, typeId)).size();
            result.put(type, (long) size);
        }
        return result;
    }

    @Override
    public String getBlogListByAdmin(String blogName, String authorName, Date startDate, Date endDate, String status, Integer currentPage) throws JsonProcessingException {

        IPage<Blog> page=new Page<>(currentPage,10);

        LambdaQueryWrapper<Blog> lqw=new LambdaQueryWrapper<>();
        if(Objects.nonNull(blogName)&&!blogName.isEmpty()){
            lqw.like(Blog::getTitle,blogName);
        }
        if(Objects.nonNull(authorName)&&!authorName.isEmpty()){
            String userIdByName = userFeign.getUserIdByName(authorName);
            lqw.like(Blog::getUserId,Long.valueOf(userIdByName));
        }
        if(Objects.nonNull(startDate)){
            lqw.ge(Blog::getCreateTime,startDate);
        }
        if(Objects.nonNull(endDate)){
            lqw.le(Blog::getCreateTime,endDate);
        }
        if(Objects.nonNull(status)&&!status.isEmpty()){
            switch(status){
                case "保存中":
                    lqw.eq(Blog::getStatus,1);
                    break;
                case "发布":
                    lqw.eq(Blog::getStatus,2);
                    break;
                case "审核中":
                    lqw.eq(Blog::getStatus,3);
                    break;
                case "禁止查看":
                    lqw.eq(Blog::getStatus,4);
                    break;
                default:
                    log.error("请求参数不正确"+status);
                    break;
            }
        }

        List<Blog> list = list(page, lqw);
        List<String> userId = list.stream().map(Blog::getUserId).map(String::valueOf).toList();

        Map<Long, String> nameInIds = userFeign.getNameInIds(userId);

        List<BlogDTO> blogDTOS = list(page, lqw).stream().map(i -> BlogDTO.builder()
                .id(String.valueOf(i.getId()))
                .title(i.getTitle())
                .context(i.getContext())
                .userId(String.valueOf(i.getUserId()))
                .userName(nameInIds.get(i.getUserId()))
                .introduce(i.getIntroduce())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .commentNum(String.valueOf(i.getCommentNum()))
                .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                        .stream().map(j -> {
                            TypeTable typeTables = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                            return typeTables.getName();
                        }).toList())
                .build()).toList();

        PageDTO<BlogDTO> pageDTO =new PageDTO<>();
        pageDTO.setPageNow((int)page.getCurrent());
        pageDTO.setPageList(blogDTOS);
        pageDTO.setTotal((int)page.getTotal());
        pageDTO.setPageSize(10);

        ObjectMapper objectMapper =new ObjectMapper();
        return objectMapper.writeValueAsString(pageDTO);

    }

    @Override
    @Transactional
    public boolean blogStar(Long blogId,LoginUser loginUser) {
        return blogIncr(blogId,RedisPrefix.BLOG_START_LOCK,loginUser.getUserId());
    }

    @Override
    @Transactional
    public boolean cancelStar(Long blogId , LoginUser loginUser) {
        return blogDecr(blogId,RedisPrefix.BLOG_START_LOCK,loginUser.getUserId());
    }

    @Override
    @Transactional
    public boolean blogKudos(Long blogId , LoginUser loginUser) {
        return blogIncr(blogId,RedisPrefix.BLOG_KUDOS_LOCK,loginUser.getUserId());
    }

    @Override
    @Transactional
    public boolean cancelKudos(Long blogId , LoginUser loginUser) {
        return blogDecr(blogId,RedisPrefix.BLOG_KUDOS_LOCK,loginUser.getUserId());
    }

    @Override
    @Async("taskExecutor")
    public void blogComment(Long blogId , LoginUser loginUser) {
        blogIncr(blogId,RedisPrefix.BLOG_COMMENT_LOCK,loginUser.getUserId());
    }

    /**
     * 根据关键词或去数据
     * @param keyWord
     * @param currentPage
     * @param pageSize
     * @return
     */
    @Override
    public ResultUtil<List<BlogSearchVO>> getBlogByKeyWord(String keyWord, Integer currentPage, Integer pageSize) {
        List<BlogSearchVO> blogSearchVOS = esBlogService.highlightSearch(keyWord, currentPage, pageSize);
        if(blogSearchVOS == null || blogSearchVOS.isEmpty()){
            return ResultUtil.success(List.of());
        }
        List<Long> blogIds = blogSearchVOS.parallelStream().map(BlogSearchVO::getId).toList();
        Map<Long, BlogSearchVO> blogSearchVOMap = blogSearchVOS.stream().collect(Collectors.toMap(BlogSearchVO::getId, Function.identity()));

        List<Blog> list = list(Wrappers.<Blog>lambdaQuery()
                .in(Blog::getId, blogIds)
        );

        List<String> userIds = list.stream().map(Blog::getUserId)
                .filter(Objects::nonNull).map(String::valueOf).toList();

        // 获取博客类别类别信息
        Map<String,List<String>> blogTypeMap = new ConcurrentHashMap<>();
        blogIds.forEach(infoId->{
            List<String> types = redisUtil.getList(RedisPrefix.ITEM_TYPE + infoId);
            blogTypeMap.put(String.valueOf(infoId),types);
        });
        Map<Long, String> imageInIds = userFeign.getImageInIds(userIds);
        List<BlogSearchVO> result = list.stream().map(i -> toBlogSearchVO(i,blogTypeMap, imageInIds)).toList();
        result = result.stream().peek(item->{
            BlogSearchVO blogSearchVO = blogSearchVOMap.get(item.getId());
            item.setTitle(blogSearchVO.getTitle());
            item.setIntroduce(blogSearchVO.getContent());
        }).toList();
       return ResultUtil.success(result);
    }

    /**
     * 上传博客中的图片
     *
     * @param file 原始文件
     * @param loginUser 登录者
     * @return
     */
    @Override
    public ResultUtil<String> uploadBlogImg(MultipartFile file, LoginUser loginUser) {
        String snowflakeId = snowflakeUtil.getSnowflakeId();
        String objectName = file.getOriginalFilename()+snowflakeId + loginUser.getUserId();
        Boolean uploaded = fileUtil.uploadImage(file, objectName);
        if(uploaded){
            return ResultUtil.success(objectName);
        }else{
            log.error("文件上传失败 文件名称{}",objectName);
            return ResultUtil.fail(objectName);
        }
    }


    @Override
    public Map<String,Object> uploadFileChunk(FileUploadDTO fileUploadDTO) {
        Map<String, Object> stringObjectMap = fileUtil.uploadChunk(fileUploadDTO.getFileNo(),
                fileUploadDTO.getChunkNum(),
                fileUploadDTO.getFileFullMd5(),
                fileUploadDTO.getFile(),
                fileUploadDTO.getMd5(),
                fileUploadDTO.getAllChunks());
        log.info("上传结果"+stringObjectMap);
        return stringObjectMap;
    }

    @Override
    public Map<String,Object> mergeFileChunk(String fileNo,Long totalFileChunks, String orgFileName) {
        Map<String, Object> stringObjectMap = fileUtil.mergeChunk(fileNo,totalFileChunks,orgFileName);
        log.info("上传结果"+stringObjectMap);
        return stringObjectMap;
    }

    @Override
    public UserBlogInfoDTO getUserBlogInfo(Long userId) {
        List<Blog> list = list(Wrappers.<Blog>lambdaQuery()
                .eq(Blog::getUserId, userId)
                .select(Blog::getId, Blog::getUserId,Blog::getWatch,Blog::getStar,Blog::getKudos)
        );
        int totalKudos = 0;
        int totalStar = 0;

        for (Blog blog : list) {
            totalStar +=  blog.getStar()!= null?  Math.toIntExact(blog.getStar()) : 0  ;
            totalKudos +=  blog.getKudos()!= null?  Math.toIntExact(blog.getKudos()) : 0  ;
        }

        UserBlogInfoDTO userBlogInfoDTO = new UserBlogInfoDTO();
        userBlogInfoDTO.setBlogs(formatNumber(list.size()));
        userBlogInfoDTO.setStar(formatNumber(totalStar));
        userBlogInfoDTO.setLikes(formatNumber(totalKudos));

        return userBlogInfoDTO;
    }

    /**
     * 依据作者信息获取其最近创作 以及热门博客
     * @param userId
     * @param currentBlogId
     * @return
     */
    @Override
    public CommendBlogsByAuthor commendBlogsByAuthor(Long userId, Long currentBlogId) {
        LambdaQueryWrapper<Blog> queryWrapper = Wrappers.<Blog>lambdaQuery();
        LambdaQueryWrapper<Blog> baseLqw = queryWrapper.eq(Blog::getUserId, userId)
                .ne(currentBlogId != null, Blog::getId, currentBlogId)
                .select(Blog::getId,
                        Blog::getTitle,
                        Blog::getIntroduce ,
                        Blog::getUserId,
                        Blog::getWatch,
                        Blog::getStar,
                        Blog::getKudos,
                        Blog::getCommentNum)
                .last("limit 10");
        // 最近创作
        LambdaQueryWrapper<Blog> lqwByTime = baseLqw.orderByDesc(Blog::getPublishTime);
        List<Blog> listByTime = list(lqwByTime);
        List<BlogDTO> resultByTime = listByTime.stream().map(item -> {
            return toBlogDTO(item, null, null);
        }).toList();
        // 热门博客
        LambdaQueryWrapper<Blog> lqwByWatch = baseLqw.orderByDesc(Blog::getWatch);
        List<Blog> listByWatch = list(lqwByWatch);
        List<BlogDTO> resultByWatch = listByWatch.stream().map(item -> {
            return toBlogDTO(item, null, null);
        }).toList();

        CommendBlogsByAuthor commendBlogsByAuthor = new CommendBlogsByAuthor();
        commendBlogsByAuthor.setHotBlogs(resultByWatch);
        commendBlogsByAuthor.setRecentBlogs(resultByTime);
        return commendBlogsByAuthor;
    }


    private boolean blogIncr(Long blogId,String prefix,Long userId) {
        Integer behaviorType = switch (prefix) {
            case RedisPrefix.BLOG_START_LOCK -> BehaviorEnum.COLLECT.getCode();
            case RedisPrefix.BLOG_KUDOS_LOCK -> BehaviorEnum.LIKE.getCode();
            case RedisPrefix.BLOG_COMMENT_LOCK -> BehaviorEnum.COMMENT.getCode();
            default -> null;
        };
        if(behaviorType == null) {
            log.error("行为不正确,prefix:{}",prefix);
            return false;
        }
        String blogStarLock = prefix+blogId;
        RLock lock = redissonClient.getLock(blogStarLock);
        try {
            Boolean forUpdate = RetryConfig.LOCK_RETRYER.call(() -> {
                return transactionTemplate.execute(status -> {
                    try {
                        boolean tryLock = lock.tryLock(1, 30, TimeUnit.SECONDS); // 开启看门狗
                        if(!tryLock){
                            status.setRollbackOnly();
                            return false; // 处罚重试
                        }
                        Blog one = getOne(Wrappers.<Blog>lambdaQuery()
                                .eq(Blog::getId, blogId)
                        );
                        if(one == null){
                            log.warn("收藏的博客不存在,id:{}",blogId);
                            status.setRollbackOnly();
                            return false;
                        }
                        boolean update = update(Wrappers.<Blog>lambdaUpdate()
                                .eq(Blog::getId, blogId)
                                .set(RedisPrefix.BLOG_START_LOCK.equals(prefix), Blog::getStar, one.getStar() + 1)
                                .set(RedisPrefix.BLOG_KUDOS_LOCK.equals(prefix), Blog::getKudos, one.getKudos() + 1)
                                .set(RedisPrefix.BLOG_COMMENT_LOCK.equals(prefix), Blog::getCommentNum, one.getCommentNum() + 1)
                        );
                        if(!update){
                            log.error("博客执行行为失败 博客ID{}, 行为：{}",blogId,prefix);
                            status.setRollbackOnly();
                            return false;
                        }
                        Boolean userFeignResult = true;
                        if(RedisPrefix.BLOG_START_LOCK.equals(prefix)){
                            userFeignResult = userFeign.userStar(String.valueOf(blogId), userId);
                        }else if(RedisPrefix.BLOG_KUDOS_LOCK.equals(prefix)){
                            userFeignResult =userFeign.kudosBlog(blogId,userId);
                        }
                        return userFeignResult;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                });
            });
            // 分布式锁+重试成功：直接返回结果
            if (forUpdate != null && forUpdate) {
                // mq 添加行为
                rabbitMqUserBehaviorSender.sendUserBehaviorMessage(behaviorType,blogId ,userId);
                return true;
            }
        } catch (RetryException  e) {
            log.error("用户操作博客 重试失败,id:{}",blogId,e);
        } catch ( ExecutionException e) {
            log.error("redisson分布式锁获取异常,id:{}",blogId,e);
        }
        // 执行兜底
        return backstopStrategyService.incrBlog(blogId,prefix);
    }

    private boolean blogDecr(Long blogId,String prefix,Long userId) {
        Integer behaviorType = switch (prefix) {
            case RedisPrefix.BLOG_START_LOCK -> BehaviorEnum.COLLECT.getCode();
            case RedisPrefix.BLOG_KUDOS_LOCK -> BehaviorEnum.LIKE.getCode();
            case RedisPrefix.BLOG_COMMENT_LOCK -> BehaviorEnum.COMMENT.getCode();
            default -> null;
        };
        if(behaviorType == null) {
            log.error("行为不正确,prefix:{}",prefix);
            return false;
        }
        String blogStarLock = prefix+blogId;
        RLock lock = redissonClient.getLock(blogStarLock);
        try {
            Boolean forUpdate = RetryConfig.LOCK_RETRYER.call(() -> transactionTemplate.execute(status -> {
                try {
                    boolean tryLock = lock.tryLock(1, 30, TimeUnit.SECONDS); // 开启看门狗
                    if(!tryLock){
                        return false; // 处罚重试
                    }
                    Blog one = getOne(Wrappers.<Blog>lambdaQuery()
                            .eq(Blog::getId, blogId)
                    );
                    if(one == null){
                        log.warn("收藏的博客不存在,id:{}",blogId);
                        return false;
                    }
                    boolean update = update(Wrappers.<Blog>lambdaUpdate()
                            .eq(Blog::getId, blogId)
                            .set(RedisPrefix.BLOG_START_LOCK.equals(prefix), Blog::getStar, one.getStar() - 1)
                            .set(RedisPrefix.BLOG_KUDOS_LOCK.equals(prefix), Blog::getKudos, one.getKudos() - 1)
                            .set(RedisPrefix.BLOG_COMMENT_LOCK.equals(prefix), Blog::getCommentNum, one.getCommentNum() - 1)
                    );

                    if(!update){
                        log.error("博客入校行为失败 博客ID{}, 行为：{}",blogId,prefix);
                        status.setRollbackOnly();
                        return false;
                    }
                    Boolean userFeignResult = true;
                    if(RedisPrefix.BLOG_START_LOCK.equals(prefix)){
                        userFeignResult = userFeign.cancelStar(String.valueOf(blogId), userId);
                    }else if(RedisPrefix.BLOG_KUDOS_LOCK.equals(prefix)){
                        userFeignResult =userFeign.cancelKudosBlog(blogId,userId);
                    }
                    return userFeignResult;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }));
            // 分布式锁+重试成功：直接返回结果
            if (forUpdate != null && forUpdate) {
                // 删除用户行为
                userBehaviorMapper.delete(Wrappers.<UserBehavior>lambdaQuery()
                        .eq(UserBehavior::getBlogId, blogId)
                        .eq(UserBehavior::getUserId, userId)
                        .eq(UserBehavior::getBehaviorType, behaviorType)
                );
                return true;
            }
        } catch (RetryException  e) {
            log.error("收藏博客 重试失败,id:{}",blogId,e);
        } catch ( ExecutionException e) {
            log.error("redisson分布式锁获取异常,id:{}",blogId,e);
        }
        // 执行兜底
        return backstopStrategyService.incrBlog(blogId,prefix);
    }

}
