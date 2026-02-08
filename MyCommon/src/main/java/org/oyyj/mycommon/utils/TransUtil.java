package org.oyyj.mycommon.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 数值类型转换工具
 */
public class TransUtil {

    /**
     * 数字转成字符串工具
     * @param value
     * @return
     */
    public static String formatNumber(long value) {
        if (value < 1000) {
            return String.valueOf(value);
        }

        if (value < 10_000) {
            return format(value, 1000);
        }

        return format(value, 10_000);
    }

    private static String format(long value, int divisor) {
        BigDecimal result = BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(divisor), 1, RoundingMode.HALF_UP);

        // 去掉末尾 .0（例如 1.0 → 1）
        return result.stripTrailingZeros().toPlainString();
    }
}
