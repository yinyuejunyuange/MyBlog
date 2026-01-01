package org.oyyj.mycommon.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class BlogCommon {
    /**
     * 按照热度筛选
     */
    public static final String BLOG_HOT="blog_hot";
    /**
     * 按照时间筛选
     */
    public static final String BLOG_TIME="blog_time";

}
