package org.oyyj.studyservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.studyservice.dto.question.QuestionDTO;
import org.oyyj.studyservice.pojo.Question;
import org.oyyj.studyservice.vo.question.QuestionPageVO;
import org.oyyj.studyservice.vo.question.SubmitAnswerDTO;
import org.oyyj.studyservice.vo.question.SubmitResultVO;

import java.util.List;

public interface QuestionService extends IService<Question> {

    // 新增试题
    QuestionDTO addQuestion(QuestionDTO questionDTO);
    // 修改试题
    QuestionDTO updateQuestion(QuestionDTO questionDTO);
    // 批量删除试题
    void deleteQuestions(List<Long> ids);
    // 根据ID查询试题
    QuestionDTO getQuestionById(Long id);
    // 分页条件查询试题
    Page<QuestionDTO> getQuestionPage(Integer current, Integer size, String questionType, String keyword, Long knowledgePointId);

    /**
     * 随机获取试题（用于答题）
     * @param userId 当前用户ID
     * @param knowledgeBaseId 知识库ID（可选），若提供则只返回该知识库下知识点关联的题目
     * @param knowledgePointIds 知识点ID列表（可选），若提供则只返回这些知识点下的题目（优先级高于knowledgeBaseId）
     * @param count 获取数量，默认10
     * @return 试题列表包装
     */
    QuestionPageVO getRandomQuestions(Long userId, Long knowledgeBaseId, List<Long> knowledgePointIds, Integer count);

    /**
     * 提交答案并计算得分
     * @param userId 用户ID
     * @param userName 用户名（冗余）
     * @param submitDTO 提交数据
     * @return 答题结果
     */
    SubmitResultVO submitAnswers(Long userId, String userName, SubmitAnswerDTO submitDTO);

}