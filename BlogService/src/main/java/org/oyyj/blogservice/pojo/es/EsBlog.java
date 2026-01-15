package org.oyyj.blogservice.pojo.es;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "blogs")
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class EsBlog {

    @Id
    private Long id;

    @Field(type= FieldType.Text, analyzer = "ik_max_word",searchAnalyzer = "ik_smart")
    private String title; // 作者

    @Field(type= FieldType.Text, analyzer = "ik_max_word",searchAnalyzer = "ik_smart")
    private String content; // 内容

    @Field(type = FieldType.Text,analyzer = "ik_max_word",searchAnalyzer = "ik_smart")
    private String introduce;
}
