package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.oyyj.blogservice.config.pojo.BlogActivityLevel;
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
     * 根据行为查询活跃度较高的用户信息
     * @return
     */
    List<UserActivityLevel> getUserActivityLevel();

    /**
     * 根据用户行为 选择被选中次数较多的博客
     * @param  userId 排除当前用户的信息
     * @return
     */
    List<BlogActivityLevel> getBlogActivityLevel(@Param("userId")  Long userId);


}
