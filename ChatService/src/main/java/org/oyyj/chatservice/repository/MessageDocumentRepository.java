package org.oyyj.chatservice.repository;


import org.oyyj.chatservice.pojo.es.MessageDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MessageDocumentRepository extends ElasticsearchRepository<MessageDocument,String> {
}
