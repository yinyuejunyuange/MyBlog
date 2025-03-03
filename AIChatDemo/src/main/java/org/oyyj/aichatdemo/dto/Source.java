package org.oyyj.aichatdemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Source {
    private String id;
    private String url;
    private String title;
    private String docAuthor;
    private String description;
    private String docSource;
    private String chunkSource;
    private String published;
    private Long wordCount;
    @JsonProperty("token_count_estimate")
    private Long tokenCountEstimate;
    private String text;
    @JsonProperty("_distance")
    private Double distance;
    private Double score;
}
