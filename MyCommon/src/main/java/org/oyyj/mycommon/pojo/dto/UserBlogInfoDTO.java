package org.oyyj.mycommon.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *与用户有关的博客信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBlogInfoDTO {

    private Integer blogs;

    private Integer likes;

    private Integer star;

    private Integer view;

}
