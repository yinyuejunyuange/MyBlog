package org.oyyj.blogservice.vo.blogs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.blogservice.pojo.es.HighLightBlog;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlogSearchVO {

    private Long id;

    private List<HighLightBlog> title;

    private List<HighLightBlog> content;

    private String userId;
    private String userName;
    private List<HighLightBlog>  introduce;
    private Date createTime;
    private Date updateTime;
    private Integer status;
    private List<String> typeList;
    private String like;
    private String star;
    private String view;
    private String commentNum;
    private Double score; // 评分
    private String userHead;// 作者头像



}
