package org.oyyj.mycommon.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.mycommon.mapper.MqMessageRecordMapper;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommon.service.IMqMessageRecordService;
import org.springframework.stereotype.Service;

@Service
public class MqMessageRecordServiceImpl extends ServiceImpl<MqMessageRecordMapper, MqMessageRecord> implements IMqMessageRecordService {
}
