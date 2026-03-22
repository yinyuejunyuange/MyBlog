package org.oyyj.studyservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgeBase.KnowledgeBaseDTO;
import org.oyyj.studyservice.pojo.KnowledgeBase;
import org.oyyj.studyservice.vo.knowledgeBase.BaseDashboardVO;
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

    ResultUtil<List<KnowledgeBaseDTO>> listKnowledgeBase(String type);

    ResultUtil<List<String>> listAllType();


    /**
     * 获取题库详情（包含关联的知识点信息）
     * @param knowledgeBaseId 题库ID
     * @return 题库详情
     */
    ResultUtil<KnowledgeBaseDTO> getKnowledgeBaseDetail(Long knowledgeBaseId);

    /**
     * 获取题库关联的知识点列表
     * @param knowledgeBaseId 题库ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 知识点列表
     */
    ResultUtil<IPage<KnowledgeBaseDTO>> getKnowledgePointsByBaseId(Long knowledgeBaseId, Integer page, Integer pageSize);

    /**
     * 获取首页知识库 信息相关接口
     * @return
     */
    ResultUtil<List<BaseDashboardVO>> getBaseDashboard();


}
