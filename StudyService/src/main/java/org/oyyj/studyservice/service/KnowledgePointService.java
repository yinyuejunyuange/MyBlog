package org.oyyj.studyservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgePointDTO;
import org.oyyj.studyservice.pojo.KnowledgePoint;

import java.util.List;

public interface KnowledgePointService extends IService<KnowledgePoint> {

    ResultUtil<String> add(KnowledgePointDTO knowledgePointDTO);

    ResultUtil<String> update(KnowledgePointDTO knowledgePointDTO);

    ResultUtil<String> deleteByIds(List<Long> ids);



}
