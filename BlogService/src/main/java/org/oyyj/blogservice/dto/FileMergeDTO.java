package org.oyyj.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMergeDTO {
    private String fileNo;

    private Long totalFileChunks;

    private String orgFileName;
}
