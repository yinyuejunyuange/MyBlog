package org.oyyj.taskservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jeffreyning.mybatisplus.base.MppBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.taskservice.pojo.AnnouncementUser;

@Mapper
public interface AnnouncementUserMapper extends MppBaseMapper<AnnouncementUser> {
}
