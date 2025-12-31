package org.oyyj.mycommon.common;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SqlCommon {
    /**
     * 倒序
     */
    public static  String DESC = "desc";
    /**
     * 正序
     */
    public static String ASC = " asc";

}
