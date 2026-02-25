package org.oyyj.studyservice.controller;

import cn.hutool.core.util.StrUtil;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgePointDTO;
import org.oyyj.studyservice.service.KnowledgePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("myBlog/study")
public class KnowledgePointController {


    @Autowired
    private KnowledgePointService knowledgePointService;

    /**
     * 新增知识点
     */
    @PostMapping
    public ResultUtil<String> add( @RequestBody KnowledgePointDTO knowledgePointDTO) {
        return knowledgePointService.add(knowledgePointDTO);
    }

    /**
     * 修改知识点
     */
    @PutMapping
    public ResultUtil<String> update(@RequestBody KnowledgePointDTO knowledgePointDTO) {
        return knowledgePointService.update(knowledgePointDTO);
    }

    /**
     * 批量删除知识点（通过请求体传递ID列表）
     * 请求示例：DELETE /api/knowledge-point/delete
     * 请求体： [1,2,3]
     */
    @DeleteMapping("/delete")
    public ResultUtil<String> deleteByIds(@RequestBody List<Long> ids) {
        return knowledgePointService.deleteByIds(ids);
    }

    /**
     * 批量删除知识点（通过URL参数，逗号分隔）
     * 请求示例：DELETE /api/knowledge-point/delete?ids=1,2,3
     */
    @DeleteMapping("/deleteByIds")
    public ResultUtil<String> deleteByIdsParam(@RequestParam String ids) {
        if (StrUtil.isBlank(ids)) {
            return ResultUtil.fail("参数ids不能为空");
        }
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        return knowledgePointService.deleteByIds(idList);
    }

}
