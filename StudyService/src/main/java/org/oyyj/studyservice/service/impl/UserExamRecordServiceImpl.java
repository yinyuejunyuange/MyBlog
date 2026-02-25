package org.oyyj.studyservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.studyservice.mapper.UserExamRecordMapper;
import org.oyyj.studyservice.pojo.UserExamRecord;
import org.oyyj.studyservice.service.UserExamRecordService;
import org.springframework.stereotype.Service;

@Service
public class UserExamRecordServiceImpl extends ServiceImpl<UserExamRecordMapper, UserExamRecord> implements UserExamRecordService {
}
