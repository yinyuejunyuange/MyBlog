package org.oyyj.chatservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.chatservice.pojo.Blacklist;

public interface BlacklistService extends IService<Blacklist> {

    void addBlacklist(Long userId,Long blackUserId);

    void removeBlacklist(Long userId,Long blackUserId);

}
