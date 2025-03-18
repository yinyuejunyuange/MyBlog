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
public class PageBlogDTO {
    private List<BlogDTO> pageList;
    private Integer total;
    private Integer pageNow;
    private Integer pageSize;
}
