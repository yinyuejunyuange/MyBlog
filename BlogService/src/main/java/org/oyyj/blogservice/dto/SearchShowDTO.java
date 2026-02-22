package org.oyyj.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchShowDTO {
    private List<String> history;

    private List<String> hotSearch;
}
