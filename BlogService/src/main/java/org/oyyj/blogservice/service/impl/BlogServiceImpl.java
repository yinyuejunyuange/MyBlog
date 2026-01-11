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
import org.oyyj.blogservice.config.common.cf.ItemCF;
import org.oyyj.blogservice.config.common.cf.UserCF;
import org.oyyj.blogservice.config.mqConfig.sender.RabbitMqUserBehaviorSender;
import org.oyyj.blogservice.config.pojo.BlogActivityLevel;
import org.oyyj.blogservice.dto.*;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.blogservice.mapper.TypeTableMapper;
import org.oyyj.blogservice.mapper.UserBehaviorMapper;
import org.oyyj.blogservice.pojo.*;
import org.oyyj.blogservice.service.*;
import org.oyyj.mycommon.common.BehaviorEnum;
import org.oyyj.mycommon.common.MqStatusEnum;
import org.oyyj.mycommon.config.pojo.EnhanceCorrelationData;
import org.oyyj.mycommon.mapper.MqMessageRecordMapper;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.oyyj.mycommonbase.utils.ObjectMapUtil;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    private TypeTableMapper typeTableMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Autowired
    private MqMessageRecordMapper mqMessageRecordMapper;

    @Autowired
    private SnowflakeUtil  snowflakeUtil;

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

    @Override
    public void saveBlog(Blog blog) {
        save(blog);
        Long id = blog.getId();

        if (blog.getTypeList() != null && !blog.getTypeList().isEmpty()) {
            // 相关联的类型
            List<TypeTable> typeTables = typeTableMapper.selectList(Wrappers.<TypeTable>lambdaQuery().in(TypeTable::getName, blog.getTypeList()));

            List<BlogType> list = typeTables.stream().map(i -> BlogType.builder().blogId(id).typeId(i.getId()).build()).toList(); // 流式处理
            blogTypeService.saveBatch(list);
        }
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
                    if(Objects.nonNull(hashWithString)){
                        if(hashWithString.isEmpty()){
                            return new ReadDTO();
                        }
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
                    if(Objects.nonNull(hashWithString)){
                        if(hashWithString.isEmpty()){
                            return new ReadDTO();
                        }
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
            List<String> typeList = list.stream().
                    map(i -> typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery()
                            .eq(TypeTable::getId, i.getTypeId())).getName()).toList();
            readDTO.setTypeList(typeList);
        }

        if(Objects.nonNull(loginUser)){
            Boolean userKudos = userFeign.isUserKudos(id, String.valueOf(loginUser.getUserId()));
            readDTO.setIsUserKudos(Objects.nonNull(userKudos)?userKudos:false);
            Boolean userStar = userFeign.isUserStar(id, String.valueOf(loginUser.getUserId()));
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
        // 先尝试从redis中获取之前计算的没有遗漏的数据
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
        return list(Wrappers.<Blog>lambdaQuery()
                .in(Blog::getId, ids)
        ).stream().map(i -> BlogDTO.builder()
                .id(String.valueOf(i.getId()))
                .title(i.getTitle())
                .userId(String.valueOf(i.getUserId()))
                .userName(i.getAuthor())
                .introduce(i.getIntroduce())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .typeList(blogTypeMap.get(String.valueOf(i)))
                .star(String.valueOf(i.getStar()))
                .kudos(String.valueOf(i.getKudos()))
                .watch(String.valueOf(i.getWatch()))
                .commentNum(String.valueOf(i.getCommentNum()))
                .build()
        ).toList();
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
                    .kudos(String.valueOf(i.getKudos()))
                    .watch(String.valueOf(i.getWatch()))
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
                        .kudos(String.valueOf(i.getKudos()))
                        .watch(String.valueOf(i.getWatch()))
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


    // 获取评论
    @Override
    public List<ReadCommentDTO> getBlogComment(String blogId, String userInfoKey) {
        List<Comment> list = commentService.list(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getBlogId, Long.valueOf(blogId))
        );
        if (list.isEmpty()) {
            return null;
        }
        // 获取用户id集合
        List<String> userIds = list.stream().map(i -> {
            Long userId = i.getUserId();
            return String.valueOf(userId);
        }).toList();

        Map<Long, String> nameInIds = userFeign.getNameInIds(userIds);
        Map<Long, String> imageInIds = userFeign.getImageInIds(userIds);

        // 通过list 获取回复的集合
        List<Long> commentIds = list.stream().map(Comment::getId).toList();

        List<Reply> replyList = replyService.list(Wrappers.<Reply>lambdaQuery()
                .in(Reply::getCommentId, commentIds)
        );
        List<String> replyUserIds = replyList.stream().map(i -> {
            Long userid = i.getUserId();
            return String.valueOf(userid);
        }).toList();

        // 获得评论中的用户名和用户头像
        Map<Long, String> replyNameInIds = userFeign.getNameInIds(replyUserIds);
        Map<Long, String> replyImageInIds = userFeign.getImageInIds(replyUserIds);

        if (!Objects.isNull(userInfoKey)) {
            List<ReadCommentDTO> readCommentDTOS = list.stream().map(i -> ReadCommentDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .userId(String.valueOf(i.getUserId()))
                    .userName(nameInIds.get(i.getUserId()))
                    .UserImage("http://localhost:8080/myBlog/user/getHead/" + imageInIds.get(i.getUserId()))
                    .context(i.getContext())
                    .replyList(
                            replyService.list(Wrappers.<Reply>lambdaQuery()
                                            .eq(Reply::getCommentId, i.getId()))
                                    .stream()
                                    .map(x -> ReadReplyDTO.builder()
                                            .id(String.valueOf(x.getId()))
                                            .userId(String.valueOf(x.getUserId()))
                                            .userName(replyNameInIds.get(x.getUserId()))
                                            .userImage("http://localhost:8080/myBlog/user/getHead/" + replyImageInIds.get(x.getUserId()))
                                            .context(x.getContext())
                                            .updateTime(x.getUpdateTime())
                                            .kudos(String.valueOf(x.getKudos()))
                                            .isUserKudos(userFeign.getUserKudosReply(String.valueOf(x.getId()), userInfoKey))
                                            .build()).toList())
                    .updateTime(i.getUpdateTime())
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(userFeign.getUserKudosComment(String.valueOf(i.getId()), userInfoKey))
                    .build()
            ).toList();

            System.out.println("查询成功:" + readCommentDTOS);
            return readCommentDTOS;
        } else {
            List<ReadCommentDTO> readCommentDTOS = list.stream().map(i -> ReadCommentDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .userId(String.valueOf(i.getUserId()))
                    .userName(nameInIds.get(i.getUserId()))
                    .UserImage("http://localhost:8080/myBlog/user/getHead/" + imageInIds.get(i.getUserId()))
                    .context(i.getContext())
                    .replyList(
                            replyService.list(Wrappers.<Reply>lambdaQuery()
                                            .eq(Reply::getCommentId, i.getId()))
                                    .stream()
                                    .map(x -> ReadReplyDTO.builder()
                                            .id(String.valueOf(x.getId()))
                                            .userId(String.valueOf(x.getUserId()))
                                            .userName(replyNameInIds.get(x.getUserId()))
                                            .userImage("http://localhost:8080/myBlog/user/getHead/" + replyImageInIds.get(x.getUserId()))
                                            .context(x.getContext())
                                            .updateTime(x.getUpdateTime())
                                            .kudos(String.valueOf(x.getKudos()))
                                            .isUserKudos(false)
                                            .build()).toList())
                    .updateTime(i.getUpdateTime())
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(false)
                    .build()
            ).toList();

            System.out.println("查询成功:" + readCommentDTOS);
            return readCommentDTOS;
        }
    }


    // 评论点赞数加一或者减一
    @Override
    public Boolean changeCommentKudos(Long commentId, Integer isAdd) {
        String changeLock = RedisPrefix.BLOG_COMMENT_KUDOS_LOCK + commentId;
        RLock lock = redissonClient.getLock(changeLock);
        try {
            Boolean call = RetryConfig.LOCK_RETRYER.call(() -> {
                boolean getLock = lock.tryLock(1, -1, TimeUnit.MINUTES);
                if (!getLock) {
                    return false;
                }
                // 获取锁直接修改数据
                Comment one = commentService.getOne(Wrappers.<Comment>lambdaQuery()
                        .eq(Comment::getId, commentId)
                );
                if (one == null) {
                    log.error("查询的博客评论不存在，评论的ID为:{}", commentId);
                    return false;
                }
                return commentService.update(Wrappers.<Comment>lambdaUpdate()
                        .eq(Comment::getId, commentId)
                        .set(YesOrNoEnum.YES.getCode().equals(isAdd), Comment::getKudos, one.getKudos() + 1)
                        .set(YesOrNoEnum.NO.getCode().equals(isAdd), Comment::getKudos, one.getKudos() - 1)
                );
            });
            if(call!=null && call){
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 兜底计算
        return  YesOrNoEnum.YES.getCode().equals(isAdd)
                ? backstopStrategyService.incrKudosComment(commentId)
                : backstopStrategyService.decrKudosComment(commentId);
    }

    // 回复点赞数加一或者减一
    @Override
    public Boolean changeReplyKudos(Long replyId, Integer isAdd) {

        String  changeLock = RedisPrefix.BLOG_REPLY_KUDOS_LOCK + replyId;

        RLock lock = redissonClient.getLock(changeLock);

        try {
            Boolean call = RetryConfig.LOCK_RETRYER.call(() -> {
                boolean getLock = lock.tryLock(1, -1, TimeUnit.SECONDS);
                if (!getLock) {
                    return false;
                }
                // 获取锁直接修改数据
                Reply one = replyService.getOne(Wrappers.<Reply>lambdaQuery()
                        .eq(Reply::getId, replyId)
                );
                if (one == null) {
                    log.error("查询的博客评论不存在，恢复的ID为:{}", replyId);
                    return false;
                }
                return replyService.update(Wrappers.<Reply>lambdaUpdate()
                        .eq(Reply::getId, replyId)
                        .set(YesOrNoEnum.YES.getCode().equals(isAdd), Reply::getKudos, one.getKudos() + 1)
                        .set(YesOrNoEnum.NO.getCode().equals(isAdd), Reply::getKudos, one.getKudos() - 1)
                );
            });

            if(call!=null && call){
                return true;
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (RetryException e){
            log.error("回复信息点赞数异常 ID:{}", replyId);
            throw new RuntimeException(e);
        }
        // 兜底计算
        return  YesOrNoEnum.YES.getCode().equals(isAdd)
                ? backstopStrategyService.incrKudosReply(replyId)
                : backstopStrategyService.decrKudosReply(replyId);

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
                .kudos(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .watch(String.valueOf(i.getWatch()))
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
                .kudos(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .watch(String.valueOf(i.getWatch()))
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
                .kudos(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .watch(String.valueOf(i.getWatch()))
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
                .kudos(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .watch(String.valueOf(i.getWatch()))
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
                try {
                    boolean tryLock = lock.tryLock(1, -1, TimeUnit.SECONDS); // 开启看门狗
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
                    return update(Wrappers.<Blog>lambdaUpdate()
                            .eq(Blog::getId, blogId)
                            .set(RedisPrefix.BLOG_START_LOCK.equals(prefix), Blog::getStar, one.getStar() + 1)
                            .set(RedisPrefix.BLOG_KUDOS_LOCK.equals(prefix), Blog::getKudos, one.getKudos() + 1)
                            .set(RedisPrefix.BLOG_COMMENT_LOCK.equals(prefix), Blog::getCommentNum, one.getCommentNum() + 1)
                    );
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
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
            Boolean forUpdate = RetryConfig.LOCK_RETRYER.call(() -> {
                try {
                    boolean tryLock = lock.tryLock(1, -1, TimeUnit.SECONDS); // 开启看门狗
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
                    return update(Wrappers.<Blog>lambdaUpdate()
                            .eq(Blog::getId, blogId)
                            .set(RedisPrefix.BLOG_START_LOCK.equals(prefix), Blog::getStar, one.getStar() - 1)
                            .set(RedisPrefix.BLOG_KUDOS_LOCK.equals(prefix), Blog::getKudos, one.getKudos() - 1)
                            .set(RedisPrefix.BLOG_COMMENT_LOCK.equals(prefix), Blog::getCommentNum, one.getCommentNum() - 1)
                    );
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
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
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        // 执行兜底
        return backstopStrategyService.incrBlog(blogId,prefix);
    }

}
