package org.oyyj.userservice.mapper;

import com.github.jeffreyning.mybatisplus.base.MppBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.userservice.pojo.UserComment;

@Mapper
public interface UserCommentMapper extends MppBaseMapper<UserComment> {
}
