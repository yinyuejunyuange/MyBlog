package org.oyyj.userservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JWTUser {
    private String id;
    private String username;
    private String imageUrl;
    private String token;

    private Boolean isValid=false; // 默认都是false
}
