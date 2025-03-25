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
public class ModifyMenuDTO {
    private String id;
    private String name;
    private String url;
    private List<String> adminTypes;
    private Integer sort;
}
