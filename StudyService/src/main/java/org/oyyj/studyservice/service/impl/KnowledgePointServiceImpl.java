package org.oyyj.studyservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgePoint.InterviewQuestionsDTO;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgeBaseRelationDTO;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgePointDTO;
import org.oyyj.studyservice.mapper.KnowledgeBaseMapper;
import org.oyyj.studyservice.mapper.KnowledgePointMapper;
import org.oyyj.studyservice.pojo.KnowledgeBase;
import org.oyyj.studyservice.pojo.KnowledgePoint;
import org.oyyj.studyservice.service.KnowledgePointService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgePointServiceImpl extends ServiceImpl<KnowledgePointMapper, KnowledgePoint> implements KnowledgePointService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    private final static ObjectMapper mapper = new ObjectMapper();

    private KnowledgePointDTO entityToDTO(KnowledgePoint entity) {
        KnowledgePointDTO knowledgePointDTO = new KnowledgePointDTO();
        BeanUtils.copyProperties(entity, knowledgePointDTO);
        knowledgePointDTO.setId(String.valueOf(entity.getId()));
        List<InterviewQuestionsDTO> interviewQuestionsDTOS = new ArrayList<>();
        try {
            if(entity.getRelatedQuestions()!=null){
                interviewQuestionsDTOS = mapper.readValue(entity.getRelatedQuestions(), new TypeReference<List<InterviewQuestionsDTO>>() {
                });
            }
        } catch (JsonProcessingException e) {
            log.error("知识点相关面试题转换失败，数据如下：{}",entity.getRelatedQuestions(),e);
        }
        knowledgePointDTO.setRelatedQuestions(interviewQuestionsDTOS);
        if(entity.getLevel() != null){
            KnowledgePoint.LevelEnum byValue = KnowledgePoint.LevelEnum.getByValue(entity.getLevel());
            if(byValue==null){
                log.error("难度等级不正确{}",entity.getLevel());
            }else{
                knowledgePointDTO.setLevel(byValue.getDesc());
            }
        }
        if(entity.getType()!=null && Strings.isNotBlank(entity.getType()) ){
            knowledgePointDTO.setType(Arrays.asList(entity.getType().split(",")));
        }

        return knowledgePointDTO;
    }

    private KnowledgePoint dtoToEntity(KnowledgePointDTO dto) {
        KnowledgePoint knowledgePoint = new KnowledgePoint();
        BeanUtils.copyProperties(dto, knowledgePoint);
        String str = null;
        try {
            str = mapper.writeValueAsString(dto.getRelatedQuestions());
        } catch (JsonProcessingException e) {
            log.error("知识点相关面试题转换成字符串失败，数据如下：{}",dto.getRelatedQuestions(),e);
        }
        knowledgePoint.setRelatedQuestions(str);
        KnowledgePoint.LevelEnum byValue = KnowledgePoint.LevelEnum.getByDesc(dto.getLevel());
        if(byValue==null){
            log.error("难度等级不正确{}",dto.getLevel());
        }else{
            knowledgePoint.setLevel(byValue.getValue());
        }
        if(dto.getType()!=null){
            knowledgePoint.setType(String.join(",", dto.getType()));
        }
        return knowledgePoint;
    }

    @Override
    public ResultUtil<String> add(KnowledgePointDTO knowledgePointDTO) {
        KnowledgePoint knowledgePoint = dtoToEntity(knowledgePointDTO);
        boolean save = save(knowledgePoint);
        return save?ResultUtil.success("知识点添加成功"):ResultUtil.fail("知识点添加失败");
    }

    @Override
    public ResultUtil<String> update(KnowledgePointDTO knowledgePointDTO) {
        if(knowledgePointDTO.getId()==null ){
            return ResultUtil.fail("修改失败，数据ID不可未空");
        }
        KnowledgePoint knowledgePoint = dtoToEntity(knowledgePointDTO);
        boolean update = saveOrUpdate(knowledgePoint);
        return update?ResultUtil.success("修改成功"):ResultUtil.fail("修改失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultUtil<String> deleteByIds(List<Long> ids) {
        boolean remove = removeByIds(ids);
        return remove?ResultUtil.success("删除成功"):ResultUtil.fail("删除失败");
    }

    @Override
    public ResultUtil<Page<KnowledgePointDTO>> listAllKnowledgePoint(Integer page, Integer pageSize, Long baseId, String level, String tags, String search) {

        if(page==null){
            page=1;
        }
        if(pageSize==null){
            pageSize=10;
        }

        KnowledgePoint.LevelEnum byDesc = KnowledgePoint.LevelEnum.getByDesc(level);
        Integer value = null;
        if(byDesc!=null){
            value = byDesc.getValue();
        }
        LambdaQueryWrapper<KnowledgePoint> wrapper = Wrappers.<KnowledgePoint>lambdaQuery()
                .like(Strings.isNotBlank(search), KnowledgePoint::getTitle, search)
                .eq(value != null, KnowledgePoint::getLevel, value);
        if(Strings.isNotBlank(tags)){
            String[] list = tags.split(",");
            for (String tag : list) {
                if("全部".equals(tag)){
                    continue;
                }
                wrapper.like(KnowledgePoint::getType,tag );
            }
        }

        if(baseId != null){
            List<KnowledgeBaseRelationDTO> knowledgeBaseRelationDTOS = baseMapper.selectKnowledgeBaseRelationByKnowledgeBaseId(baseId);
            if(knowledgeBaseRelationDTOS!=null&& !knowledgeBaseRelationDTOS.isEmpty()){
                List<Long> pointIds = knowledgeBaseRelationDTOS.stream().map(KnowledgeBaseRelationDTO::getKnowledgePointId).toList();
                wrapper.in(KnowledgePoint::getId, pointIds);
            }
        }

        wrapper.select(KnowledgePoint::getId,
                KnowledgePoint::getTitle,
                KnowledgePoint::getLevel,
                KnowledgePoint::getType);

        Page<KnowledgePoint> pageInfo = new Page<>(page, pageSize);
        List<KnowledgePointDTO> list = list(pageInfo, wrapper).stream().map(this::entityToDTO).toList();
        Page<KnowledgePointDTO> result = new Page<>();
        result.setTotal(pageInfo.getTotal());
        result.setRecords(list);
        return ResultUtil.success(result);
    }

    @Override
    public ResultUtil<List<KnowledgePointDTO>> listAllKnowledgePoint(Long baseId) {
        List<KnowledgeBaseRelationDTO> knowledgeBaseRelationDTOS = baseMapper.selectKnowledgeBaseRelationByKnowledgeBaseId(baseId);
        List<Long> pointIds = new ArrayList<>();
        if(knowledgeBaseRelationDTOS!=null&& !knowledgeBaseRelationDTOS.isEmpty()){
            pointIds = knowledgeBaseRelationDTOS.stream().map(KnowledgeBaseRelationDTO::getKnowledgePointId).toList();
        }

        if(pointIds.isEmpty()){
            return ResultUtil.success(List.of());
        }
        List<KnowledgePoint> list = list(Wrappers.<KnowledgePoint>lambdaQuery()
                .in(KnowledgePoint::getId, pointIds)
                .select(KnowledgePoint::getId, KnowledgePoint::getTitle)
        );

        return ResultUtil.success(list.stream().map(this::entityToDTO).toList());
    }


}
