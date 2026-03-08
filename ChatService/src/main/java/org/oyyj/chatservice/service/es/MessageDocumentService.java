package org.oyyj.chatservice.service.es;

import org.oyyj.chatservice.pojo.es.MessageDocument;

import java.util.List;

public interface MessageDocumentService {

    /**
     * 保存消息文档到 Elasticsearch
     * @param messageDocument 消息文档
     * @return 保存后的文档
     */
    void save(MessageDocument messageDocument);

    /**
     * 根据关键字搜索消息内容
     * @param keyword 搜索关键字
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页搜索结果列表
     */
    List<MessageDocument> searchByContent(String keyword, int page, int size);

    /**
     * 根据用户ID查询聊天记录（可选，作为补充）
     */
    List<MessageDocument> findByUserId(String userId);
}
