package org.oyyj.userservice.mapper;

import com.github.jeffreyning.mybatisplus.base.MppBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.userservice.pojo.UserKudos;

@Mapper
public interface UserKudosMppMapper extends MppBaseMapper<UserKudos> {
}
