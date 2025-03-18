package org.oyyj.adminservice.pojo;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JWTAdmin {
    @NonNull
    private String id;
    @NonNull
    private String name;
    @NonNull
    private String imageUrl;
    @NonNull
    private String token;

    private Boolean isValid=false;
}
