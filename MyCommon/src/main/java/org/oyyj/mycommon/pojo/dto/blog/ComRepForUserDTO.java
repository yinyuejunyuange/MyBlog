package org.oyyj.mycommon.pojo.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComRepForUserDTO {

    private Long userId;
    private Integer commentCount;
    private Integer replyCount;
    private Integer toxicCount;
    private BigDecimal toxicRate;

}
