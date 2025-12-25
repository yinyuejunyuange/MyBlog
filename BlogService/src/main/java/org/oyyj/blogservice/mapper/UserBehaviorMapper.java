package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.blogservice.config.pojo.UserActivityLevel;
import org.oyyj.blogservice.pojo.UserBehavior;

import java.util.List;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {
    /**
     * 获取所有行为中的用户
     * @return
     */
    List<Long> getUserIdList();

    /**
     *
     * @return
     */
    List<UserActivityLevel> getUserActivityLevel();
}
