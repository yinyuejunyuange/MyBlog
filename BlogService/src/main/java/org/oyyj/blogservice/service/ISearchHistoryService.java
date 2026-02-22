package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.pojo.SearchHistory;
import org.oyyj.blogservice.pojo.es.HighLightBlog;
import org.oyyj.blogservice.vo.blogs.BlogSearchHighLightVO;

import java.util.List;

public interface ISearchHistoryService extends IService<SearchHistory> {

    List<String> recommendForUser(Long userId);

    void recordSearch(Long userId, String rawQuery);

    List<String> userHistorySearch(Long userId);

    List<BlogSearchHighLightVO> selectRelate(String keywords);
}
