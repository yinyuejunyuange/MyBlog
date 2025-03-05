package org.oyyj.userservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AIFileDTO {
    private Long id;
    private String fileAddress;
    private String fileNameJson;
    private Integer isUpload;
    private Integer isDelete;
}
