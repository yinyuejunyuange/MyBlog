package org.oyyj.blogservice.vo.blogs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogSearchVO {

    private Long id;

    private String title;

    private String content;



}
