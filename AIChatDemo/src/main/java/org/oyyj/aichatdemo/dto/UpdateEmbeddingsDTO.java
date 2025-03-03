package org.oyyj.aichatdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 新增/删除 工作区的json文档
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateEmbeddingsDTO {
    private List<String> adds;
    private List<String> deletes;
}
