package org.oyyj.blogservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.util.ResultUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/myBlog/blogFeign")
public class BlogFeignController {

    private final IBlogService iBlogService;

    public BlogFeignController(IBlogService iBlogService) {
        this.iBlogService = iBlogService;
    }

    /**
     * 获取用户的所有收藏
     * @param ids
     * @return
     */
    @PostMapping("/titlesByStrIds")
    public Map<Long,String> blogTitleByStrIds(@RequestBody List<String> ids){
        List<Long> list = ids.stream().map(Long::parseLong).toList();
        if(list.isEmpty()){
            return Map.of();
        }

        return iBlogService.list(Wrappers.<Blog>lambdaQuery()
                .in(Blog::getId, list)
        ).stream().collect(Collectors.toMap(Blog::getId, Blog::getTitle));
    }

}
