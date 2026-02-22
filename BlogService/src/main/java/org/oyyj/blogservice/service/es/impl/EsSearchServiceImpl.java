package org.oyyj.blogservice.service.es.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.mapper.SearchHistoryMapper;
import org.oyyj.blogservice.pojo.SearchHistory;
import org.oyyj.blogservice.pojo.es.EsSearch;
import org.oyyj.blogservice.pojo.es.HighLightBlog;
import org.oyyj.blogservice.service.es.EsSearchService;
import org.oyyj.blogservice.vo.blogs.BlogSearchHighLightVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EsSearchServiceImpl implements EsSearchService {

    @Autowired
    private  ElasticsearchClient esClient; // 你的 ES 客户端 Bean
    @Autowired
    private SearchHistoryMapper searchHistoryMapper;

    private static final String START = "<em>";
    private static final String END = "</em>";
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    /**
     * 初始化：将 MySQL 中的搜索词同步到 ES
     */
    @PostConstruct
    public void init() {
        ensureIndexExists();
        log.info("开始同步 MySQL 搜索历史到 ES...");

        // 1. 从 MySQL 获取所有需要同步的搜索词 ID（假设你有一个搜索历史表）
        // 请根据你的实际表结构调整查询逻辑
        List<Long> dbIds = searchHistoryMapper.selectList(Wrappers.<SearchHistory>lambdaQuery()
                .select(SearchHistory::getId)
        ).stream().map(SearchHistory::getId).toList();

        // 2. 从 ES 中查询已存在的 ID
        List<Long> esIds = getEsSearchIds();

        Set<Long> esIdSet = new HashSet<>(esIds);
        // 3. 需要新增的 ID
        List<Long> idsToAdd = dbIds.stream()
                .filter(id -> !esIdSet.contains(id))
                .collect(Collectors.toList());

        if (idsToAdd.isEmpty()) {
            log.info("没有需要同步的数据");
            return;
        }

        // 4. 批量查询 MySQL 数据并写入 ES
        int batchSize = 200;
        for (int i = 0; i < idsToAdd.size(); i += batchSize) {
            int end = Math.min(i + batchSize, idsToAdd.size());
            List<Long> batchIds = idsToAdd.subList(i, end);

            // 根据 ID 列表查询搜索词详情（假设 searchHistoryMapper 有方法根据 ID 列表查询）
            List<SearchHistory> historyList = searchHistoryMapper.selectBatchIds(batchIds);
            // 转换为 EsSearch 实体
            List<EsSearch> esSearchList = historyList.stream()
                    .map(h -> {
                        EsSearch es = new EsSearch();
                        es.setId(h.getId());
                        // 这里选择使用标准化后的词还是原始词？根据你的需求，假设使用 query_norm
                        es.setSearch(h.getQueryNorm());
                        return es;
                    })
                    .collect(Collectors.toList());

            // 批量写入 ES
            bulkSave(esSearchList);
            log.info("已同步批次 {}-{}", i, end);
        }

        log.info("ES 同步完成，共新增 {} 条记录", idsToAdd.size());
    }

    private void ensureIndexExists() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(EsSearch.class);
        if (!indexOps.exists()) {
            log.info("索引 searchs 不存在，开始创建...");
            indexOps.create();                 // 创建索引
            indexOps.putMapping(indexOps.createMapping()); // 根据 @Field 注解写入映射
            log.info("索引 searchs 创建成功，映射已应用");
        }
    }
    /**
     * 从 ES 中查询所有已存在的 ID（使用 scroll 游标）
     */
    private List<Long> getEsSearchIds() {
        List<Long> ids = new ArrayList<>();
        try {
            // 只获取 id 字段
            SearchRequest searchRequest = SearchRequest.of(r -> r
                    .index("searchs")
                    .source(s -> s.filter(f -> f.includes("id")))
                    .scroll(s -> s.time("2m"))
                    .size(1000)
            );

            SearchResponse<EsSearch> response = esClient.search(searchRequest, EsSearch.class);
            String scrollId = response.scrollId();
            HitsMetadata<EsSearch> hits = response.hits();

            while (hits != null && hits.hits() != null && !hits.hits().isEmpty()) {
                // 收集 ID
                hits.hits().stream()
                        .map(Hit::source)
                        .filter(Objects::nonNull)
                        .map(EsSearch::getId)
                        .forEach(ids::add);

                // 继续滚动
                String finalScrollId = scrollId;
                ScrollResponse<EsSearch> scrollResponse = esClient.scroll(s -> s
                        .scrollId(finalScrollId)
                        .scroll(t -> t.time("2m")), EsSearch.class);
                scrollId = scrollResponse.scrollId();
                hits = scrollResponse.hits();
            }

            // 清除 scroll
            if (scrollId != null) {
                String finalScrollId1 = scrollId;
                esClient.clearScroll(c -> c.scrollId(finalScrollId1));
            }
        } catch (IOException e) {
            log.error("查询 ES ID 失败", e);
            throw new RuntimeException(e);
        }
        return ids;
    }

    /**
     * 批量保存 EsSearch 列表到 ES
     */
    public void bulkSave(List<EsSearch> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (EsSearch es : list) {
                br.operations(op -> op
                        .index(idx -> idx
                                .index("searchs")
                                .id(es.getId().toString())
                                .document(es)
                        )
                );
            }
            BulkResponse response = esClient.bulk(br.build());
            if (response.errors()) {
                log.error("批量保存 ES 失败: {}", response);
                // 可以进一步处理错误
            }
        } catch (IOException e) {
            log.error("批量保存 ES 异常", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 保存单个 EsSearch 到 ES（插入或更新）
     */
    @Async
    public void save(EsSearch esSearch) {
        try {
            IndexResponse response = esClient.index(i -> i
                    .index("searchs")
                    .id(esSearch.getId().toString())
                    .document(esSearch)
            );
            log.debug("保存成功: {}", response.result());
        } catch (IOException e) {
            log.error("保存 ES 失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 高亮查询：根据关键词搜索 search 字段，返回拆分后的高亮片段列表
     * @param keyword 查询关键词
     * @param page 页码（从 0 开始）
     * @param size 每页大小
     * @return 包含多个 HighLightBlog 片段的列表
     */
    public List<BlogSearchHighLightVO> highlightSearch(String keyword, int page, int size) {
        List<BlogSearchHighLightVO> result = new ArrayList<>();
        try {
            // 构建高亮配置
            Highlight highlight = Highlight.of(h -> h
                    .fields("search", HighlightField.of(f -> f
                            .preTags(START)
                            .postTags(END)
                            .requireFieldMatch(false)
                            .numberOfFragments(1)  // 每个文档只返回一个最佳片段
                    ))
            );

            SearchRequest searchRequest = SearchRequest.of(r -> r
                    .index("searchs")
                    .query(q -> q
                            .match(m -> m
                                    .field("search")
                                    .query(keyword)
                            )
                    )
                    .highlight(highlight)
                    .from(page * size)
                    .size(size)
            );

            SearchResponse<EsSearch> response = esClient.search(searchRequest, EsSearch.class);
            List<String> alFullTextList = new ArrayList<>();
            for (Hit<EsSearch> hit : response.hits().hits()) {
                List<HighLightBlog> highLightBlogs = new ArrayList<>();
                Map<String, List<String>> highlightMap = hit.highlight();
                List<String> highlights = highlightMap.getOrDefault("search", Collections.emptyList());

                if (!highlights.isEmpty()) {
                    // 取第一个高亮片段，解析成片段列表并添加到结果
                    highLightBlogs.addAll(parse(highlights.get(0)));
                } else {
                    // 如果没有高亮（理论上不会发生），则返回原始文本作为非高亮片段
                    EsSearch source = hit.source();
                    if (source != null && source.getSearch() != null) {
                        highLightBlogs.addAll(parse(source.getSearch()));
                    }
                }
                String collect = highLightBlogs.stream().map(HighLightBlog::getText).collect(Collectors.joining());
                if(alFullTextList.contains(collect)){
                    continue;
                }
                alFullTextList.add(collect);
                BlogSearchHighLightVO blogSearchHighLightVO = new BlogSearchHighLightVO(highLightBlogs,collect);
                result.add(blogSearchHighLightVO);
            }
        } catch (IOException e) {
            log.error("高亮查询失败", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 解析包含高亮标签的文本，返回片段列表
     * @param text 包含 <em> 标签的文本
     * @return 片段列表
     */
    private List<HighLightBlog> parse(String text) {
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

    /**
     * 简单分页查询（不带高亮）
     */
    public List<EsSearch> search(String keyword, int page, int size) {
        try {
            SearchRequest searchRequest = SearchRequest.of(r -> r
                    .index("searchs")
                    .query(q -> q
                            .match(m -> m
                                    .field("search")
                                    .query(keyword)
                            )
                    )
                    .from(page * size)
                    .size(size)
            );

            SearchResponse<EsSearch> response = esClient.search(searchRequest, EsSearch.class);
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("查询失败", e);
            throw new RuntimeException(e);
        }
    }

}
