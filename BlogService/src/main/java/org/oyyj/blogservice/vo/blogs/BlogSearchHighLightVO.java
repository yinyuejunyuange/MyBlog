package org.oyyj.blogservice.vo.blogs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.blogservice.pojo.es.HighLightBlog;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogSearchHighLightVO {

    private List<HighLightBlog>  searchList;
    private String fullText;

}
