package org.oyyj.mycommon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.oyyj.mycommon.pojo.UploadMetadata;


@Mapper
public interface UploadMetadataMapper extends BaseMapper<UploadMetadata> {
    /**
     * 获取文件的最大分片
     * @param fileNo
     * @return
     */
    Long getFileMaxChunkNumber(@Param("fileNo") String fileNo);

}
