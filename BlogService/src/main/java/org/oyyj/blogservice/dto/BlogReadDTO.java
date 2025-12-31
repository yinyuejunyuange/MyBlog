package org.oyyj.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 博客阅读量处理
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogReadDTO {
    private Long blogId;

    private Integer readCount;
}
