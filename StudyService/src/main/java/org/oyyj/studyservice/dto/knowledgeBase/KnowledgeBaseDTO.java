package org.oyyj.studyservice.dto.knowledgeBase;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.studyservice.pojo.KnowledgeBase;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeBaseDTO {

    private String id;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String name;
    private String icon;
    private String description;
    private List<String> types;
    private List<String> knowledgeIds;

    public KnowledgeBaseDTO entityToDTO(KnowledgeBase entity) {
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        BeanUtils.copyProperties(entity, dto);
        if(entity.getId()!=null){
            dto.setId(entity.getId().toString());
        }
        if(entity.getCategory()!=null){
            dto.setTypes(List.of(entity.getCategory().split(",")));
        }
        return dto;
    }

    public KnowledgeBase dtoToEntity(KnowledgeBaseDTO dto) {
        KnowledgeBase entity = new KnowledgeBase();
        BeanUtils.copyProperties(dto, entity);
        if(dto.getId()!=null){
            entity.setId(Long.parseLong(dto.getId()));
        }
        if(dto.getTypes()!=null  && !dto.getTypes().isEmpty()){
            entity.setCategory(String.join(",", dto.getTypes()));
        }
        return entity;
    }
}
