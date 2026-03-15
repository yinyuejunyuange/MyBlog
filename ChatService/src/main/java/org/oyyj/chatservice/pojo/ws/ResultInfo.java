package org.oyyj.chatservice.pojo.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultInfo {
    /**
     * 响应类型 RESULT 返回消息结果，  SEND 推送消息， PONG 心跳
     */
    private String type;

    private String msgId;

    private String result;

    private Object data;

    private String msg;

    @Getter
    public enum ResultInfoEnum{

        SUCCESS("success"),
        DATA_WRONG("data_wrong"),
        REPEAT("repeat"),
        FAIL_SEND("fail_send"),
        CONDITION_FAIL("condition_fail"),
        FAIL_SAVE("fail_save");

        private final String value;

        private ResultInfoEnum(String value){
            this.value = value;
        }

    }

}
