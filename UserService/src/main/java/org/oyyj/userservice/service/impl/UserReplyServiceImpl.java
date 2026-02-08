package org.oyyj.userservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.userservice.mapper.UserReplyMapper;
import org.oyyj.userservice.pojo.UserReply;
import org.oyyj.userservice.service.IUserReplyService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserReplyServiceImpl extends MppServiceImpl<UserReplyMapper, UserReply> implements IUserReplyService {

    @Override
    public List<Long> isUserLikeReply(List<Long> replyIds, Long userId) {
        if( replyIds == null || replyIds.isEmpty()){
            log.error("查询回复时，回复ID 不可为空");
            return List.of();
        }
        if(userId == null){
            log.error("查询回复时，关注者ID 不可为空");
            return List.of();
        }

        return  list(Wrappers.<UserReply>lambdaQuery()
                .eq(UserReply::getUserId, userId)
                .in(UserReply::getReplyId, replyIds)
                .select(UserReply::getReplyId)
        ).stream().map(UserReply::getReplyId).collect(Collectors.toList());
    }
}
