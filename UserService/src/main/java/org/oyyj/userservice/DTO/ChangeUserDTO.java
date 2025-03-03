package org.oyyj.userservice.DTO;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

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
