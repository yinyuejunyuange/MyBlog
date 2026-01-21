package org.oyyj.mycommon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.mycommon.pojo.UploadMetadata;

import java.util.List;

public interface IUploadMetadataService extends IService<UploadMetadata> {

    /**
     * 获取最大分片
     * @param fileNo
     * @return
     */
    Long getFileMaxChunk(String fileNo);

    /**
     * 检查某个分片是否存在 如果存在就不重复上传
     * @param fileNo
     * @param chunkNum
     * @return
     */
    Boolean checkChunkExists(String fileNo,Integer chunkNum);

    /**
     * 检查某个文件所有的已经上传的分片
     * @param fileNo
     * @return
     */
    List<Long> getExistsChunks(String fileNo);
}
