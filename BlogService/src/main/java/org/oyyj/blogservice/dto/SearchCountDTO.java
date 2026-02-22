package org.oyyj.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchCountDTO {
    // 标准化搜索词
    private String queryNorm;
    // 出现次数
    private Integer count;
}
