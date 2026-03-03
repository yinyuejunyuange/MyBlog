package org.oyyj.studyservice.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgePointDTO;
import org.oyyj.studyservice.pojo.KnowledgePoint;
import org.oyyj.studyservice.service.KnowledgePointService;
import org.oyyj.studyservice.utils.ParamTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/myBlog/point")
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

    /**
     * 获取分页知识点
     * @param page
     * @param pageSize
     * @param baseId
     * @param level
     * @param tags
     * @param search
     * @return
     */
    @GetMapping()
    public ResultUtil<Page<KnowledgePointDTO>> list(@RequestParam("page") Integer page,
                                                    @RequestParam("pageSize") Integer pageSize,
                                                    @RequestParam(value = "baseId",required = false)String baseId,
                                                    @RequestParam(value = "level",required = false) String level,
                                                    @RequestParam(value = "tags", required = false) String tags,
                                                    @RequestParam(value = "search",required = false) String search){
        return knowledgePointService.listAllKnowledgePoint(page,pageSize, ParamTypeUtil.toLong(baseId),level,tags,search);
    }


    /**
     * 获取某个知识库下面所有知识点(仅有 id 和 title)
     * @param id
     * @return
     */
    @GetMapping("/getContent")
    public ResultUtil<List<KnowledgePointDTO>> listAll(@RequestParam("id") String id){
        return knowledgePointService.listAllKnowledgePoint(ParamTypeUtil.toLong(id));
    }

    /**
     * 获取详情信息
     *
     * @param id
     * @return
     */
    @GetMapping("/detail")
    public ResultUtil<KnowledgePointDTO> detail(@RequestParam("id") String id){
        KnowledgePoint byId = knowledgePointService.getById(ParamTypeUtil.toLong(id));
        return ResultUtil.success(byId.entityToDTO(byId));
    }



}
