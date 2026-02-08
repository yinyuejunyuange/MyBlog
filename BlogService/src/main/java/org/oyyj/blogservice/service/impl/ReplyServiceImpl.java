package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.rholder.retry.RetryException;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.dto.ReadCommentDTO;
import org.oyyj.blogservice.dto.ReadReplyDTO;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.mapper.ReplyMapper;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.blogservice.service.IBackstopStrategyService;
import org.oyyj.blogservice.service.IReplyService;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ReplyServiceImpl extends ServiceImpl<ReplyMapper, Reply> implements IReplyService {


    @Autowired
    private IBackstopStrategyService backstopStrategyService; // 注入兜底服务

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private UserFeign userFeign;

    private final Integer pageSize = 20; // 每次查询的数量

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
                Reply one = getOne(Wrappers.<Reply>lambdaQuery()
                        .eq(Reply::getId, replyId)
                );
                if (one == null) {
                    log.error("查询的博客评论不存在，恢复的ID为:{}", replyId);
                    return false;
                }
                return update(Wrappers.<Reply>lambdaUpdate()
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

    @Override
    public List<ReadReplyDTO> getReply(Long commentId, LoginUser loginUser, Integer pageNum) {
        if(pageNum == null){
            pageNum = 1;
        }
        Page<Reply> page = new Page<>(pageNum,pageSize);
        List<Reply> list = list(page,Wrappers.<Reply>lambdaQuery()
                .eq(Reply::getCommentId, commentId)
                .orderByDesc(Reply::getCreateTime)
        );
        if (list.isEmpty()) {
            return List.of();
        }
        // 通过list 获取回复的集合
        List<Long> replyIds = list.stream().map(Reply::getId).toList();

        List<Long> userLikeComments ;
        if (YesOrNoEnum.YES.getCode().equals(loginUser.getIsUserLogin())) {
            userLikeComments = userFeign.isUserLikeReply(replyIds, loginUser.getUserId());
            return list.stream().map(i -> ReadReplyDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .userId(String.valueOf(i.getUserId()))
                    .userName(i.getUserName())
                    .userImage(i.getUserImage())
                    .replyId(i.getRepliedId())
                    .replyName(i.getRepliedName())
                    .context(i.getContext())
                    .updateTime(i.getUpdateTime())
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(userLikeComments.contains(i.getUserId()))
                    .build()
            ).toList();
        } else {
            return list.stream().map(i -> ReadReplyDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .userId(String.valueOf(i.getUserId()))
                    .userName(i.getUserName())
                    .userImage(i.getUserImage())
                    .replyId(i.getRepliedId())
                    .replyName(i.getRepliedName())
                    .context(i.getContext())
                    .updateTime(i.getUpdateTime())
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(false)
                    .build()
            ).toList();
        }
    }
}
