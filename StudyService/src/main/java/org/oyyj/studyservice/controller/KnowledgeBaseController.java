package org.oyyj.studyservice.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgeBase.KnowledgeBaseDTO;
import org.oyyj.studyservice.pojo.KnowledgeBase;
import org.oyyj.studyservice.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

}
