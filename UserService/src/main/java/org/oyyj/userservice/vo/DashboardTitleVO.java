package org.oyyj.userservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardTitleVO {

    private Integer blogs;

    private Integer users;

    private Integer comments;

    private Integer knowledgePoints;

}
