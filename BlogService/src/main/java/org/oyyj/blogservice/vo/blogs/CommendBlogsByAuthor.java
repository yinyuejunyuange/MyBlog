package org.oyyj.blogservice.vo.blogs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.blogservice.dto.BlogDTO;

import java.util.List;

/**
 * 依据作者信息获取其最近创作 以及热门博客
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommendBlogsByAuthor {
    /**
     * 依据发布时间的最近创作
     */
    private List<BlogDTO>  recentBlogs;
    /**
     * 依据阅读量的热门博客
     */
    private List<BlogDTO>  hotBlogs;
}
