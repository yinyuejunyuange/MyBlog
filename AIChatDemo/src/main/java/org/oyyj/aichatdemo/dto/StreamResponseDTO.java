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
public class StreamResponseDTO {
    private String uuid;
    private List<Source> sources;
    private String type;
    private String textResponse;
    private Boolean close;
    private Boolean error;
    private Long chatId;
    private Metrics metrics;

}
