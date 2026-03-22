package org.oyyj.mycommon.pojo.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询 近12月用户的博客发表记录
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Blog12MonthDTO {

    private List<String> monthList;

    private List<Integer> blogCountList;
}
