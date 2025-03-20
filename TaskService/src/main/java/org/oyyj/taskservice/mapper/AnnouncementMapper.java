package org.oyyj.taskservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.oyyj.taskservice.pojo.Announcement;

@Mapper
public interface AnnouncementMapper  extends BaseMapper<Announcement> {
}
