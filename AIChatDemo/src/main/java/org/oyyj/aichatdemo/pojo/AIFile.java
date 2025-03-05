package org.oyyj.aichatdemo.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("file")
public class AIFile {
    @TableId("id")
    private Long id;
    @TableField("file_address")
    private String fileAddress;
    @TableField("file_name_json")
    private String fileNameJson; // 存储上传到工作区后的文件
    @TableField("is_upload")
    private Integer isUpload;// 判断 是否上传成功
    @TableField("is_delete")
    private Integer isDelete;// 是否删除

}
