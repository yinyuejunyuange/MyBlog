package org.oyyj.userservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageListDTO<T> {
    private List<T> pageList;
    private Integer total;
    private Integer pageNow;
    private Integer pageSize;

}
