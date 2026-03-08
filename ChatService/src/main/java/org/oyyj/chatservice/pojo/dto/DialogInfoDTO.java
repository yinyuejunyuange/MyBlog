package org.oyyj.chatservice.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DialogInfoDTO {

    private String dialogId;

    private String snowflakeId;

}
