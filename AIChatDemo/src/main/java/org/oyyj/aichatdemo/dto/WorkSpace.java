package org.oyyj.aichatdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkSpace {
    private Long id;
    private String name;
    private String slug;
    private Object vectorTag;
    private Date createdAt;
    private Double openAiTemp;
    private Integer openAiHistory;
    private Date lastUpdatedAt;
    private String openAiPrompt;
    private Double similarityThreshold;
    private String chatProvider;
    private String chatModel;
    private Integer topN;
    private String chatMode;
    private Object pfpFilename;
    private Object agentProvider;
    private Object agentModel;
    private String queryRefusalResponse;
    private String vectorSearchMode;
    private List<DocumentDTO> documents;


}
