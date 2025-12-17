package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.blogservice.pojo.UserBehavior;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {
}
