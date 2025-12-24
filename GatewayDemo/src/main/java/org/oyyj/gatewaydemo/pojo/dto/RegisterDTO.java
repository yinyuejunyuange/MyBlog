package org.oyyj.gatewaydemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterDTO {
    private String username;
    private String password;
    private Integer sex;
    private String email;
    private String phone;
    private List<String> searchList;

}
