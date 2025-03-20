package org.oyyj.taskservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.jsqlparser.util.validation.metadata.DatabaseException;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
/**
 * 查询 任务DTO
 */
public class TaskDTO {
    private Long adminId;
    private String taskName;
    private Date startTime;
    private Date endTime;
    private String status;}
