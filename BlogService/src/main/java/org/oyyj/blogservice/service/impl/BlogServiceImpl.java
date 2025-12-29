package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.blogservice.config.common.cf.ItemCF;
import org.oyyj.blogservice.config.common.cf.UserCF;
import org.oyyj.blogservice.config.pojo.BlogActivityLevel;
import org.oyyj.blogservice.dto.*;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.blogservice.mapper.TypeTableMapper;
import org.oyyj.blogservice.mapper.UserBehaviorMapper;
import org.oyyj.blogservice.pojo.*;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.service.IBlogTypeService;
import org.oyyj.blogservice.service.ICommentService;
import org.oyyj.blogservice.service.IReplyService;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.transform.Result;
import java.security.SecurityPermission;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 加载Lua脚本
    private static final DefaultRedisScript<Long> READ_COUNT_SCRIPT;

    static {
        READ_COUNT_SCRIPT = new DefaultRedisScript<>();
        READ_COUNT_SCRIPT.setScriptText(
                "local hasRead = redis.call('EXISTS', KEYS[2])\n" +
                        "if hasRead == 1 then\n" +
                        "    return 0\n" +
                        "end\n" +
                        "redis.call('SET', KEYS[2], 1, 'EX', ARGV[1])\n" +
                        "local count = redis.call('INCR', KEYS[1])\n" +
                        "return count"
        );
        READ_COUNT_SCRIPT.setResultType(Long.class);
    }



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
    public ReadDTO ReadBlog(Long id, String userInfoKey,Long userId) {
        // 获取 blog的主要信息
        ReadDTO blogInfo = getBlogInfo(id, userInfoKey);
        initBlogReadCount(id,blogInfo);
        incrementReadCount(id,userId);
/*取消自旋使用 redis + LUA脚本 */
        return blogInfo;
    }

    // 缓存中数据量增加


    // 使用分布式左 初始化博客阅读数量
    public void initBlogReadCount(Long blogId,ReadDTO readDTO) {

        if (readDTO!= null) {
            String countKey = "blog:read:count:" + blogId;
            // 使用 setIfAbsent 原子操作：仅当键不存在时才设置值
            // 返回 true 表示设置成功（首次初始化），false 表示键已存在（无需操作）
            redisTemplate.opsForValue().setIfAbsent(
                    countKey,
                    String.valueOf(readDTO.getWatch()),
                    7,  // 过期时间（根据业务设置，避免永久缓存）
                    TimeUnit.DAYS
            );
        }
    }


    // 从缓存中获取数据 并使用redisson处理缓存击穿
    public ReadDTO getBlogInfo( Long id, String userInfoKey) {
        String countKey = "blog:info:" + id;
        // 1. 先查缓存
        String blogJson = redisTemplate.opsForValue().get(countKey);
        if (Objects.nonNull(blogJson)) {
            try {
                if(blogJson.isEmpty()){
                    // 缓存中的是空字符串 （缓存穿透）
                    return null;
                }
                ReadDTO readDTO = objectMapper.readValue(blogJson, ReadDTO.class);
                return readDTO;
            } catch (Exception e) {
                log.warn("反序列化博客缓存失败，blogId: {}"+ id + ":" + e);
                // 缓存失效 直接删除
                redisTemplate.delete(countKey);
            }
        }

        // 缓存未命中  准备获取分布式锁
        String lockKey = "lock:blog:info:"+id;
        RLock lock = redissonClient.getLock(lockKey);
        ReadDTO result =  null;
        try {
            // 启动看门狗
            boolean isLocked = lock.tryLock(1, TimeUnit.SECONDS);
            if(isLocked ){
                try {
                    try {
                        // 再次检查避免 再处理锁的时候获取到数据了
                        String info = redisTemplate.opsForValue().get(countKey);
                        if(Objects.nonNull(info)){
                            if(info.isEmpty()){
                                return null;
                            }
                            return objectMapper.readValue(info,ReadDTO.class);
                        }

                    } catch (JsonProcessingException e) {
                        log.warn("反序列化博客缓存失败，blogId: {}"+ id + ":" + e);
                        // 缓存失效 直接删除
                        redisTemplate.delete(countKey);
                    }

                    // 查询数据库
                    result = getReadDTO( id, userInfoKey);

                    if(result != null){
                        redisTemplate.opsForValue().set(countKey, objectMapper.writeValueAsString(result), 30, TimeUnit.MINUTES);
                    }else{
                        redisTemplate.opsForValue().set(countKey, "", 30, TimeUnit.MINUTES);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    if(lock.isHeldByCurrentThread()){
                        lock.unlock();
                    }
                }
            }else{
                for (int i = 0; i < 3; i++) {
                    blogJson = redisTemplate.opsForValue().get(countKey);
                    if (Objects.nonNull(blogJson)) {
                        try {
                            if(blogJson.isEmpty()){
                                // 缓存中的是空字符串 （缓存穿透）
                                return null;
                            }
                            return objectMapper.readValue(blogJson, ReadDTO.class);
                        } catch (Exception e) {
                            log.warn("反序列化博客缓存失败，blogId: {}"+ id + ":" + e);
                            // 缓存失效 直接删除
                            redisTemplate.delete(countKey);
                        }
                        // 重复逻辑
                        isLocked = lock.tryLock(1, TimeUnit.SECONDS);
                        if(isLocked ){
                            try {
                                try {
                                    // 再次检查避免 再处理锁的时候获取到数据了
                                    String info = redisTemplate.opsForValue().get(countKey);
                                    if(Objects.nonNull(info)){
                                        if(info.isEmpty()){
                                            return null;
                                        }
                                        return objectMapper.readValue(info,ReadDTO.class);
                                    }

                                } catch (JsonProcessingException e) {
                                    log.warn("反序列化博客缓存失败，blogId: {}"+ id + ":" + e);
                                    // 缓存失效 直接删除
                                    redisTemplate.delete(countKey);
                                }

                                // 查询数据库
                                result = getReadDTO( id, userInfoKey);

                                if(result != null){
                                    redisTemplate.opsForValue().set(countKey, result.getWatch(), 30, TimeUnit.MINUTES);
                                }else{
                                    redisTemplate.opsForValue().set(countKey, "", 30, TimeUnit.MINUTES);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }finally {
                                if(lock.isHeldByCurrentThread()){
                                    lock.unlock();
                                }
                            }
                            break;
                        }

                    }
                }
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return result;
    }


    // 初始化博客内容
    public ReadDTO getReadDTO(Long id, String userInfoKey) {
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

        if (userInfoKey != null && !userInfoKey.isEmpty()) {
            System.out.println("userInfoKey:" + userInfoKey);
            Boolean userKudos = userFeign.isUserKudos(id, userInfoKey);
            if (userKudos) {
                readDTO.setIsUserKudos(userKudos);
            }
            if (userFeign.isUserStar(id, userInfoKey)) {
                readDTO.setIsUserStar(true);
            }
        }
        return readDTO;
    }

    /**
     * 增加阅读计数（使用Lua脚本保证原子性）
     */
    private Long incrementReadCount(Long blogId, Long userId) {
        String countKey = "blog:read:count:" + blogId;
        String userReadKey = "blog:user:read:" + blogId + ":" + userId;

        // 执行Lua脚本
        Long result = redisTemplate.execute(
                READ_COUNT_SCRIPT,
                Arrays.asList(countKey, userReadKey),
                String.valueOf(1800), // 30分钟过期时间
                String.valueOf(userId)
        );

        return result;
    }


    // 定期从redis中得到数据存储到数据库中 保证数据的一致性   同时也避免高并发导致的数据问题
    /**
     * 定期同步Redis阅读数到数据库
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void syncReadCountToDB() {
        Set<String> keys = redisTemplate.keys("blog:read:count:*");
        if (keys != null) {
            for (String key : keys) {
                Long blogId = Long.parseLong(key.substring("blog:read:count:".length()));
                String countStr = redisTemplate.opsForValue().get(key);
                if (countStr != null) {
                    Long count = Long.parseLong(countStr);

                    // 更新数据库
                    Blog blog = new Blog();
                    blog.setId(blogId);
                    blog.setWatch(count);
                    blogMapper.updateById(blog);
                }
            }
        }
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
            List<Long> blogIds = blogMapper.selectBlogIdRand();
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
    @Transactional
    public Boolean changeCommentKudos(Long commentId, Byte bytes) {
        Comment forUpdate = commentService.getOne(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getId, commentId)
                .last("for update")
        );
        if (bytes == 1) {
            // 1代表增加点赞数
            boolean update = commentService.update(Wrappers.<Comment>lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .set(Comment::getKudos, forUpdate.getKudos() + 1)
            );
            System.out.println(update);
            return update;
        } else if (bytes == 2) {
            boolean update = commentService.update(Wrappers.<Comment>lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .set(Comment::getKudos, forUpdate.getKudos() - 1)
            );
            System.out.println(update);
            return update;
        } else {
            log.error("改变类型错误");
            return null;
        }
    }

    // 回复点赞数加一或者减一
    @Override
    public Boolean changeReplyKudos(Long replyId, Byte bytes) {
        Reply forUpdate = replyService.getOne(Wrappers.<Reply>lambdaQuery()
                .eq(Reply::getId, replyId)
                .last("for update") // 悲观锁
        );
        if (bytes == 1) {
            // 1代表增加点赞数
            boolean update = replyService.update(Wrappers.<Reply>lambdaUpdate()
                    .eq(Reply::getId, replyId)
                    .set(Reply::getKudos, forUpdate.getKudos() + 1));
            System.out.println(update);
            return update;
        } else if (bytes == 2) {
            boolean update = replyService.update(Wrappers.<Reply>lambdaUpdate()
                    .eq(Reply::getId, replyId)
                    .set(Reply::getKudos, forUpdate.getKudos() - 1));
            System.out.println(update);
            return update;
        } else {
            log.error("改变类型错误");
            return null;
        }

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

    @Override
    public PageDTO<BlogDTO> getBlogByUserId(int current, int pageSize, Long userId) {

        if (Objects.isNull(userId)) {
            return null;
        }
        IPage<Blog> page = new Page<>(current, pageSize);
        List<Blog> list = list(page, Wrappers.<Blog>lambdaQuery().eq(Blog::getUserId, userId));
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

    @Override
    public PageDTO<BlogDTO> getBlogByTypeList(int current, int pageSize, List<String> typeList) {
        if (Objects.isNull(typeList)) {
            return null;
        }
        IPage<Blog> page = new Page<>(current, pageSize);

        List<TypeTable> typeTableList = typeTableMapper.selectList(Wrappers.<TypeTable>lambdaQuery().in(TypeTable::getName, typeList));
        if (typeTableList.isEmpty()) {
            return null;
        }
        List<Long> typeIds = typeTableList.stream().map(TypeTable::getId).toList();


        List<BlogType> blogTypeList = blogTypeService.list(Wrappers.<BlogType>lambdaQuery().in(BlogType::getTypeId, typeIds));
        List<Long> blogIds = blogTypeList.stream().map(BlogType::getBlogId).toList();
        if (blogIds.isEmpty()) {
            return null;
        }
        List<Blog> list = list(page, Wrappers.<Blog>lambdaQuery().in(Blog::getId, blogIds));
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

    private String getBlogStatus(Integer i){
        return switch (i) {
            case 1 -> "保存中";
            case 2 -> "发布";
            case 3 -> "审核中";
            case 4 -> "禁止查看";
            default -> {
                log.error("请求参数不正确" + i);
                yield null;
            }
        };
    }


}
