package org.oyyj.blogservice.repository;

import org.oyyj.blogservice.pojo.es.EsSearch;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsSearchRepository extends ElasticsearchRepository<EsSearch,String> {
}
