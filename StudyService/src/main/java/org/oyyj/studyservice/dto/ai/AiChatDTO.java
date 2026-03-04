package org.oyyj.studyservice.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatDTO {

    private String message;

    private String knowledgeBaseId;

}
