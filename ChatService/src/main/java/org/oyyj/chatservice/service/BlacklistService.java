package org.oyyj.chatservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.chatservice.pojo.Blacklist;
import org.oyyj.chatservice.pojo.vo.BlackListVO;
import org.oyyj.mycommonbase.utils.ResultUtil;

public interface BlacklistService extends IService<Blacklist> {

    void addBlacklist(Long userId,Long blackUserId);

    void removeBlacklist(Long userId,Long blackUserId);

    ResultUtil<Page<BlackListVO>> listBlackUser(Long userId, Integer pageNum, Integer pageSize);

}
