package org.oyyj.studyservice.vo.chatMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageVO {

    private String sessionId;

    private String baseName;

    private String firstQuestionName;

    private Date createTime;

}
