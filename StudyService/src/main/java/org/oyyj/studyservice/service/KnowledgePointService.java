package org.oyyj.studyservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgePointDTO;
import org.oyyj.studyservice.pojo.KnowledgePoint;

import java.util.List;

public interface KnowledgePointService extends IService<KnowledgePoint> {

    ResultUtil<String> add(KnowledgePointDTO knowledgePointDTO);

    ResultUtil<String> update(KnowledgePointDTO knowledgePointDTO);

    ResultUtil<String> deleteByIds(List<Long> ids);

    ResultUtil<Page<KnowledgePointDTO>> listAllKnowledgePoint(Integer page,
                                                              Integer pageSize,
                                                              Long baseId,
                                                              String level,
                                                              String tags,
                                                              String search);

    ResultUtil<List<KnowledgePointDTO>> listAllKnowledgePoint(Long baseId);
}
