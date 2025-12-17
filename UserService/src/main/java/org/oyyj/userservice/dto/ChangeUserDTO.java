package org.oyyj.userservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeUserDTO {
    private String userName;
    private String email;
    private Integer sex; //1 男 0 女
    private String introduce;
}
