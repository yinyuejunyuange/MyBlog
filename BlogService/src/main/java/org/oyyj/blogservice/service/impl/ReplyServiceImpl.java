package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.oyyj.blogservice.vo.reply.ReplyResultVO;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
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
    @Autowired
    private TransactionTemplate transactionTemplate;

    // 回复点赞数加一或者减一
    @Override
    public Boolean changeReplyKudos(Long replyId, Integer isAdd,LoginUser loginUser) {

        String  changeLock = RedisPrefix.BLOG_REPLY_KUDOS_LOCK + replyId;

        RLock lock = redissonClient.getLock(changeLock);

        try {
            Boolean call = RetryConfig.LOCK_RETRYER.call(() -> {
                try {
                    boolean getLock = lock.tryLock(1, 30, TimeUnit.SECONDS);
                    return transactionTemplate.execute(status -> {
                        // 定时30s解锁
                        if (!getLock) {
                            status.setRollbackOnly();
                            return false;
                        }
                        // 获取锁直接修改数据
                        Reply one = getOne(Wrappers.<Reply>lambdaQuery()
                                .eq(Reply::getId, replyId)
                        );
                        if (one == null) {
                            log.error("查询的博客评论不存在，恢复的ID为:{}", replyId);
                            status.setRollbackOnly();
                            return false;
                        }
                        if(Objects.equals(isAdd, YesOrNoEnum.NO.getCode()) && one.getKudos()<=0 ) {
                            log.error("查询的博客回复点赞数为0 不可减小，回复的ID为:{}", replyId);

                        }
                        boolean update = update(Wrappers.<Reply>lambdaUpdate()
                                .eq(Reply::getId, replyId)
                                .set(Reply::getUpdateTime,one.getUpdateTime()) // 保证有一个用于修改的避免报错
                                .set(YesOrNoEnum.YES.getCode().equals(isAdd), Reply::getKudos, one.getKudos() + 1)
                                .set(YesOrNoEnum.NO.getCode().equals(isAdd) && one.getKudos()>0 , Reply::getKudos, one.getKudos() - 1)
                        );
                        if(!update){
                            log.warn("回复信息{}处理失败",replyId);
                            status.setRollbackOnly();
                            return false;
                        }
                        boolean  userFeignResult;
                        if(Objects.equals(isAdd, YesOrNoEnum.YES.getCode())){
                            userFeignResult = userFeign.kudosReply(String.valueOf(replyId), loginUser.getUserId());
                        }else{
                            userFeignResult = userFeign.cancelKudosReply(String.valueOf(replyId), loginUser.getUserId());
                        }
                        if(!userFeignResult){
                            throw new RuntimeException("用户端处理回复失败{}");
                        }
                        return true;
                    });
                } catch (InterruptedException | TransactionException e) {
                    throw new RuntimeException(e);
                } finally {
                    if(lock.isLocked() && lock.isHeldByCurrentThread()){
                        lock.unlock();
                    }
                }
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
    public ReplyResultVO getReply(Long commentId, LoginUser loginUser, String lastId, Date lastTime) {
        LambdaQueryWrapper<Reply> wrapper = Wrappers.<Reply>lambdaQuery()
                .eq(Reply::getCommentId, commentId);
        if (lastTime != null && lastId != null) {
            wrapper.and(w -> w
                    .lt(Reply::getCreateTime, lastTime)
                    .or(o -> o
                            .eq(Reply::getCreateTime, lastTime)
                            .lt(Reply::getId, lastId)
                    )
            );
        }
        wrapper.orderByDesc(Reply::getCreateTime)
                .orderByDesc(Reply::getId)
                .last("limit " + pageSize);

        List<Reply> list = list(wrapper);
        ReplyResultVO replyResultVO = new ReplyResultVO();
        if (list.isEmpty()) {
            replyResultVO.setList(List.of());
            return replyResultVO;
        }
        // 通过list 获取回复的集合
        List<Long> replyIds = list.stream().map(Reply::getId).toList();
        List<Long> userLikeComments ;
        List<Reply> sortList = list.stream().sorted(Comparator.comparing(Reply::getUpdateTime).reversed()).toList();
        List<ReadReplyDTO> readReplyDTOList = new ArrayList<>();
        replyResultVO.setLastId(String.valueOf(sortList.getLast().getId()));
        replyResultVO.setLastTime(sortList.getLast().getUpdateTime());
        if (YesOrNoEnum.YES.getCode().equals(loginUser.getIsUserLogin())) {
            userLikeComments = userFeign.isUserLikeReply(replyIds, loginUser.getUserId());
            readReplyDTOList = sortList.stream().map(i -> ReadReplyDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .userId(String.valueOf(i.getUserId()))
                    .userName(i.getUserName())
                    .userImage(i.getUserImage())
                    .replyId(i.getRepliedId())
                    .replyName(i.getRepliedName())
                    .context(i.getContext())
                    .updateTime(formatDateToStr(i.getUpdateTime()))
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(userLikeComments.contains(i.getId()))
                    .isBelongUser(loginUser.getUserId().equals(i.getUserId()))
                    .build()
            ).toList();
        } else {
            readReplyDTOList = sortList.stream().map(i -> ReadReplyDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .userId(String.valueOf(i.getUserId()))
                    .userName(i.getUserName())
                    .userImage(i.getUserImage())
                    .replyId(i.getRepliedId())
                    .replyName(i.getRepliedName())
                    .context(i.getContext())
                    .updateTime( formatDateToStr(i.getUpdateTime()) )
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(false)
                    .isBelongUser(false)
                    .build()
            ).toList();
        }
        replyResultVO.setList(readReplyDTOList);
        return replyResultVO;
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
