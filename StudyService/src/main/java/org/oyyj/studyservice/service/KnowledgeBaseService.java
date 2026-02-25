package org.oyyj.studyservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgeBase.KnowledgeBaseDTO;
import org.oyyj.studyservice.pojo.KnowledgeBase;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    ResultUtil<String> add(KnowledgeBaseDTO dto);

    ResultUtil<String> update(KnowledgeBaseDTO dto);

    ResultUtil<String> delete(Long id);

    ResultUtil<IPage<KnowledgeBase>> pageQuery(
            int pageNum,
            int pageSize,
            String name
    );

    ResultUtil<String> removeKnowledgePoints(Long knowledgeBaseId, List<String> knowledgeIds);

    ResultUtil<String> addKnowledgePoints(Long knowledgeBaseId, List<String> knowledgeIds);
}
