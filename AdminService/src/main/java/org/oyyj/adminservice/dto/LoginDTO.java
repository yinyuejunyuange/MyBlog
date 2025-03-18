package org.oyyj.adminservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginDTO {

    private String phone;

    private String password;

    private String encode;

    private String uuid;
}
