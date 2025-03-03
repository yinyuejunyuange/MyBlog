package org.oyyj.aichatdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Document {
    private String id;
    private String url;
    private String title;
    private String docAuthor;
    private String description;
    private String docSource;
    private String chunkSource;
    private String published;
    private Integer wordCount;
    private String pageContent;
    private Integer token_count_estimate;
    private String location;
}
