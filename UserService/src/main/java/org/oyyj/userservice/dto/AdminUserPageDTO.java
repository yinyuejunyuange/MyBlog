package org.oyyj.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserPageDTO {
    private Long totalPage;
    private Integer currentPage;
    private List<AdminUserDTO> users;
}
