package org.oyyj.chatservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.chatservice.pojo.MqMessageLog;

/**
 * MQ消息记录 Mapper 接口
 */
@Mapper
public interface MqMessageLogMapper extends BaseMapper<MqMessageLog> {

}