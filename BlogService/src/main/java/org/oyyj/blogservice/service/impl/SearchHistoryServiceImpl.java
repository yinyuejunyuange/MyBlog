package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.oyyj.blogservice.dto.SearchCountDTO;
import org.oyyj.blogservice.mapper.SearchHistoryMapper;
import org.oyyj.blogservice.pojo.SearchHistory;
import org.oyyj.blogservice.pojo.es.EsSearch;
import org.oyyj.blogservice.pojo.es.HighLightBlog;
import org.oyyj.blogservice.service.ISearchHistoryService;
import org.oyyj.blogservice.service.es.EsSearchService;
import org.oyyj.blogservice.vo.blogs.BlogSearchHighLightVO;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchHistoryServiceImpl extends ServiceImpl<SearchHistoryMapper, SearchHistory> implements ISearchHistoryService {
    @Value("${app.normalize.service.url}")
    private String url;

    @Value("${app.hot-hours}")
    private int hotHours;

    @Value("${app.hot-top}")
    private int hotTop;

    @Value("${app.recommend-limit}")
    private int recommendLimit;

    @Autowired
    private EsSearchService esSearchService;

    private final ObjectMapper mapper = new ObjectMapper();

    public String normalize(String query) {
        RestTemplate rest = new RestTemplate();
        try {
            String resp = rest.getForObject(url + "?q={q}", String.class, query);
            JsonNode node = mapper.readTree(resp);
            return node.get("normalized_query").asText();
        } catch (Exception e) {
            log.error("调用分词失败，使用原词", e);
            return query; // 降级
        }
    }

    @Override
    public void recordSearch(Long userId, String rawQuery) {
        String norm = normalize(rawQuery);
        SearchHistory h = new SearchHistory();
        h.setUserId(userId);
        h.setQueryRaw(rawQuery);
        h.setQueryNorm(norm);
        h.setCreatedAt(new Date());
        int insert = baseMapper.insert(h);
        // 存储到es中
        if(insert != 0) {
            EsSearch esSearch = new EsSearch();
            esSearch.setId(h.getId());
            esSearch.setSearch(rawQuery);
            esSearchService.save(esSearch);
        }
    }

    @Override
    public List<String> userHistorySearch(Long userId) {
        return list(Wrappers.<SearchHistory>lambdaQuery().eq(SearchHistory::getUserId, userId)
                .eq(SearchHistory::getIsVisible, YesOrNoEnum.YES.getCode())
                .last("limit " + recommendLimit)
        ).stream().map(SearchHistory::getQueryRaw).toList();
    }

    @Override
    public List<BlogSearchHighLightVO> selectRelate(String keywords) {
        return esSearchService.highlightSearch(keywords,0,20);
    }

    // 热门搜索
    public List<SearchCountDTO> hotSearch() {
        Date since = Date.from(Instant.now().minus(hotHours, ChronoUnit.HOURS));
        return  baseMapper.countByNormSince(since, 0, hotTop);
    }

    // 个性化推荐（实时计算）
    @Override
    public List<String> recommendForUser(Long userId) {
        // 1. 获取该用户最近搜索的5个词（作为种子）
        List<SearchHistory> recent = baseMapper.findTop5ByUserIdOrderByCreatedAtDesc(userId);
        if (recent.isEmpty()) {
            return recommendForHot();
        }
        Set<String> userWords = recent.stream()
                .map(SearchHistory::getQueryNorm)
                .collect(Collectors.toSet());

        // 2. 找出所有搜过这些词的其他用户
        Set<Long> candidateUserIds = new HashSet<>();
        for (String word : userWords) {
            candidateUserIds.addAll(baseMapper.findUserIdsByQueryNorm(word));
        }
        // 排除自己
        candidateUserIds.remove(userId);
        if (candidateUserIds.isEmpty()) {
            return recommendForHot();
        }

        // 3. 在这些用户的历史中，找出他们搜过的其他词（排除用户自己搜过的词），按出现次数排序
        List<SearchHistory> topByUserIdsOrderByCreatedAtDesc = baseMapper.findTopByUserIdsOrderByCreatedAtDesc(new ArrayList<>(candidateUserIds));

        return topByUserIdsOrderByCreatedAtDesc.stream().map(SearchHistory::getQueryRaw).toList();
    }

    public List<String> recommendForHot(){

        List<SearchCountDTO> searchCountDTOS = hotSearch();
        List<String> norms = searchCountDTOS.stream().map(SearchCountDTO::getQueryNorm).toList();
        List<SearchHistory> list = list(Wrappers.<SearchHistory>lambdaQuery()
                .in(!norms.isEmpty(), SearchHistory::getQueryNorm, norms)
                .last("limit " + recommendLimit)
        );
        return  list.stream().map(SearchHistory::getQueryRaw).toList();
    }



}
