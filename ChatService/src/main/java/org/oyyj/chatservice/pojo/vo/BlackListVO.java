package org.oyyj.chatservice.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlackListVO {

    private String blackUserId;

    private String userName;

    private String userHead;



}
