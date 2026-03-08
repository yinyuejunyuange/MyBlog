package org.oyyj.chatservice.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.chatservice.mapper.MqMessageLogMapper;
import org.oyyj.chatservice.pojo.MqMessageLog;
import org.oyyj.chatservice.service.IMqMessageLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MQ消息记录服务实现类
 */
@Service
public class MqMessageLogServiceImpl extends ServiceImpl<MqMessageLogMapper, MqMessageLog>
        implements IMqMessageLogService {

    @Override
    public MqMessageLog createPendingLog(MqMessageLog log) {
        // 设置默认值
        if (log.getStatus() == null) {
            log.setStatus(0); // 待发送
        }
        if (log.getRetryCount() == null) {
            log.setRetryCount(0);
        }
        if (log.getIsDelete() == null) {
            log.setIsDelete(0);
        }
        this.save(log);
        return log;
    }

    @Override
    public boolean markAsSent(String messageId) {
        return updateStatus(messageId, 1, null);
    }

    @Override
    public boolean markAsDelivered(String messageId) {
        return updateStatus(messageId, 2, null);
    }

    @Override
    public boolean markAsConsumed(String messageId) {
        return updateStatus(messageId, 3, null);
    }

    @Override
    public boolean markAsFailed(String messageId, String errorMsg) {
        LambdaQueryWrapper<MqMessageLog> wrapper = Wrappers.lambdaQuery(MqMessageLog.class)
                .eq(MqMessageLog::getMessageId, messageId);
        MqMessageLog updateEntity = new MqMessageLog();
        updateEntity.setStatus(4); // 消费失败
        updateEntity.setErrorMsg(errorMsg);
        // 重试次数增加1（需要在查询原记录基础上增加，此处简化，也可通过SQL实现 +1）
        // 更好的做法是先查询再更新，这里展示思路
        MqMessageLog existing = this.getOne(wrapper);
        if (existing != null) {
            updateEntity.setRetryCount(existing.getRetryCount() + 1);
        }
        return this.update(updateEntity, wrapper);
    }

    @Override
    public List<MqMessageLog> getRetryMessages(int maxRetry, int limit) {
        LambdaQueryWrapper<MqMessageLog> wrapper = Wrappers.lambdaQuery(MqMessageLog.class)
                .in(MqMessageLog::getStatus, 4, 5) // 消费失败或发送失败
                .lt(MqMessageLog::getRetryCount, maxRetry) // 重试次数未超限
                .orderByAsc(MqMessageLog::getCreatedAt) // 按创建时间顺序重试
                .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    /**
     * 通用更新状态方法
     */
    private boolean updateStatus(String messageId, int status, String errorMsg) {
        LambdaQueryWrapper<MqMessageLog> wrapper = Wrappers.lambdaQuery(MqMessageLog.class)
                .eq(MqMessageLog::getMessageId, messageId);
        MqMessageLog updateEntity = new MqMessageLog();
        updateEntity.setStatus(status);
        updateEntity.setErrorMsg(errorMsg);
        return this.update(updateEntity, wrapper);
    }
}