package org.oyyj.userservice.DTO;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class CommentDTO {
    private String userId;
    @NonNull
    private String BlogId;
    @NonNull
    private String context;
}
