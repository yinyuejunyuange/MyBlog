package org.oyyj.chatservice.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DialogCreateVO {

    private String dialogId;

    private String snowflakeId;

}
