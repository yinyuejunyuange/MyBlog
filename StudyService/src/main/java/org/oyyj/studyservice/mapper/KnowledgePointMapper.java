package org.oyyj.studyservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgeBaseRelationDTO;
import org.oyyj.studyservice.pojo.KnowledgePoint;

import java.util.List;

@Mapper
public interface KnowledgePointMapper extends BaseMapper<KnowledgePoint> {

    // 根据知识库ID查询未删除的关联记录
    List<KnowledgeBaseRelationDTO> selectKnowledgeBaseRelationByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    // 插入一条关联记录
    int insertKnowledgeBaseRelation(KnowledgeBaseRelationDTO relation);

    // 根据ID更新（逻辑删除或修改）
    int updateKnowledgeBaseRelationById(KnowledgeBaseRelationDTO relation);

    // 根据ID删除（物理删除）
    int deleteKnowledgeBaseRelationById(Long id);

    // 插入关联数据
    int insertRelateByIds(@Param("pointIds") List<Long> pointIds, @Param("baseId") Long baseId );

    int deleteRelationByIds(@Param("pointIds") List<Long> pointIds, @Param("baseId") Long baseId );
}
