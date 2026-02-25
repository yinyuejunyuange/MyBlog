package org.oyyj.studyservice.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.studyservice.dto.UserExamRecord.UserAnswerDetailDTO;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgeBaseRelationDTO;
import org.oyyj.studyservice.dto.question.QuestionDTO;
import org.oyyj.studyservice.mapper.KnowledgeBaseMapper;
import org.oyyj.studyservice.mapper.KnowledgePointMapper;
import org.oyyj.studyservice.mapper.QuestionMapper;
import org.oyyj.studyservice.mapper.UserExamRecordMapper;
import org.oyyj.studyservice.pojo.KnowledgeBase;
import org.oyyj.studyservice.pojo.KnowledgePoint;
import org.oyyj.studyservice.pojo.Question;
import org.oyyj.studyservice.pojo.UserExamRecord;
import org.oyyj.studyservice.service.QuestionService;
import org.oyyj.studyservice.vo.question.QuestionItemVO;
import org.oyyj.studyservice.vo.question.QuestionPageVO;
import org.oyyj.studyservice.vo.question.SubmitAnswerDTO;
import org.oyyj.studyservice.vo.question.SubmitResultVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {



    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserExamRecordMapper userExamRecordMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    public QuestionServiceImpl(UserExamRecordMapper userExamRecordMapper, KnowledgePointMapper knowledgePointMapper) {
        this.userExamRecordMapper = userExamRecordMapper;
        this.knowledgePointMapper = knowledgePointMapper;
    }


    // DTO 转实体（处理 JSON 字段）
    private Question dtoToEntity(QuestionDTO dto) {
        Question entity = new Question();
        BeanUtils.copyProperties(dto, entity);
        // 将 List<String> answer 转为 JSON 字符串
        if (dto.getAnswer() != null) {
            try {
                entity.setAnswer(objectMapper.writeValueAsString(dto.getAnswer()));
            } catch (JsonProcessingException e) {
                log.error("答案 JSON 序列化失败", e);
                throw new RuntimeException("答案格式错误");
            }
        }

        // options 字段在 DTO 中是 String，直接复制，无需转换（但也可以保证是 JSON）
        return entity;
    }



    @Override
    @Transactional
    public QuestionDTO addQuestion(QuestionDTO questionDTO) {
        // 参数校验（基础校验可以放在 DTO 上用注解，但这里可以再加）
        if (questionDTO.getQuestionType() == null || questionDTO.getQuestionText() == null) {
            throw new IllegalArgumentException("题型和题干不能为空");
        }
        Question entity = dtoToEntity(questionDTO);
        // 设置创建时间等可以在插入时由数据库自动生成，但如果有需要可以手动设置
        this.save(entity);
        questionDTO.setId(entity.getId());
        return questionDTO;
    }

    @Override
    @Transactional
    public QuestionDTO updateQuestion(QuestionDTO questionDTO) {
        if (questionDTO.getId() == null) {
            throw new IllegalArgumentException("更新时试题ID不能为空");
        }
        // 检查是否存在
        Question existing = this.getById(questionDTO.getId());
        if (existing == null) {
            throw new RuntimeException("试题不存在");
        }
        Question entity = dtoToEntity(questionDTO);
        // 更新时间由数据库自动更新
        this.updateById(entity);
        return questionDTO;
    }

    @Override
    @Transactional
    public void deleteQuestions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("删除ID列表不能为空");
        }
        this.removeByIds(ids);
    }

    @Override
    public QuestionDTO getQuestionById(Long id) {
        Question entity = this.getById(id);
        return entity != null ? entityToDto(entity) : null;
    }

    @Override
    public Page<QuestionDTO> getQuestionPage(Integer current, Integer size, String questionType, String keyword, Long knowledgePointId) {
        // 构建分页对象
        Page<Question> page = new Page<>(current, size);
        // 构建查询条件
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(questionType)) {
            wrapper.eq(Question::getQuestionType, questionType);
        }
        if (knowledgePointId != null) {
            wrapper.eq(Question::getKnowledgePointId, knowledgePointId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Question::getQuestionText, keyword)
                    .or().like(Question::getExplanation, keyword));
        }
        wrapper.orderByDesc(Question::getCreateTime);
        // 执行分页查询
        Page<Question> entityPage = this.page(page, wrapper);
        // 转换为 DTO 分页
        Page<QuestionDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<QuestionDTO> dtoList = entityPage.getRecords().stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    public QuestionPageVO getRandomQuestions(Long userId, Long knowledgeBaseId, List<Long> knowledgePointIds, Integer count) {
        if (count == null || count <= 0) count = 10;

        // 1. 确定查询的知识点ID列表
        List<Long> targetPointIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(knowledgePointIds)) {
            targetPointIds.addAll(knowledgePointIds);
        } else if (knowledgeBaseId != null) {
            // 根据知识库查询所有关联的知识点ID
            List<KnowledgeBaseRelationDTO> relations = knowledgePointMapper.selectKnowledgeBaseRelationByKnowledgeBaseId(knowledgeBaseId);
            targetPointIds = relations.stream()
                    .map(KnowledgeBaseRelationDTO::getKnowledgePointId)
                    .distinct()
                    .collect(Collectors.toList());
        }
        // 如果 targetPointIds 为空，则查询全部题目（全局随机）

        // 2. 构建查询条件
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getIsDelete, 0); // 只查未删除的
        if (!CollectionUtils.isEmpty(targetPointIds)) {
            wrapper.in(Question::getKnowledgePointId, targetPointIds);
        }
        // 随机获取 N 条（MySQL 使用 RAND()，数据量大时可能有性能问题，简单实现）
        wrapper.last("ORDER BY RAND() LIMIT " + count);

        // 3. 执行查询
        List<Question> questions = this.list(wrapper);

        // 4. 转换为 VO
        List<QuestionItemVO> itemVOS = questions.stream()
                .map(QuestionItemVO::convertQuestionItemVO)
                .collect(Collectors.toList());

        QuestionPageVO result = new QuestionPageVO();
        result.setUserId(String.valueOf(userId));
        result.setKnowledgeBaseId(String.valueOf(knowledgeBaseId));
        result.setQuestions(itemVOS);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitResultVO submitAnswers(Long userId, String userName, SubmitAnswerDTO submitDTO) {
        if (CollectionUtils.isEmpty(submitDTO.getAnswers())) {
            throw new IllegalArgumentException("答案列表不能为空");
        }

        Date now = new Date();

        // 1. 创建答题批次主记录
        UserExamRecord record = new UserExamRecord();
        record.setUserId(userId);
        record.setUserName(userName);
        record.setStartTime(now);
        record.setEndTime(now);
        record.setStatus(1); // 已完成
        record.setTotalScore(0); // 初始总分
        userExamRecordMapper.insert(record);

        // 2. 遍历每个答案，计算得分
        List<SubmitResultVO.QuestionResultVO> detailResults = new ArrayList<>();
        int totalScoreInt = 0; // 存储乘以100后的整数总分

        for (SubmitAnswerDTO.UserAnswerItem item : submitDTO.getAnswers()) {
            Long questionId = item.getQuestionId();
            List<String> userAnswer = item.getUserAnswer();

            // 获取题目信息
            Question question = this.getById(questionId);
            if (question == null) {
                log.warn("题目不存在，questionId={}", questionId);
                continue;
            }

            // 将 Question 转换为 QuestionDTO 以便使用 getScore 方法
            QuestionDTO dto = entityToDto(question); // 需要实现 entityToDto 方法

            // 计算得分比例
            BigDecimal scoreRatio = dto.getScore(userAnswer);
            // 乘以100得到整数得分（保留两位小数，四舍五入取整）
            int scoreInt = scoreRatio.multiply(new BigDecimal(100))
                    .setScale(0, RoundingMode.HALF_UP).intValue();

            // 判断是否正确（得分比例 > 0 即为正确）
            boolean isCorrect = scoreRatio.compareTo(BigDecimal.ZERO) > 0;

            // 创建明细记录
            UserAnswerDetailDTO detail = new UserAnswerDetailDTO();
            detail.setRecordId(record.getId());
            detail.setQuestionId(questionId);
            try {
                detail.setUserAnswer(objectMapper.writeValueAsString(userAnswer));
            } catch (JsonProcessingException e) {
                log.error("用户答案序列化失败", e);
                detail.setUserAnswer("[]");
            }
            detail.setIsCorrect(isCorrect ? 1 : 0);
            detail.setScore(scoreInt); // 存储整数
            userExamRecordMapper.insertUserAnswerDetail(detail);

            // 累计总分
            totalScoreInt += scoreInt;

            // 组装结果详情
            SubmitResultVO.QuestionResultVO resultVO = new SubmitResultVO.QuestionResultVO();
            resultVO.setQuestionId(questionId);
            resultVO.setIsCorrect(isCorrect);
            resultVO.setScore(scoreRatio); // 返回实际小数得分
            detailResults.add(resultVO);
        }

        // 3. 更新批次总分
        record.setTotalScore(totalScoreInt);
        userExamRecordMapper.updateById(record);

        // 4. 构造返回结果（总分转为小数）
        BigDecimal totalScore = new BigDecimal(totalScoreInt).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        SubmitResultVO result = new SubmitResultVO();
        result.setRecordId(record.getId());
        result.setTotalScore(totalScore);
        result.setDetails(detailResults);
        return result;
    }

    // 辅助方法：将 Question 转换为 QuestionDTO
    private QuestionDTO entityToDto(Question entity) {
        QuestionDTO dto = new QuestionDTO();
        BeanUtils.copyProperties(entity, dto);
        if (entity.getAnswer() != null && !entity.getAnswer().isEmpty()) {
            try {
                List<String> answerList = objectMapper.readValue(entity.getAnswer(), new TypeReference<List<String>>() {});
                dto.setAnswer(answerList);
            } catch (JsonProcessingException e) {
                log.error("答案解析失败", e);
                dto.setAnswer(List.of());
            }
        } else {
            dto.setAnswer(List.of());
        }
        return dto;
    }

}
