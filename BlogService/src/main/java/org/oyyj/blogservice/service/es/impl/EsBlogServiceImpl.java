package org.oyyj.blogservice.service.es.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.core.TimeValue;
import org.oyyj.blogservice.common.EsBlogFields;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.es.EsBlog;
import org.oyyj.blogservice.pojo.es.HighLightBlog;
import org.oyyj.blogservice.repository.EsBlogRepository;
import org.oyyj.blogservice.service.es.EsBlogService;
import org.oyyj.blogservice.util.MDUtil;
import org.oyyj.blogservice.vo.blogs.BlogSearchVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class EsBlogServiceImpl implements EsBlogService {

    @Autowired
    private EsBlogRepository esBlogRepository;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private ElasticsearchClient esClient;

    private static final String START = "<em>";
    private static final String END = "</em>";

    // 初始化 检查es并将所有博客信息存储到Es中
    @PostConstruct
    public void init(){
        List<Long> dbBlogIds = blogMapper.selectList(Wrappers.<Blog>lambdaQuery()
                .select(Blog::getId)
        ).parallelStream().map(Blog::getId).toList();

        List<Long> esBlogIds = getEsBlogIds();
        Set<Long> esIdSet = new HashSet<>(esBlogIds);
        List<Long> ids = dbBlogIds.stream()
                .filter(id -> !esIdSet.contains(id))
                .toList();
        // 批量并发执行
        int startIndex = 0;
        int batchSize = 200;
        while (startIndex < ids.size()) {
            int endIndex = startIndex + batchSize;
            List<Long> batchIds = ids.subList(startIndex,  Math.min(startIndex + batchSize, ids.size()) );
            List<EsBlog> esBlogs;
            if(!batchIds.isEmpty()){
                esBlogs = blogMapper.selectList(Wrappers.<Blog>lambdaQuery()
                        .in(Blog::getId, batchIds)
                        .select(Blog::getId, Blog::getContext, Blog::getIntroduce ,Blog::getTitle)
                ).parallelStream().map(item -> {
                    EsBlog esBlog = new EsBlog();
                    esBlog.setId(item.getId());
                    esBlog.setIntroduce(item.getIntroduce());
                    esBlog.setContent(item.getContext());
                    esBlog.setTitle(item.getTitle());
                    return esBlog;
                }).toList();

                esBlogRepository.saveAll(esBlogs);
            }
            startIndex = endIndex;
        }
    }

    /**
     * 查询ES中的博客ID信息
     * es默认只会查询 10条记录  并且有设置上限
     * 要查询全部数据 需要使用游标查询
     * @return
     */
    private List<Long> getEsBlogIds(){
        SearchRequest searchRequest = SearchRequest.of(r -> r
                .source(
                        s -> s.filter(f -> f.includes(EsBlogFields.ID))
                )
                .scroll(s -> s.time("2m")) // 设置游标存活时间
                .size(1000) // 每次滚动返回的数量
        );
        SearchResponse<EsBlog> search;
        List<Long> resultList = new ArrayList<>();
        try {
            search = esClient.search(searchRequest, EsBlog.class);
            List<Hit<EsBlog>> hits = search.hits().hits();
            String scrollId  = search.scrollId();
            while(hits!=null && !hits.isEmpty()){
                List<Long> list = hits.stream()
                        .map(Hit::source)
                        .filter(Objects::nonNull)
                        .map(EsBlog::getId)
                        .toList();

                resultList.addAll(list);

                //  scroll 下一页
                String finalScrollId = scrollId;
                ScrollResponse<EsBlog> scrollResponse = esClient.scroll(
                        sc -> sc.scrollId(finalScrollId)
                                .scroll(t -> t.time("2m")),
                        EsBlog.class
                );

                scrollId = scrollResponse.scrollId();
                hits = scrollResponse.hits().hits();
            }
        } catch (IOException e) {
            log.error("es数据查询失败");
            throw new RuntimeException(e);
        }
        return  resultList;
    }

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
                .sort(s -> s.score(sc -> sc.order(SortOrder.Desc)))  // 按照匹配的强弱排序
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
            BlogSearchVO convert = convert(hit);
            blist.add(convert);
        }
        return blist;
    }

    public BlogSearchVO convert(Hit<EsBlog> hit) {
        EsBlog source = hit.source();

        Map<String, List<String>> highlight = hit.highlight();

        BlogSearchVO vo = new BlogSearchVO();
        String title = Optional.ofNullable(highlight.get("title"))
                .map(l -> l.get(0))
                .orElse(source.getTitle());
        List<HighLightBlog> titleList = parse(title);
        // 标题
        vo.setTitle(titleList);

        // 内容摘要（高亮 + 去 MD）
        String contentHighlight = Optional.ofNullable(highlight.get("content"))
                .map(l -> l.get(0))
                .orElse(source.getContent());

        vo.setContent(
                parse(MDUtil.mdToTextKeepHighlight(contentHighlight))
        );
        vo.setId(source.getId());
        return vo;
    }

    /**
     * 处理高亮数据
     * @param text
     * @return
     */
    private static List<HighLightBlog> parse(String text) {
        List<HighLightBlog> list = new ArrayList<>();
        if (text == null || text.isEmpty()) return list;

        int index = 0;
        while (index < text.length()) {
            int start = text.indexOf(START, index);
            if (start == -1) {
                list.add(new HighLightBlog(text.substring(index), false));
                break;
            }

            if (start > index) {
                list.add(new HighLightBlog(text.substring(index, start), false));
            }

            int end = text.indexOf(END, start);
            if (end == -1) break;

            String highlightText = text.substring(start + START.length(), end);
            list.add(new HighLightBlog(highlightText, true));

            index = end + END.length();
        }
        return list;
    }
}
