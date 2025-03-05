package org.oyyj.aichatdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.aichatdemo.mapper.AIFileMapper;
import org.oyyj.aichatdemo.pojo.AIFile;
import org.oyyj.aichatdemo.service.IAIFileService;
import org.springframework.stereotype.Service;

@Service
public class AIFileServiceImpl extends ServiceImpl<AIFileMapper, AIFile> implements IAIFileService {
}
