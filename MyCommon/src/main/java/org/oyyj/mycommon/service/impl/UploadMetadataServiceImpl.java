package org.oyyj.mycommon.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.mycommon.mapper.UploadMetadataMapper;
import org.oyyj.mycommon.pojo.UploadMetadata;
import org.oyyj.mycommon.service.IUploadMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UploadMetadataServiceImpl extends ServiceImpl<UploadMetadataMapper,UploadMetadata>  implements IUploadMetadataService  {
    @Autowired
    private UploadMetadataMapper uploadMetadataMapper;


    @Override
    public Long getFileMaxChunk(String fileNo) {
        Long fileMaxChunkNumber = uploadMetadataMapper.getFileMaxChunkNumber(fileNo);
        return fileMaxChunkNumber==null?0:fileMaxChunkNumber;
    }

    @Override
    public Boolean checkChunkExists(String fileNo, Integer chunkNum) {
        List<UploadMetadata> list = list(Wrappers.<UploadMetadata>lambdaQuery()
                .eq(UploadMetadata::getFileNo, fileNo)
                .eq(UploadMetadata::getChunkNum, chunkNum)
                .eq(UploadMetadata::getFileType, UploadMetadata.MetaDataEnum.CHUNK.getValue())
        );

        return !list.isEmpty();
    }

    @Override
    public List<Long> getExistsChunks(String fileNo) {
        return list(Wrappers.<UploadMetadata>lambdaQuery()
                .eq(UploadMetadata::getFileNo, fileNo)
                .eq(UploadMetadata::getFileType, UploadMetadata.MetaDataEnum.CHUNK.getValue())
        ).stream().map(UploadMetadata::getChunkNum).toList();
    }
}
