package org.oyyj.blogservice.dto;

import lombok.Data;

/**
 * 生成摘要参数接口
 */
@Data
public class BlogSummaryRequestDTO {
    /**
     * Markdown 原文
     */
    private String md_content;

    /**
     * 生成摘要的句子数
     */
    private Integer sentence_count = 3;
}
