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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                    .updateTime(i.getUpdateTime())
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(userLikeComments.contains(i.getUserId()))
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
                Comment one = getOne(Wrappers.<Comment>lambdaQuery()
                        .eq(Comment::getId, commentId)
                );
                if (one == null) {
                    log.error("查询的博客评论不存在，评论的ID为:{}", commentId);
                    return false;
                }
                return update(Wrappers.<Comment>lambdaUpdate()
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

}
