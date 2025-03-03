package org.oyyj.aichatdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentDTO {
    private Integer id;
    private String docId;
    private String filename;
    private String docpath;
    private Integer workspaceId;
    private String metadata;
    private Boolean pinned;
    private Boolean watched;
    private Date createdAt;
    private Date lastUpdatedAt;
}
