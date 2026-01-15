package org.oyyj.blogservice.repository;

import org.oyyj.blogservice.pojo.es.EsBlog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.CrudRepository;

public interface EsBlogRepository extends ElasticsearchRepository<EsBlog,String> {
}
