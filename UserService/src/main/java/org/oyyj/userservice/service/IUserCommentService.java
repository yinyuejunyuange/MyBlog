package org.oyyj.userservice.service;

import com.github.jeffreyning.mybatisplus.service.IMppService;
import org.oyyj.userservice.pojo.UserComment;

import java.util.List;

public interface IUserCommentService extends IMppService<UserComment> {
    /**
     * 用户是否点赞评论
     * @param commentIds
     * @param userId
     * @return
     */
    List<Long> isUserLikeComment(List<Long> commentIds, Long userId);
}
