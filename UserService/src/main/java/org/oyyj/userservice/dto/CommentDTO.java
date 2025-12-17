package org.oyyj.userservice.dto;

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
