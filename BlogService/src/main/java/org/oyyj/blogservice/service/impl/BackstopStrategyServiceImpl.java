package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.blogservice.mapper.CommentMapper;
import org.oyyj.blogservice.mapper.ReplyMapper;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.blogservice.service.IBackstopStrategyService;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class BackstopStrategyServiceImpl implements IBackstopStrategyService {

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ReplyMapper replyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrBlog(Long blogId ,String prefix) {
        log.info("执行for update悲观锁兜底逻辑，blogId：{}",blogId);
        Blog one = blogMapper.selectOne(Wrappers.<Blog>lambdaQuery()
                .eq(Blog::getId, blogId)
                .last("for update") // 悲观锁：锁定当前行，避免并发修改
        );
        if (one == null) {
            log.error("博客不存在（悲观锁兜底），blogId：{}", blogId);
            return false;
        }
        return blogMapper.update(Wrappers.<Blog>lambdaUpdate()
                .eq(Blog::getId, blogId)
                .set(RedisPrefix.BLOG_START_LOCK.equals(prefix), Blog::getStar, one.getStar() + 1)
                .set(RedisPrefix.BLOG_COMMENT_LOCK.equals(prefix), Blog::getCommentNum, one.getCommentNum() + 1)
                .set(RedisPrefix.BLOG_KUDOS_LOCK.equals(prefix), Blog::getKudos, one.getKudos() + 1)
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decrBlog(Long blogId ,String prefix) {
        log.info("执行for update悲观锁兜底逻辑，blogId：{}",blogId);
        Blog one = blogMapper.selectOne(Wrappers.<Blog>lambdaQuery()
                .eq(Blog::getId, blogId)
                .last("for update") // 悲观锁：锁定当前行，避免并发修改
        );
        if (one == null) {
            log.error("博客不存在（悲观锁兜底），blogId：{}", blogId);
            return false;
        }
        return blogMapper.update(Wrappers.<Blog>lambdaUpdate()
                .eq(Blog::getId, blogId)
                .set(RedisPrefix.BLOG_START_LOCK.equals(prefix), Blog::getStar, one.getStar() - 1)
                .set(RedisPrefix.BLOG_COMMENT_LOCK.equals(prefix), Blog::getCommentNum, one.getCommentNum() - 1)
                .set(RedisPrefix.BLOG_KUDOS_LOCK.equals(prefix), Blog::getKudos, one.getKudos() - 1)
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrKudosComment(Long commentId) {
        Comment forUpdate = commentMapper.selectOne(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getId, commentId)
                .last("for update")
        );
        return commentMapper.update(Wrappers.<Comment>lambdaUpdate()
                .eq(Comment::getId, commentId)
                .set(Comment::getKudos, forUpdate.getKudos() + 1)
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decrKudosComment(Long commentId) {
        Comment forUpdate = commentMapper.selectOne(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getId, commentId)
                .last("for update")
        );
        return commentMapper.update(Wrappers.<Comment>lambdaUpdate()
                .eq(Comment::getId, commentId)
                .set(Comment::getKudos, forUpdate.getKudos() - 1)
        ) > 0;
    }

    @Override
    public boolean incrKudosReply(Long replyId) {
        Reply forUpdate = replyMapper.selectOne(Wrappers.<Reply>lambdaQuery()
                .eq(Reply::getId, replyId)
                .last("for update") // 悲观锁
        );
        return  replyMapper.update(Wrappers.<Reply>lambdaUpdate()
                .eq(Reply::getId, replyId)
                .set(Reply::getKudos, forUpdate.getKudos() + 1)
        ) > 0 ;

    }

    @Override
    public boolean decrKudosReply(Long replyId) {
        Reply forUpdate = replyMapper.selectOne(Wrappers.<Reply>lambdaQuery()
                .eq(Reply::getId, replyId)
                .last("for update") // 悲观锁
        );
        return  replyMapper.update(Wrappers.<Reply>lambdaUpdate()
                .eq(Reply::getId, replyId)
                .set(Reply::getKudos, forUpdate.getKudos() - 1)
        ) > 0 ;
    }
}
