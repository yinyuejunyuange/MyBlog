package org.oyyj.studyservice.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.annotation.RequestRole;
import org.oyyj.mycommonbase.common.RoleEnum;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgeBaseRelationDTO;
import org.oyyj.studyservice.dto.question.QuestionDTO;
import org.oyyj.studyservice.mapper.KnowledgePointMapper;
import org.oyyj.studyservice.pojo.KnowledgeBase;
import org.oyyj.studyservice.pojo.KnowledgePoint;
import org.oyyj.studyservice.pojo.Question;
import org.oyyj.studyservice.service.KnowledgeBaseService;
import org.oyyj.studyservice.service.KnowledgePointService;
import org.oyyj.studyservice.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/myBlog/question/admin")
@Slf4j
public class QuestionAdminController {

    @Autowired
    private QuestionService questionService;

    // todo

    // 新增问题

    // 修改问题信息

    // 分页查看所有问题(可以通过知识库搜索 通过知识点搜索 )

    // 查看问题详情 (展示关联的知识点 和 知识库)

    // 关联知识点 knowledgePoint

    @Autowired
    private KnowledgePointService knowledgePointService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    /**
     * 新增试题
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping("/add")
    public ResultUtil<QuestionDTO> addQuestion(@RequestBody QuestionDTO questionDTO) throws AuthenticationException {
        

        if (questionDTO.getQuestionType() == null || questionDTO.getQuestionType().isEmpty()) {
            return ResultUtil.fail("题型不能为空");
        }

        if (questionDTO.getQuestionText() == null || questionDTO.getQuestionText().isEmpty()) {
            return ResultUtil.fail("题干不能为空");
        }

        try {
            QuestionDTO addedQuestion = questionService.addQuestion(questionDTO);
            return ResultUtil.success(addedQuestion);
        } catch (Exception e) {
            log.error("添加试题失败", e);
            return ResultUtil.fail("试题添加失败：" + e.getMessage());
        }
    }

    /**
     * 修改试题信息
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/update")
    public ResultUtil<QuestionDTO> updateQuestion(@RequestBody QuestionDTO questionDTO) throws AuthenticationException {
        

        if (questionDTO.getId() == null) {
            return ResultUtil.fail("试题ID不能为空");
        }

        try {
            QuestionDTO updatedQuestion = questionService.updateQuestion(questionDTO);
            return ResultUtil.success(updatedQuestion);
        } catch (Exception e) {
            log.error("修改试题失败", e);
            return ResultUtil.fail("试题修改失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除试题
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @DeleteMapping("/delete")
    public ResultUtil<String> deleteQuestions(@RequestBody List<Long> ids) throws AuthenticationException {
        

        if (ids == null || ids.isEmpty()) {
            return ResultUtil.fail("试题ID列表不能为空");
        }

        try {
            questionService.deleteQuestions(ids);
            return ResultUtil.success("删除成功");
        } catch (Exception e) {
            log.error("删除试题失败", e);
            return ResultUtil.fail("删除失败：" + e.getMessage());
        }
    }

    /**
     * 分页查看所有问题
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/list")
    public ResultUtil<Page<QuestionDTO>> listQuestions(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "questionType", required = false) String questionType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "knowledgePointId", required = false) Long knowledgePointId,
            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId) throws AuthenticationException {
        

        Page<QuestionDTO> questionPage;

        if (knowledgeBaseId != null && knowledgePointId == null) {
            List<KnowledgeBaseRelationDTO> relations = knowledgePointMapper
                    .selectKnowledgeBaseRelationByKnowledgeBaseId(knowledgeBaseId);

            if (relations == null || relations.isEmpty()) {
                Page<QuestionDTO> emptyPage = new Page<>(page, pageSize, 0);
                emptyPage.setRecords(List.of());
                return ResultUtil.success(emptyPage);
            }

            List<Long> pointIds = relations.stream()
                    .map(KnowledgeBaseRelationDTO::getKnowledgePointId)
                    .toList();

            questionPage = new Page<>(page, pageSize, 0);
            List<QuestionDTO> allQuestions = new java.util.ArrayList<>();

            for (Long pointId : pointIds) {
                Page<QuestionDTO> tempPage = questionService.getQuestionPage(page, pageSize, questionType, keyword, pointId);
                allQuestions.addAll(tempPage.getRecords());
            }

            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, allQuestions.size());

            if (start < allQuestions.size()) {
                questionPage.setRecords(allQuestions.subList(start, end));
                questionPage.setTotal(allQuestions.size());
            } else {
                questionPage.setRecords(List.of());
                questionPage.setTotal(0);
            }
        } else {
            questionPage = questionService.getQuestionPage(page, pageSize, questionType, keyword, knowledgePointId);
        }

        return ResultUtil.success(questionPage);
    }

    /**
     * 查看问题详情
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/detail")
    public ResultUtil<Map<String, Object>> getQuestionDetail(@RequestParam("id") Long id) throws AuthenticationException {
        

        QuestionDTO questionDTO = questionService.getQuestionById(id);
        if (questionDTO == null) {
            return ResultUtil.fail("试题不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("question", questionDTO);

        if (questionDTO.getKnowledgePointId() != null) {
            KnowledgePoint knowledgePoint = knowledgePointService.getById(questionDTO.getKnowledgePointId());
            if (knowledgePoint != null) {
                Map<String, Object> pointInfo = new HashMap<>();
                pointInfo.put("id", knowledgePoint.getId());
                pointInfo.put("title", knowledgePoint.getTitle());
                pointInfo.put("level", knowledgePoint.getLevel());
                result.put("knowledgePoint", pointInfo);

                List<KnowledgeBaseRelationDTO> relations = knowledgePointMapper
                        .selectKnowledgeBaseRelationByKnowledgePointId(knowledgePoint.getId());

                if (relations != null && !relations.isEmpty()) {
                    List<Map<String, Object>> baseList = relations.stream()
                            .map(relation -> {
                                KnowledgeBase base = knowledgeBaseService.getById(relation.getKnowledgeBaseId());
                                if (base != null) {
                                    Map<String, Object> baseInfo = new HashMap<>();
                                    baseInfo.put("id", base.getId());
                                    baseInfo.put("name", base.getName());
                                    baseInfo.put("types", base.getCategory()); // 知识点相关类别
                                    return baseInfo;
                                }
                                return null;
                            })
                            .filter(java.util.Objects::nonNull)
                            .toList();

                    result.put("knowledgeBases", baseList);
                }
            }
        }

        return ResultUtil.success(result);
    }

    /**
     * 关联知识点
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping("/relateKnowledgePoint")
    public ResultUtil<String> relateKnowledgePoint(
            @RequestParam("questionId") Long questionId,
            @RequestParam("knowledgePointId") Long knowledgePointId) throws AuthenticationException {
        

        Question question = questionService.getById(questionId);
        if (question == null) {
            return ResultUtil.fail("试题不存在");
        }

        KnowledgePoint knowledgePoint = knowledgePointService.getById(knowledgePointId);
        if (knowledgePoint == null) {
            return ResultUtil.fail("知识点不存在");
        }

        question.setKnowledgePointId(knowledgePointId);
        boolean update = questionService.updateById(question);

        if (update) {
            return ResultUtil.success("关联知识点成功");
        } else {
            return ResultUtil.fail("关联知识点失败");
        }
    }

    /**
     * 取消关联知识点
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @DeleteMapping("/unrelateKnowledgePoint")
    public ResultUtil<String> unrelateKnowledgePoint(
            @RequestParam("questionId") Long questionId) throws AuthenticationException {


        Question question = questionService.getById(questionId);
        if (question == null) {
            return ResultUtil.fail("试题不存在");
        }

        question.setKnowledgePointId(null);
        boolean update = questionService.updateById(question);

        if (update) {
            return ResultUtil.success("取消关联成功");
        } else {
            return ResultUtil.fail("取消关联失败");
        }
    }

}
