package org.oyyj.blogservice.service.es;

import org.oyyj.blogservice.pojo.es.EsSearch;
import org.oyyj.blogservice.pojo.es.HighLightBlog;
import org.oyyj.blogservice.vo.blogs.BlogSearchHighLightVO;

import java.util.List;

public interface EsSearchService {
    void init();// bean创建需要调用

    void bulkSave(List<EsSearch> list);

    void save(EsSearch esSearch);

    List<BlogSearchHighLightVO> highlightSearch(String keyword, int page, int size);

    List<EsSearch> search(String keyword, int page, int size);
}
