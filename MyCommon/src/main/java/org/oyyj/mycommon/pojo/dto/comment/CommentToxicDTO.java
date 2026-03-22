package org.oyyj.mycommon.pojo.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评论攻击行判断
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentToxicDTO {

    private Long id;

    private Integer type;

    private String comment;

    private Integer isToxic;

    /**
     * 攻击种类 ：人身攻击、地域歧视、种族歧视、其他
     */
    private String topicList;

}
