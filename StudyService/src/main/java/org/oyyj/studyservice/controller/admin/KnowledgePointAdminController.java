package org.oyyj.studyservice.controller.admin;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.annotation.RequestRole;
import org.oyyj.mycommonbase.common.RoleEnum;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgePointDTO;
import org.oyyj.studyservice.dto.question.QuestionDTO;
import org.oyyj.studyservice.pojo.KnowledgePoint;
import org.oyyj.studyservice.pojo.Question;
import org.oyyj.studyservice.service.KnowledgePointCommentService;
import org.oyyj.studyservice.service.KnowledgePointService;
import org.oyyj.studyservice.service.QuestionService;
import org.oyyj.studyservice.vo.knowledgeComment.KnowledgeCommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.util.List;

@RestController
@RequestMapping("/myBlog/knowledgePoint/admin")
@Slf4j
public class KnowledgePointAdminController {

    // todo

    // 创建知识点

    // 修改知识点

    // 查看知识点详情

    // 分页查看知识点的评论信息

    // 手动关联试题
    @Autowired
    private KnowledgePointService knowledgePointService;

    @Autowired
    private KnowledgePointCommentService knowledgePointCommentService;

    @Autowired
    private QuestionService questionService;

    /**
     * 创建知识点
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping("/add")
    public ResultUtil<String> addKnowledgePoint(@RequestBody KnowledgePointDTO knowledgePointDTO) throws AuthenticationException {
       
        if (knowledgePointDTO.getTitle() == null || knowledgePointDTO.getTitle().isEmpty()) {
            return ResultUtil.fail("知识点标题不能为空");
        }

        return knowledgePointService.add(knowledgePointDTO);
    }

    /**
     * 修改知识点
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/update")
    public ResultUtil<String> updateKnowledgePoint(@RequestBody KnowledgePointDTO knowledgePointDTO) throws AuthenticationException {
        

        if (knowledgePointDTO.getId() == null || knowledgePointDTO.getId().isEmpty()) {
            return ResultUtil.fail("知识点ID不能为空");
        }

        return knowledgePointService.update(knowledgePointDTO);
    }

    /**
     * 删除知识点
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @DeleteMapping("/delete")
    public ResultUtil<String> deleteKnowledgePoint(@RequestBody List<Long> ids) throws AuthenticationException {
        

        if (ids == null || ids.isEmpty()) {
            return ResultUtil.fail("知识点ID列表不能为空");
        }

        return knowledgePointService.deleteByIds(ids);
    }

    /**
     * 查看知识点详情
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/detail")
    public ResultUtil<KnowledgePointDTO> getKnowledgePointDetail(@RequestParam("id") Long id) throws AuthenticationException {
        

        KnowledgePoint knowledgePoint = knowledgePointService.getById(id);
        if (knowledgePoint == null) {
            return ResultUtil.fail("知识点不存在");
        }

        KnowledgePoint.LevelEnum levelEnum = KnowledgePoint.LevelEnum.getByValue(knowledgePoint.getLevel());
        KnowledgePointDTO dto = new KnowledgePointDTO();
        dto.setId(String.valueOf(knowledgePoint.getId()));
        dto.setCreateBy(knowledgePoint.getCreateBy());
        dto.setCreateTime(knowledgePoint.getCreateTime());
        dto.setUpdateBy(knowledgePoint.getUpdateBy());
        dto.setUpdateTime(knowledgePoint.getUpdateTime());
        dto.setTitle(knowledgePoint.getTitle());
        dto.setRecommendedAnswer(knowledgePoint.getRecommendedAnswer());
        dto.setLevel(levelEnum != null ? levelEnum.getDesc() : null);
        dto.setIsDelete(knowledgePoint.getIsDelete());

        return ResultUtil.success(dto);
    }

    /**
     * 分页查看知识点列表
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/list")
    public ResultUtil<Page<KnowledgePointDTO>> listKnowledgePoints(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "baseId", required = false) Long baseId,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "search", required = false) String search) throws AuthenticationException {


        return knowledgePointService.listAllKnowledgePoint(page, pageSize, baseId, level, tags, search);
    }

    /**
     * 分页查看知识点的评论信息
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/comments")
    public ResultUtil<KnowledgeCommentVO> getKnowledgePointComments(
            @RequestParam("knowledgePointId") Long knowledgePointId,
            @RequestParam(value = "lastCommentId", required = false) Long lastCommentId) throws AuthenticationException {
        

        KnowledgePoint knowledgePoint = knowledgePointService.getById(knowledgePointId);
        if (knowledgePoint == null) {
            return ResultUtil.fail("知识点不存在");
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(-1L);
        loginUser.setIsUserLogin(0);

        return knowledgePointCommentService.getComment(knowledgePointId, lastCommentId, loginUser);
    }

    /**
     * 手动关联试题到知识点
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping("/relateQuestion")
    public ResultUtil<String> relateQuestionToKnowledgePoint(
            @RequestParam("knowledgePointId") Long knowledgePointId,
            @RequestParam("questionId") Long questionId) throws AuthenticationException {
        

        KnowledgePoint knowledgePoint = knowledgePointService.getById(knowledgePointId);
        if (knowledgePoint == null) {
            return ResultUtil.fail("知识点不存在");
        }

        Question question = questionService.getById(questionId);
        if (question == null) {
            return ResultUtil.fail("试题不存在");
        }

        question.setKnowledgePointId(knowledgePointId);
        boolean update = questionService.updateById(question);

        if (update) {
            return ResultUtil.success("试题关联成功");
        } else {
            return ResultUtil.fail("试题关联失败");
        }
    }

    /**
     * 批量关联试题到知识点
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping("/relateQuestions")
    public ResultUtil<String> relateQuestionsToKnowledgePoint(
            @RequestParam("knowledgePointId") Long knowledgePointId,
            @RequestBody List<Long> questionIds) throws AuthenticationException {
        

        KnowledgePoint knowledgePoint = knowledgePointService.getById(knowledgePointId);
        if (knowledgePoint == null) {
            return ResultUtil.fail("知识点不存在");
        }

        if (questionIds == null || questionIds.isEmpty()) {
            return ResultUtil.fail("试题ID列表不能为空");
        }

        int successCount = 0;
        for (Long questionId : questionIds) {
            Question question = questionService.getById(questionId);
            if (question != null) {
                question.setKnowledgePointId(knowledgePointId);
                if (questionService.updateById(question)) {
                    successCount++;
                }
            }
        }

        return ResultUtil.success("成功关联" + successCount + "道试题");
    }

    /**
     * 取消试题与知识点的关联
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @DeleteMapping("/unrelateQuestion")
    public ResultUtil<String> unrelateQuestionFromKnowledgePoint(
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

    /**
     * 查看知识点关联的试题列表
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/questions")
    public ResultUtil<Page<QuestionDTO>> getKnowledgePointQuestions(
            @RequestParam("knowledgePointId") Long knowledgePointId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws AuthenticationException {
        

        KnowledgePoint knowledgePoint = knowledgePointService.getById(knowledgePointId);
        if (knowledgePoint == null) {
            return ResultUtil.fail("知识点不存在");
        }

        Page<QuestionDTO> questionPage = questionService.getQuestionPage(page, pageSize, null, null, knowledgePointId);
        return ResultUtil.success(questionPage);
    }

}
