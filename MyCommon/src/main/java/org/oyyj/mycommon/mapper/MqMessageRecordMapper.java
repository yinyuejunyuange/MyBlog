package org.oyyj.mycommon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.mycommon.pojo.MqMessageRecord;


@Mapper
public interface MqMessageRecordMapper extends BaseMapper<MqMessageRecord> {
}
