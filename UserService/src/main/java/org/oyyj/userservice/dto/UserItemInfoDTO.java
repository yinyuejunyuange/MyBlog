package org.oyyj.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserItemInfoDTO {

    private String userName;

    private String imageHead;

    private String introduction;

}
