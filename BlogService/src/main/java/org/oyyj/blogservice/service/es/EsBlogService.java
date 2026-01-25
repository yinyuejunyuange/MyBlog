package org.oyyj.blogservice.service.es;

import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.es.EsBlog;
import org.oyyj.blogservice.vo.blogs.BlogSearchVO;
import org.springframework.stereotype.Service;

import java.util.List;

public interface EsBlogService {

    EsBlog saveBlog(EsBlog blog);

    List<BlogSearchVO>  highlightSearch(String keyword , Integer pageNum, Integer pageSize);

}
