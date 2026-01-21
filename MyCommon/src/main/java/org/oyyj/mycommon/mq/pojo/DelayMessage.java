package org.oyyj.mycommon.mq.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 延时队列消息结构
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DelayMessage {

    private String messageId;

    private Integer delayType;

    private String content;

    @Getter
    public enum DelayMessageType{
        FILE_CHUNK_CLEAN("分片文件清理",1);

        private final String name;
        private final int type;
        DelayMessageType(String name, int type){
            this.name = name;
            this.type = type;
        }
    }

}
