package org.oyyj.blogservice.service.es.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.common.EsBlogFields;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.es.EsBlog;
import org.oyyj.blogservice.repository.EsBlogRepository;
import org.oyyj.blogservice.service.es.EsBlogService;
import org.oyyj.blogservice.vo.blogs.BlogSearchVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EsBlogServiceImpl implements EsBlogService {

    @Autowired
    private EsBlogRepository esBlogRepository;

    @Autowired
    private ElasticsearchClient esClient;


    // todo 初始化 检查es并将所有博客信息存储到Es中



    @Override
    public EsBlog saveBlog(EsBlog blog) {
        return esBlogRepository.save(blog);
    }

    @Override
    public List<BlogSearchVO> highlightSearch(String keyword , Integer pageNum , Integer pageSize) {

        if(pageNum==null|| pageNum<=0){
            pageNum=1;
        }
        if(pageSize==null||pageSize<=0){
            pageSize=10;
        }
        Integer finalPageNum = pageNum;
        Integer finalPageSize = pageSize;
        SearchRequest searchRequest = SearchRequest.of(r -> r
                .source(
                        s->s.filter(f-> f.includes(EsBlogFields.TITLE)
                                .includes(EsBlogFields.ID)
                        )
                )
                .query(
                        q -> q.multiMatch(
                                m -> m.query(keyword)
                                        .fields(EsBlogFields.TITLE, EsBlogFields.CONTENT)

                        ))
                .highlight(h -> h
                        .fields(EsBlogFields.TITLE, f -> f) // 自动配置默认的高亮
                        .fields(EsBlogFields.CONTENT, f -> f)
                )
                .from((finalPageNum -1) * finalPageSize)
                .size(finalPageSize)
        );
        SearchResponse<EsBlog> search = null;
        try {
            search = esClient.search(searchRequest, EsBlog.class);
        } catch (IOException e) {
            log.error(  e.getMessage(),e);
            throw new RuntimeException(e);
        }
        List<Hit<EsBlog>> hits = search.hits().hits();
        List<BlogSearchVO> blist = new ArrayList<>();
        for (Hit<EsBlog> hit : hits) {
            EsBlog source = hit.source();
            BlogSearchVO blogSearchVO = new BlogSearchVO();
            if(source == null){
                continue;
            }
            blogSearchVO.setId(source.getId());
            Map<String, List<String>> highlight = hit.highlight();
            if(highlight != null){
                if(highlight.containsKey(EsBlogFields.TITLE)){
                    blogSearchVO.setTitle(String.join(",", highlight.get(EsBlogFields.TITLE)));
                }
                if(highlight.containsKey(EsBlogFields.CONTENT)){
                    blogSearchVO.setContent(String.join(",", highlight.get(EsBlogFields.CONTENT)));
                }
            }
            blist.add(blogSearchVO);
        }
        return blist;

    }
}
