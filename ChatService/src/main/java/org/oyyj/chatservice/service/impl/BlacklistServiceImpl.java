package org.oyyj.chatservice.service.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.chatservice.mapper.BlacklistMapper;
import org.oyyj.chatservice.pojo.Blacklist;
import org.oyyj.chatservice.service.BlacklistService;
import org.springframework.stereotype.Service;

@Service
public class BlacklistServiceImpl
        extends ServiceImpl<BlacklistMapper, Blacklist>
        implements BlacklistService {

    @Override
    public void addBlacklist(Long userId, Long blackUserId) {

        Blacklist blacklist = new Blacklist();
        blacklist.setUserId(userId);
        blacklist.setBlackUserId(blackUserId);

        save(blacklist);
    }

    @Override
    public void removeBlacklist(Long userId, Long blackUserId) {

        remove(Wrappers.<Blacklist>lambdaQuery()
                .eq(Blacklist::getUserId,userId)
                .eq(Blacklist::getBlackUserId,blackUserId)
        );
    }

}
