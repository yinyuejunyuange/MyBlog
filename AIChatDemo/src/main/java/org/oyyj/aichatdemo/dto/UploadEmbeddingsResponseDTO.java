package org.oyyj.aichatdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadEmbeddingsResponseDTO {
    private WorkSpace workspace;
}
