package org.oyyj.blogservice.pojo.es;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighLightBlog that = (HighLightBlog) o;
        return isHighlight == that.isHighlight && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, isHighlight);
    }
}
