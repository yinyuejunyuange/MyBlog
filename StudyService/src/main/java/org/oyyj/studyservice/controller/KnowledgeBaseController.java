package org.oyyj.studyservice.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgeBase.KnowledgeBaseDTO;
import org.oyyj.studyservice.pojo.KnowledgeBase;
import org.oyyj.studyservice.service.KnowledgeBaseService;
import org.oyyj.studyservice.utils.ParamTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/myBlog/knowledgeBase")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/add")
    public ResultUtil<String> add(@RequestBody KnowledgeBaseDTO dto) {
        return knowledgeBaseService.add(dto);
    }

    @PutMapping("/update")
    public ResultUtil<String> update(@RequestBody KnowledgeBaseDTO dto) {
        return knowledgeBaseService.update(dto);
    }

    @DeleteMapping("/delete/{id}")
    public ResultUtil<String> delete(@PathVariable Long id) {
        return knowledgeBaseService.delete(id);
    }

    @GetMapping("/page")
    public ResultUtil<IPage<KnowledgeBase>> pageQuery(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name
    ) {
        return knowledgeBaseService.pageQuery(pageNum, pageSize, name);
    }

    /**
     * 获取某类别的所有知识库
     * @param type
     * @return
     */
    @GetMapping()
    public ResultUtil<List<KnowledgeBaseDTO>> pageQuery(@RequestParam(value = "type",required = false) String type){
        return knowledgeBaseService.listKnowledgeBase(type);
    }

    /**
     * 依据某个获取详情
     * @param id
     * @return
     */
    @GetMapping("/byId")
    public ResultUtil<KnowledgeBaseDTO> getById(@RequestParam(value = "id") String id) {

        KnowledgeBase byId = knowledgeBaseService.getById(ParamTypeUtil.toLong(id));
        return ResultUtil.success(new KnowledgeBaseDTO().entityToDTO(byId));

    }

    /**
     * 获取所有类别的信息
     * @return
     */
    @GetMapping("/type")
    public ResultUtil<List<String>> listType(){
        return knowledgeBaseService.listAllType();
    }



}
