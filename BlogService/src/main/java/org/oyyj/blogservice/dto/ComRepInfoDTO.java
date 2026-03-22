package org.oyyj.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论攻击性修改i
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComRepInfoDTO {
    private String id;

    private Integer isToxic;

    /**
     *  2.：可以是以下一个或多个类别（用英文逗号分隔）：
     *                                              - 人身攻击：针对个人的外貌、能力、品德等。
     *                                              - 地域歧视：针对特定地区或籍贯的贬低。
     *                                              - 种族歧视：针对特定种族或民族的偏见。
     *                                              - 其他：上述未涵盖的攻击对象。
     */
    private String mulType;
}
