package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.dto.ReadCommentDTO;
import org.oyyj.blogservice.dto.ReadReplyDTO;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.mapper.CommentMapper;
import org.oyyj.blogservice.mapper.ReplyMapper;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.blogservice.service.IBackstopStrategyService;
import org.oyyj.blogservice.service.ICommentService;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {


    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private UserFeign userFeign;

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private IBackstopStrategyService backstopStrategyService; // 注入兜底服务

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final Integer pageSize = 20; // 每次查询的数量
    // 获取评论
    @Override
    public List<ReadCommentDTO> getBlogComment(String blogId, LoginUser loginUser ,Integer pageNum) {
        if(pageNum == null){
            pageNum = 1;
        }
        Page<Comment> page = new Page<>(pageNum,pageSize);
        List<Comment> list = list(page,Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getBlogId, Long.valueOf(blogId))
                .orderByDesc(Comment::getCreateTime)
        );
        if (list.isEmpty()) {
            return List.of();
        }
        // 通过list 获取回复的集合
        List<Long> commentIds = list.stream().map(Comment::getId).toList();
        List<Long> userLikeComments = userFeign.isUserLikeComments(commentIds, loginUser.getUserId());
        Long replyNum = replyMapper.selectCount(Wrappers.<Reply>lambdaQuery()
                .in(Reply::getCommentId, commentIds)
        );
        if (YesOrNoEnum.YES.getCode().equals(loginUser.getIsUserLogin())) {
            List<ReadCommentDTO> readCommentDTOS = list.stream().map(i -> ReadCommentDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .userId(String.valueOf(i.getUserId()))
                    .userName(i.getUserName())
                    .userImage(i.getUserImage())
                    .context(i.getContext())
                    .replyNum(Math.toIntExact(replyNum))
                    .updateTime(formatDateToStr(i.getUpdateTime()))
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(userLikeComments.contains(i.getId()))
                    .isBelongUser(loginUser.getUserId().equals(i.getUserId()))
                    .build()
            ).toList();

            System.out.println("查询成功:" + readCommentDTOS);
            return readCommentDTOS;
        } else {
            List<ReadCommentDTO> readCommentDTOS = list.stream().map(i -> ReadCommentDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .userId(String.valueOf(i.getUserId()))
                    .userName(i.getUserName())
                    .userImage(i.getUserImage())
                    .context(i.getContext())
                    .replyNum(Math.toIntExact(replyNum))
                    .updateTime(formatDateToStr(i.getUpdateTime()))
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(false)
                    .isBelongUser(false)
                    .build()
            ).toList();
            System.out.println("查询成功:" + readCommentDTOS);
            return readCommentDTOS;
        }
    }


    // 评论点赞数加一或者减一
    @Override
    public Boolean changeCommentKudos(Long commentId, Integer isAdd , LoginUser loginUser) {
        String changeLock = RedisPrefix.BLOG_COMMENT_KUDOS_LOCK + commentId;
        RLock lock = redissonClient.getLock(changeLock);
        try {
            boolean getLock = false;
            getLock = lock.tryLock(1, 30, TimeUnit.SECONDS); // 最长30s
            boolean finalGetLock = getLock;  // 先拿到锁再进入 retry中执行事务
            Boolean call = RetryConfig.LOCK_RETRYER.call(() -> transactionTemplate.execute(status -> {

                if (!finalGetLock) {
                    status.setRollbackOnly(); // 手动标记回滚
                    return false;
                }
                // 获取锁直接修改数据
                Comment one = getOne(Wrappers.<Comment>lambdaQuery()
                        .eq(Comment::getId, commentId)
                );
                if (one == null) {
                    log.error("查询的博客评论不存在，评论的ID为:{}", commentId);
                    status.setRollbackOnly(); // 手动标记回滚
                    return false;
                }
                boolean update = update(Wrappers.<Comment>lambdaUpdate()
                        .eq(Comment::getId, commentId)
                        .set(YesOrNoEnum.YES.getCode().equals(isAdd), Comment::getKudos, one.getKudos() + 1)
                        .set(YesOrNoEnum.NO.getCode().equals(isAdd), Comment::getKudos, one.getKudos() - 1)
                );
                if (!update) {
                    status.setRollbackOnly();
                    log.info("评论{}点赞数量增加失败",commentId);
                    return false;
                }
                boolean userFeignResult = true;
                // 调用用户服务接口 让用户添加加1/减1
                if (YesOrNoEnum.YES.getCode().equals(isAdd)) {
                    userFeignResult = userFeign.kudosComment(String.valueOf(commentId), loginUser.getUserId());
                } else {
                    userFeignResult = userFeign.cancelKudosComment(String.valueOf(commentId), loginUser.getUserId());
                }

                if (!userFeignResult) {
                    throw new RuntimeException("用户端数据处理失败回滚");
                }
                return update;
            }));
            if(call!=null && call){
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        // 兜底计算
        return  YesOrNoEnum.YES.getCode().equals(isAdd)
                ? backstopStrategyService.incrKudosComment(commentId)
                : backstopStrategyService.decrKudosComment(commentId);
    }

    /**
     * 将日期转换成字符串
     * @param date
     * @return
     */
    private String formatDateToStr(Date date) {
        // 创建SimpleDateFormat对象，指定格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // 格式化Date为字符串
        return sdf.format(date);
    }

}
