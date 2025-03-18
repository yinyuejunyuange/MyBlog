package org.oyyj.adminservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuDTO {
    private String id;
    private String menuName;
    private String menuUrl;
    private String parentId;
    private Integer sort;
    List<MenuDTO> children;
}
