package org.oyyj.userservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.userservice.mapper.UserCommentMapper;
import org.oyyj.userservice.pojo.UserComment;
import org.oyyj.userservice.service.IUserCommentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserCommentServiceImpl extends MppServiceImpl<UserCommentMapper, UserComment> implements IUserCommentService {
    @Override
    public List<Long>isUserLikeComment(List<Long> replyIds, Long userId) {
        if( replyIds == null || replyIds.isEmpty()){
            log.error("查询回复时，回复ID 不可为空");
            return List.of();
        }
        if(userId == null){
            log.error("查询回复时，关注者ID 不可为空");
            return List.of();
        }

        return list(Wrappers.<UserComment>lambdaQuery()
                .eq(UserComment::getUserId, userId)
                .in(UserComment::getCommentId, replyIds)
                .select(UserComment::getCommentId)
        ).stream().map(UserComment::getCommentId).collect(Collectors.toList());
    }
}
