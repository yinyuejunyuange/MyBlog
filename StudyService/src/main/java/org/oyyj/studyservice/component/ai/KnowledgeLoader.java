package org.oyyj.studyservice.component.ai;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.studyservice.mapper.KnowledgePointMapper;
import org.oyyj.studyservice.pojo.KnowledgePoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class KnowledgeLoader {


    @Autowired
    private KnowledgePointMapper mapper;
    @Autowired
    private EmbeddingStore<TextSegment> store;
    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ElasticsearchClient esClient; // 直接注入，用于检查索引

    @Value("${elasticsearch.index-name:knowledge_base}")
    private String indexName;

    @PostConstruct
    public void load() {
        // 检查索引中是否已有数据（可选，避免重复加载）
        if (isIndexPopulated()) {
            log.info("Elasticsearch 索引 {} 已存在数据，跳过加载", indexName);
            return;
        }

        List<KnowledgePoint> list = mapper.selectList(Wrappers.emptyWrapper());
        log.info("开始加载 {} 个知识点到 Elasticsearch", list.size());

        for (KnowledgePoint kp : list) {
            TextSegment segment = TextSegment.from(
                    kp.getTitle() + "\n" + kp.getRecommendedAnswer()
            );
            store.add(embeddingModel.embed(segment).content(), segment);
        }

        log.info("知识点加载完成");
    }

    /**
     * 检查索引是否已有文档
     */
    private boolean isIndexPopulated() {
        try {
            SearchResponse<Void> response = esClient.search(SearchRequest.of(s -> s
                            .index(indexName)
                            .size(0)  // 不返回实际文档，只统计总数
                    ),
                    Void.class
            );
            long count = response.hits().total().value();
            return count > 0;
        } catch (IOException e) {
            log.warn("检查索引失败，将尝试加载数据", e);
            return false;
        }
    }

}
