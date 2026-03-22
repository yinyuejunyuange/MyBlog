package org.oyyj.studyservice.service.impl;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.checkerframework.checker.units.qual.K;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgeBase.KnowledgeBaseDTO;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgeBaseRelationDTO;
import org.oyyj.studyservice.mapper.ChatMessageMapper;
import org.oyyj.studyservice.mapper.KnowledgeBaseMapper;
import org.oyyj.studyservice.mapper.KnowledgePointMapper;
import org.oyyj.studyservice.mapper.QuestionMapper;
import org.oyyj.studyservice.pojo.ChatMessage;
import org.oyyj.studyservice.pojo.KnowledgeBase;
import org.oyyj.studyservice.pojo.KnowledgePoint;
import org.oyyj.studyservice.pojo.Question;
import org.oyyj.studyservice.service.KnowledgeBaseService;
import org.oyyj.studyservice.vo.knowledgeBase.BaseDashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;
    @Autowired
    private QuestionMapper questionMapper;
    private ChatMessageMapper chatMessageMapper;

    @Override
    public ResultUtil<String> add(KnowledgeBaseDTO dto) {
        KnowledgeBase knowledgeBase = dto.dtoToEntity(dto);
        boolean save = save(knowledgeBase);
        if(save){
            List<String> knowledgeIds = dto.getKnowledgeIds();
            List<Long> list = new java.util.ArrayList<>(knowledgeIds.stream().map(Long::parseLong).toList());
            if(list.isEmpty()){
                return ResultUtil.success("知识库添加成功");
            }
            int i = knowledgePointMapper.insertRelateByIds(list, knowledgeBase.getId());
            if(i != knowledgeIds.size()){
                List<Long> existIds = knowledgePointMapper.selectKnowledgeBaseRelationByKnowledgeBaseId(knowledgeBase.getId())
                        .stream().map(KnowledgeBaseRelationDTO::getId).toList();
                list.removeAll(existIds);
                return ResultUtil.success("添加成功，但有以下数据没有成功关联: "+list.stream().map(String::valueOf).collect(Collectors.joining(",")));
            }
        }
        return ResultUtil.success("添加成功");
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultUtil<String> update(KnowledgeBaseDTO dto) {
        if (dto.getId() == null) {
            return ResultUtil.fail("知识库ID不能为空");
        }

        KnowledgeBase entity = dto.dtoToEntity(dto);
        entity.setUpdateTime(new Date());

        boolean updated = this.updateById(entity);
        return updated
                ? ResultUtil.success("修改成功")
                : ResultUtil.fail("修改失败");
    }

    /**
     * 删除知识库（逻辑删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultUtil<String> delete(Long id) {
        if (id == null) {
            return ResultUtil.fail("ID不能为空");
        }

        boolean removed = this.removeById(id);
        return removed
                ? ResultUtil.success("删除成功")
                : ResultUtil.fail("删除失败");
    }

    /**
     * 分页查询
     */
    @Override
    public ResultUtil<IPage<KnowledgeBase>> pageQuery(
            int pageNum,
            int pageSize,
            String name
    ) {
        Page<KnowledgeBase> page = new Page<>(pageNum, pageSize);

        IPage<KnowledgeBase> result = this.page(
                page,
                Wrappers.<KnowledgeBase>lambdaQuery()
                        .like(StringUtils.hasText(name), KnowledgeBase::getName, name)
                        .orderByDesc(KnowledgeBase::getCreateTime)
        );

        return ResultUtil.success(result);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultUtil<String> addKnowledgePoints(Long knowledgeBaseId, List<String> knowledgeIds) {

        if (knowledgeBaseId == null) {
            return ResultUtil.fail("知识库ID不能为空");
        }

        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return ResultUtil.success("未添加任何知识点");
        }

        // 1. 字符串 ID -> Long
        List<Long> pointIdList;
        try {
            pointIdList = knowledgeIds.stream()
                    .map(Long::parseLong)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (NumberFormatException e) {
            return ResultUtil.fail("知识点ID格式错误");
        }

        if (pointIdList.isEmpty()) {
            return ResultUtil.success("未添加任何知识点");
        }

        // 2. 批量插入（INSERT INTO ... SELECT）
        int insertCount = knowledgePointMapper.insertRelateByIds(
                pointIdList,
                knowledgeBaseId
        );

        // 3. 全部成功
        if (insertCount == pointIdList.size()) {
            return ResultUtil.success("知识点添加成功");
        }

        // 4. 查询当前已成功关联的
        List<Long> successIds = knowledgePointMapper
                .selectKnowledgeBaseRelationByKnowledgeBaseId(knowledgeBaseId)
                .stream()
                .map(KnowledgeBaseRelationDTO::getKnowledgePointId)
                .toList();

        // 5. 计算失败的
        List<Long> failIds = new ArrayList<>(pointIdList);
        failIds.removeAll(successIds);

        if (!failIds.isEmpty()) {
            return ResultUtil.success(
                    "添加成功，但有以下知识点未成功关联: " +
                            failIds.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(","))
            );
        }

        return ResultUtil.success("知识点添加成功");
    }

    @Override
    public ResultUtil<List<KnowledgeBaseDTO>> listKnowledgeBase(String type) {
        List<KnowledgeBaseDTO> list = list(Wrappers.<KnowledgeBase>lambdaQuery()
                .like(StringUtils.hasText(type), KnowledgeBase::getCategory, type)
        ).stream().map(item -> new KnowledgeBaseDTO().entityToDTO(item)).toList();

        return ResultUtil.success(list);
    }

    @Override
    public ResultUtil<List<String>> listAllType() {
        List<String> strings = new ArrayList<>();
        strings.add("全部");
        strings.addAll(baseMapper.listAllBaseTypes());

        return ResultUtil.success(strings);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultUtil<String> removeKnowledgePoints(Long knowledgeBaseId, List<String> knowledgeIds) {

        if (knowledgeBaseId == null) {
            return ResultUtil.fail("知识库ID不能为空");
        }

        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return ResultUtil.success("未移除任何知识点");
        }

        List<Long> pointIdList;
        try {
            pointIdList = knowledgeIds.stream()
                    .map(Long::parseLong)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (NumberFormatException e) {
            return ResultUtil.fail("知识点ID格式错误");
        }

        if (pointIdList.isEmpty()) {
            return ResultUtil.success("未移除任何知识点");
        }

        // 1. 删除关系
        int deleteCount = knowledgePointMapper.deleteRelationByIds(
                pointIdList,
                knowledgeBaseId
        );

        // 2. 全部删除成功
        if (deleteCount == pointIdList.size()) {
            return ResultUtil.success("知识点移除成功");
        }

        // 3. 查询当前仍然存在的关联
        List<Long> stillExistIds = knowledgePointMapper
                .selectKnowledgeBaseRelationByKnowledgeBaseId(knowledgeBaseId)
                .stream()
                .map(KnowledgeBaseRelationDTO::getKnowledgePointId)
                .collect(Collectors.toList());

        // 4. 实际未移除的
        List<Long> failIds = new ArrayList<>(pointIdList);
        failIds.retainAll(stillExistIds);

        if (!failIds.isEmpty()) {
            return ResultUtil.success(
                    "移除完成，但以下知识点未成功移除: " +
                            failIds.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(","))
            );
        }

        return ResultUtil.success("知识点移除成功");
    }

    @Override
    public ResultUtil<KnowledgeBaseDTO> getKnowledgeBaseDetail(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return ResultUtil.fail("题库ID不能为空");
        }

        KnowledgeBase knowledgeBase = this.getById(knowledgeBaseId);
        if (knowledgeBase == null) {
            return ResultUtil.fail("题库不存在");
        }

        KnowledgeBaseDTO dto = new KnowledgeBaseDTO().entityToDTO(knowledgeBase);

        List<KnowledgeBaseRelationDTO> relations = knowledgePointMapper
                .selectKnowledgeBaseRelationByKnowledgeBaseId(knowledgeBaseId);

        if (relations != null && !relations.isEmpty()) {
            List<String> knowledgeIds = relations.stream()
                    .map(relation -> String.valueOf(relation.getKnowledgePointId()))
                    .collect(Collectors.toList());
            dto.setKnowledgeIds(knowledgeIds);
        }

        return ResultUtil.success(dto);
    }

    @Override
    public ResultUtil<IPage<KnowledgeBaseDTO>> getKnowledgePointsByBaseId(Long knowledgeBaseId, Integer page, Integer pageSize) {
        if (knowledgeBaseId == null) {
            return ResultUtil.fail("题库ID不能为空");
        }

        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        KnowledgeBase knowledgeBase = this.getById(knowledgeBaseId);
        if (knowledgeBase == null) {
            return ResultUtil.fail("题库不存在");
        }

        List<KnowledgeBaseRelationDTO> relations = knowledgePointMapper
                .selectKnowledgeBaseRelationByKnowledgeBaseId(knowledgeBaseId);

        Page<KnowledgeBaseDTO> resultPage = new Page<>(page, pageSize, 0);

        if (relations == null || relations.isEmpty()) {
            resultPage.setRecords(new ArrayList<>());
            return ResultUtil.success(resultPage);
        }

        List<Long> pointIds = relations.stream()
                .map(KnowledgeBaseRelationDTO::getKnowledgePointId)
                .collect(Collectors.toList());

        int total = pointIds.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        if (start >= total) {
            resultPage.setRecords(new ArrayList<>());
            resultPage.setTotal(total);
            return ResultUtil.success(resultPage);
        }

        List<Long> pagePointIds = pointIds.subList(start, end);

        List<KnowledgePoint> knowledgePoints = knowledgePointMapper.selectBatchIds(pagePointIds);

        List<KnowledgeBaseDTO> dtoList = knowledgePoints.stream()
                .map(point -> {
                    KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
                    dto.setId(String.valueOf(point.getId()));
                    dto.setName(point.getTitle());
                    dto.setDescription(point.getRecommendedAnswer());
                    dto.setCreateTime(point.getCreateTime());
                    dto.setUpdateTime(point.getUpdateTime());
                    return dto;
                })
                .collect(Collectors.toList());

        resultPage.setRecords(dtoList);
        resultPage.setTotal(total);

        return ResultUtil.success(resultPage);
    }

    @Override
    public ResultUtil<List<BaseDashboardVO>> getBaseDashboard() {
        List<BaseDashboardVO> result = new ArrayList<>();
        List<KnowledgeBase> list = list(Wrappers.<KnowledgeBase>lambdaQuery()
                .select(KnowledgeBase::getId, KnowledgeBase::getName)
        );
        if (list == null || list.isEmpty()) {
            return ResultUtil.success(result);
        }
        List<Long> baseIds = list.stream().map(KnowledgeBase::getId).toList();
        List<KnowledgeBaseRelationDTO> knowledgeBaseRelationDTOS = knowledgePointMapper.selectKnowledgeBaseRelationByKnowledgeBaseIds(baseIds);
        Map<Long, List<KnowledgeBaseRelationDTO>> basePointMap = knowledgeBaseRelationDTOS.stream().collect(Collectors.groupingBy(KnowledgeBaseRelationDTO::getKnowledgeBaseId));
        List<Long> pointIds = knowledgeBaseRelationDTOS.stream().map(KnowledgeBaseRelationDTO::getKnowledgePointId).toList();
        List<Question> questions = questionMapper.selectList(Wrappers.<Question>lambdaQuery()
                .in(Question::getKnowledgePointId, pointIds)
                .select(Question::getId, Question::getKnowledgePointId)
        );
        Map<Long, List<Question>> questionsMap = questions.stream().collect(Collectors.groupingBy(Question::getKnowledgePointId));
        List<ChatMessage> chatMessages = chatMessageMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .select(ChatMessage::getKnowledgeBaseId, ChatMessage::getId, ChatMessage::getSessionId)
        );
        Map<Long, List<ChatMessage>> chatMessageMap = chatMessages.stream().collect(Collectors.groupingBy(ChatMessage::getKnowledgeBaseId));
        for (KnowledgeBase knowledgeBase : list) {
            BaseDashboardVO baseDashboardVO = new BaseDashboardVO();
            baseDashboardVO.setBaseName(knowledgeBase.getName());
            if(basePointMap.containsKey(knowledgeBase.getId())) {
                baseDashboardVO.setPointsNum(basePointMap.get(knowledgeBase.getId()).size());
            }
            if(questionsMap.containsKey(knowledgeBase.getId())) {
                baseDashboardVO.setQuestions(questionsMap.get(knowledgeBase.getId()).size());
            }
            if(chatMessageMap.containsKey(knowledgeBase.getId())) {
                List<ChatMessage> chatMessagesList = chatMessageMap.get(knowledgeBase.getId());
                Set<String> msgSet = chatMessagesList.stream().map(ChatMessage::getSessionId).collect(Collectors.toSet());
                baseDashboardVO.setInterviewNum(msgSet.size());
            }
            result.add(baseDashboardVO);
        }
        return ResultUtil.success(result);
    }
}
