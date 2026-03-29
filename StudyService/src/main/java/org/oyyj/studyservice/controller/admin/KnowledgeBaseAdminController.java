package org.oyyj.studyservice.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.annotation.RequestRole;
import org.oyyj.mycommonbase.common.RoleEnum;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgeBase.KnowledgeBaseDTO;
import org.oyyj.studyservice.pojo.KnowledgeBase;
import org.oyyj.studyservice.service.KnowledgeBaseService;
import org.oyyj.studyservice.vo.knowledgeBase.BaseDashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.util.List;

@RestController
@RequestMapping("/myBlog/knowledgeBase/admin")
@Slf4j
public class KnowledgeBaseAdminController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 分页查看题库
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/list")
    public ResultUtil<IPage<KnowledgeBaseDTO>> listKnowledgeBases(
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "name", required = false) String name) throws AuthenticationException {
        IPage<KnowledgeBase> result = knowledgeBaseService.pageQuery(currentPage, pageSize, name).getData();
        IPage<KnowledgeBaseDTO> dtoPage = new Page<>(
                result.getCurrent(), result.getSize(), result.getTotal());
        List<KnowledgeBaseDTO> dtoList = result.getRecords().stream()
                .map(item -> {
                    return new KnowledgeBaseDTO().entityToDTO( item);
                })
                .toList();
        dtoPage.setRecords(dtoList);
        return ResultUtil.success(dtoPage);
    }

    /**
     * 查看题库详情
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/detail")
    public ResultUtil<KnowledgeBaseDTO> getKnowledgeBaseDetail(
            @RequestParam("id") Long id) throws AuthenticationException {
        

        return knowledgeBaseService.getKnowledgeBaseDetail(id);
    }

    /**
     * 新增题库
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping("/add")
    public ResultUtil<String> addKnowledgeBase(@RequestBody KnowledgeBaseDTO dto) throws AuthenticationException {
        

        if (dto.getName() == null || dto.getName().isEmpty()) {
            return ResultUtil.fail("题库名称不能为空");
        }

        return knowledgeBaseService.add(dto);
    }

    /**
     * 修改题库
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/update")
    public ResultUtil<String> updateKnowledgeBase(@RequestBody KnowledgeBaseDTO dto) throws AuthenticationException {
        

        if (dto.getId() == null || dto.getId().isEmpty()) {
            return ResultUtil.fail("题库ID不能为空");
        }

        return knowledgeBaseService.update(dto);
    }

    /**
     * 删除题库
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @DeleteMapping("/delete")
    public ResultUtil<String> deleteKnowledgeBase(@RequestParam("id") Long id) throws AuthenticationException {
        

        return knowledgeBaseService.delete(id);
    }

    /**
     * 关联知识点
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping("/addKnowledgePoints")
    public ResultUtil<String> addKnowledgePoints(
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestBody List<String> knowledgeIds) throws AuthenticationException {
        

        return knowledgeBaseService.addKnowledgePoints(knowledgeBaseId, knowledgeIds);
    }

    /**
     * 取消关联知识点
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping("/removeKnowledgePoints")
    public ResultUtil<String> removeKnowledgePoints(
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestBody List<String> knowledgeIds) throws AuthenticationException {
        

        return knowledgeBaseService.removeKnowledgePoints(knowledgeBaseId, knowledgeIds);
    }

    /**
     * 获取题库关联的知识点列表
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/knowledgePoints")
    public ResultUtil<IPage<KnowledgeBaseDTO>> getKnowledgePoints(
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws AuthenticationException {
        

        return knowledgeBaseService.getKnowledgePointsByBaseId(knowledgeBaseId, page, pageSize);
    }

    /**
     * 获取首页知识库信息
     * 知识库名称 知识点数量 关联题目数量 用户模拟面试次数
     */
    @RequestRole(role = {RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/knowledgeDashboard")
    public ResultUtil<List<BaseDashboardVO>> getDashboard(){
        return knowledgeBaseService.getBaseDashboard();
    }

}
