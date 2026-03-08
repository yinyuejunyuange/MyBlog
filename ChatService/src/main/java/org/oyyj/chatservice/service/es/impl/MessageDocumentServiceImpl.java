package org.oyyj.chatservice.service.es.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.oyyj.chatservice.pojo.ChatMessage;
import org.oyyj.chatservice.pojo.es.MessageDocument;
import org.oyyj.chatservice.repository.MessageDocumentRepository;
import org.oyyj.chatservice.service.es.MessageDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * es实际操作---消息记录
 */
@Service
@EnableAsync
public class MessageDocumentServiceImpl implements MessageDocumentService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    // 索引名称
    private static final String INDEX_NAME = "chat_messages";

    @Async
    @Override
    public void save(MessageDocument messageDocument) {
        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(String.valueOf(messageDocument.getId()))
                    .document(messageDocument)
            );

            return ;
        } catch (IOException e) {
            throw new RuntimeException("保存消息到 Elasticsearch 失败", e);
        }
    }

    @Override
    public List<MessageDocument> searchByContent(String keyword, int page, int size) {
        try {
            // 构建查询：content 字段匹配关键字（使用 match 查询，会分词）
            Query query = MatchQuery.of(m -> m
                    .field("content")
                    .query(keyword)
            )._toQuery();

            // 执行搜索
            SearchResponse<MessageDocument> response = elasticsearchClient.search(s -> s
                            .index(INDEX_NAME)
                            .query(query)
                            .from(page * size)      // 分页起始
                            .size(size)              // 每页大小
                            .sort(sort -> sort.field(f -> f.field("timestamp").order(SortOrder.Desc))), // 按时间倒序
                    MessageDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("搜索消息失败", e);
        }
    }

    @Override
    public List<MessageDocument> findByUserId(String userId) {
        try {
            // 构建复杂查询：查询 fromUserId = userId 或 toUserId = userId
            Query query = BoolQuery.of(b -> b
                    .should(s -> s.term(t -> t.field("fromUserId").value(userId)))
                    .should(s -> s.term(t -> t.field("toUserId").value(userId)))
            )._toQuery();

            SearchResponse<MessageDocument> response = elasticsearchClient.search(s -> s
                            .index(INDEX_NAME)
                            .query(query)
                            .sort(sort -> sort.field(f -> f.field("timestamp").order(SortOrder.Desc)))
                            .size(100), // 可调整
                    MessageDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("查询用户消息失败", e);
        }
    }

    /**
     * 获取消息记录
     * @param userId
     * @param chatUserId
     * @param keyword
     * @param page
     * @param size
     * @return
     */
    public List<MessageDocument> complexSearch(String userId, String chatUserId, String keyword,  int page, int size) {
        try {
            // 使用 BoolQuery 组合多个条件
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

            // 必须满足：用户是发送者或接收者
            boolBuilder.must(m -> m.bool(b -> b
                    // 条件1: chatUserId -> userId
                    .should(s -> s.bool(sb -> sb
                            .must(m1 -> m1.term(t -> t.field("toUserId").value(userId)))
                            .must(m2 -> m2.term(t -> t.field("fromUserId").value(chatUserId)))
                            .must(m3 -> m3.terms(t -> t.field("status")
                                    .terms(v -> v.value(
                                            List.of(
                                                    FieldValue.of(ChatMessage.ChatMessageStatus.SEND.getCode()),
                                                    FieldValue.of(ChatMessage.ChatMessageStatus.READ.getCode())
                                            ))
                                    )
                            ))
                    ))

                    // 条件2: userId -> chatUserId
                    .should(s -> s.bool(sb -> sb
                            .must(m1 -> m1.term(t -> t.field("toUserId").value(chatUserId)))
                            .must(m2 -> m2.term(t -> t.field("fromUserId").value(userId)))
                            .must(m3 -> m3.terms(t -> t.field("status")
                                    .terms(v -> v.value(
                                            List.of(
                                                    FieldValue.of(ChatMessage.ChatMessageStatus.SEND.getCode()),
                                                    FieldValue.of(ChatMessage.ChatMessageStatus.READ.getCode()))
                                            ))
                                    )
                            ))
                    ))
            );


            // 如果有关键词，添加 content 匹配
            if (keyword != null && !keyword.isEmpty()) {
                boolBuilder.must(m -> m.match(t -> t.field("content").query(keyword)));
            }

            Query query = boolBuilder.build()._toQuery();

            SearchResponse<MessageDocument> response = elasticsearchClient.search(s -> s
                            .index(INDEX_NAME)
                            .query(query)
                            .from(page * size)
                            .size(size)
                            .sort(sort -> sort.field(f -> f.field("timestamp").order(SortOrder.Desc))),
                    MessageDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("复杂查询失败", e);
        }
    }
}
