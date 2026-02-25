package org.oyyj.studyservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.question.QuestionDTO;
import org.oyyj.studyservice.service.QuestionService;
import org.oyyj.studyservice.vo.question.QuestionPageVO;
import org.oyyj.studyservice.vo.question.SubmitAnswerDTO;
import org.oyyj.studyservice.vo.question.SubmitResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/myBlog/question")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    // ---------- 基础增删改查 ----------

    /**
     * 新增试题
     */
    @PostMapping
    public ResultUtil<QuestionDTO> add(@RequestBody QuestionDTO questionDTO) {
        QuestionDTO result = questionService.addQuestion(questionDTO);
        return ResultUtil.success(result);
    }

    /**
     * 修改试题
     */
    @PutMapping
    public ResultUtil<QuestionDTO> update(@RequestBody QuestionDTO questionDTO) {
        QuestionDTO result = questionService.updateQuestion(questionDTO);
        return ResultUtil.success(result);
    }

    /**
     * 批量删除试题（路径传参，逗号分隔）
     * 示例：DELETE /api/question/1,2,3
     */
    @DeleteMapping("/{ids}")
    public ResultUtil<String> delete(@PathVariable List<Long> ids) {
        questionService.deleteQuestions(ids);
        return ResultUtil.success("删除成功");
    }

    /**
     * 根据ID查询试题
     */
    @GetMapping("/{id}")
    public ResultUtil<QuestionDTO> getById(@PathVariable Long id) {
        QuestionDTO result = questionService.getQuestionById(id);
        return result != null ? ResultUtil.success(result) : ResultUtil.fail("数据不存在");
    }

    /**
     * 分页条件查询试题
     * @param current 当前页码，默认1
     * @param size 每页大小，默认10
     * @param questionType 题型（可选）
     * @param keyword 关键词（题干或讲解模糊匹配）
     * @param knowledgePointId 知识点ID（可选）
     */
    @GetMapping("/page")
    public ResultUtil<Page<QuestionDTO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long knowledgePointId) {
        Page<QuestionDTO> page = questionService.getQuestionPage(current, size, questionType, keyword, knowledgePointId);
        return ResultUtil.success(page);
    }

    // ---------- 答题相关接口 ----------

    /**
     * 随机获取试题（用于答题）
     * 用户信息从请求头获取（示例：X-User-Id, X-User-Name），实际应从认证上下文获取
     * @param knowledgeBaseId 知识库ID（可选）
     * @param knowledgePointIds 知识点ID列表（可选，逗号分隔）
     * @param count 获取数量，默认10
     */
    @GetMapping("/random")
    public ResultUtil<QuestionPageVO> getRandomQuestions(
            @RequestParam(required = false) Long knowledgeBaseId,
            @RequestParam(required = false) List<Long> knowledgePointIds,
            @RequestParam(defaultValue = "10") Integer count,
            @RequestUser LoginUser loginUser) {
        QuestionPageVO result = questionService.getRandomQuestions(loginUser.getUserId(), knowledgeBaseId, knowledgePointIds, count);
        return ResultUtil.success(result);
    }

    /**
     * 提交答案并计算得分
     * 用户信息从请求头获取（示例：X-User-Id, X-User-Name）
     * @param submitDTO 提交的答案数据
     */
    @PostMapping("/submit")
    public ResultUtil<SubmitResultVO> submitAnswers(
            @RequestBody SubmitAnswerDTO submitDTO,
            @RequestUser LoginUser loginUser) {
        SubmitResultVO result = questionService.submitAnswers(loginUser.getUserId(),loginUser.getUserName(), submitDTO);
        return ResultUtil.success(result);
    }

}
