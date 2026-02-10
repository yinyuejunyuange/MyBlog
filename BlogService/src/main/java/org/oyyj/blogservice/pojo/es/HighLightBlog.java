package org.oyyj.blogservice.pojo.es;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 高亮结构
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HighLightBlog {
    // 文字片段
    private String text;
    // 是否高亮
    private Boolean isHighlight;
}
