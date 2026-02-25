package org.oyyj.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 攻击性预测结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TextToxicAnalysisResultDTO {

    /**
     * 输入文本内容
     */
    private String inputText; // 对应 input_text（驼峰命名规范）

    /**
     * 是否有毒性（布尔值）
     */
    private Integer isToxic; // 对应 is_toxic 1 具有攻击性 0 不具有攻击性

    /**
     * 毒性概率（建议用 BigDecimal 避免浮点精度丢失，也可根据需求改为 Double）
     */
    private BigDecimal pToxic; // 对应 p_toxic

    /**
     * 多类型标识（根据业务场景可改为 Integer/String，这里默认 String 适配更多场景）
     */
    private List<String> mulType; // 对应 mul_type

    /**
     * 主题概率映射（key=主题ID，value=该主题的概率）
     */
    private Map<String, BigDecimal> pTopic; // 对应 p_topic


}
