package org.oyyj.taskservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobBean {
    private String jobName; // 任务名字
    private String jobClass; // 任务类
    private String cornExpression; // corn表达式
}
