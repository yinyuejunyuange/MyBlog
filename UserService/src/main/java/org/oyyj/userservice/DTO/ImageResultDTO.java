package org.oyyj.userservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImageResultDTO {
    private Integer errno;
    private Object data;

    public ImageResultDTO(String url) {
        this.errno = 0;
        Map<String,String> map=new HashMap<String,String>();
        map.put("url", url);
        this.data=map;
    }
}
