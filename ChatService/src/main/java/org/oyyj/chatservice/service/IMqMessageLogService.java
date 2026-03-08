package org.oyyj.chatservice.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.chatservice.pojo.MqMessageLog;


import java.util.List;

/**
 * MQ消息记录服务接口
 */
public interface IMqMessageLogService extends IService<MqMessageLog> {

    /**
     * 创建一条待发送的消息记录
     * @param log 消息记录（messageId, exchange, routingKey, content 等）
     * @return 保存后的记录
     */
    MqMessageLog createPendingLog(MqMessageLog log);

    /**
     * 标记消息为发送成功（更新状态为已发送）
     * @param messageId 消息ID
     * @return 是否成功
     */
    boolean markAsSent(String messageId);

    /**
     * 标记消息为已到达队列（可选）
     */
    boolean markAsDelivered(String messageId);

    /**
     * 标记消息消费成功
     */
    boolean markAsConsumed(String messageId);

    /**
     * 标记消息消费失败，并增加重试次数
     */
    boolean markAsFailed(String messageId, String errorMsg);

    /**
     * 获取需要重试的消息列表（状态为消费失败或发送失败，且重试次数未超限）
     * @param maxRetry 最大重试次数
     * @param limit 查询数量
     * @return 消息列表
     */
    List<MqMessageLog> getRetryMessages(int maxRetry, int limit);
}
