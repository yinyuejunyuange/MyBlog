package org.oyyj.blogservice.service;

import org.oyyj.blogservice.pojo.Blog;

import java.sql.Wrapper;

public interface IBackstopStrategyService {
    boolean incrBlog(Long blogId ,String prefix);

    boolean decrBlog(Long blogId ,String prefix);

    boolean incrKudosComment(Long commentId );

    boolean decrKudosComment(Long commentId);

    boolean incrKudosReply(Long replyId);

    boolean decrKudosReply(Long replyId);
}
