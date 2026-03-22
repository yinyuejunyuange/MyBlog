package org.oyyj.studyservice.vo.knowledgeBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首页视图展示的知识点数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseDashboardVO {

    private String baseName;

    private Integer pointsNum;

    private Integer questions;

    /**
     * 模拟面试次数
     */
    private Integer interviewNum;

}
