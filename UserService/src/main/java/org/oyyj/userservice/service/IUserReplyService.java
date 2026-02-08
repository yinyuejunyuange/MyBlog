package org.oyyj.userservice.service;


import com.github.jeffreyning.mybatisplus.service.IMppService;
import org.oyyj.userservice.pojo.UserReply;

import java.util.List;

public interface IUserReplyService extends IMppService<UserReply> {
    /**
     * 用户是否点赞回复信息
     * @param replyIds
     * @param userId
     * @return
     */
    List<Long> isUserLikeReply(List<Long> replyIds, Long userId);
}
