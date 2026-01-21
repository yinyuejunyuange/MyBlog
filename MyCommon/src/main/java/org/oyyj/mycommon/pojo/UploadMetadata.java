package org.oyyj.mycommon.pojo;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * 断点续传定义类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("upload_metadata")
public class UploadMetadata {
    //上传 upload id
    private Long uploadId;
    // 文件唯一标识
    private String fileNo;
    // 当前片数
    private Long chunkNum;
    // 类型
    private Integer fileType;
    // 文件MD5
    private String fileMd5;
    // 文件分片md5
    private String chunkMd5;
    // MINIO 桶名
    private String bucketName;
    // MINIO 对象名
    private String objectName;
    // 分片大小（字节 >= 5MB  MINIO 官方限制）
    private Long partSize;
    // 文件总片数
    private Long totalFileChunks;
    // 创建时间
    private Date createTime;
    @TableLogic
    private Integer isDeleted;

    @Getter
    public  enum MetaDataEnum{
        CHUNK("chunk",0),
        WHOLE("whole",1);
        private final String name;
        private final Integer value;
        MetaDataEnum(String name,Integer value){
            this.name=name;
            this.value=value;
        }

        public static MetaDataEnum getEnum(String name){
            for (MetaDataEnum metaDataEnum : MetaDataEnum.values()) {
                if (metaDataEnum.name.equals(name)) {
                    return metaDataEnum;
                }
            }
            return null;
        }

    }
}
