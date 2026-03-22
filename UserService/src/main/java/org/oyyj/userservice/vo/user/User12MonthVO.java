package org.oyyj.userservice.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户12个月以来的用户增长量
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User12MonthVO {

    private List<String> monthList;

    private List<Integer> userCountList;

}
