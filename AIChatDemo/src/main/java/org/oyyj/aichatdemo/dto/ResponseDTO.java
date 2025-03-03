package org.oyyj.aichatdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseDTO {
    private String id;
    private String type;
    private boolean close;
    private String error;
    private int chatId;
    private String textResponse;
    private List<Object> sources; // 假设 sources 是一个 Object 类型的列表
    private Metrics metrics;
}
