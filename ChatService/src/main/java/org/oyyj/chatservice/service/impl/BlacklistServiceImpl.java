package org.oyyj.chatservice.service.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.chatservice.feign.UserFeign;
import org.oyyj.chatservice.mapper.BlacklistMapper;
import org.oyyj.chatservice.pojo.Blacklist;
import org.oyyj.chatservice.pojo.vo.BlackListVO;
import org.oyyj.chatservice.service.BlacklistService;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BlacklistServiceImpl
        extends ServiceImpl<BlacklistMapper, Blacklist>
        implements BlacklistService {

    private final UserFeign userFeign;

    public BlacklistServiceImpl(UserFeign userFeign) {
        this.userFeign = userFeign;
    }

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

    /**
     * 获取用户
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public ResultUtil<Page<BlackListVO>> listBlackUser(Long userId, Integer pageNum, Integer pageSize) {
        if(pageNum==null||pageNum<=0){
            pageNum=1;
        }
        if(pageSize==null|| pageSize<=0){
            pageSize=10;
        }
        Page<Blacklist> page = new Page<>(pageNum, pageSize);
        Page<Blacklist> pageResult = page(page, Wrappers.<Blacklist>lambdaQuery()
                .eq(Blacklist::getUserId, userId)
        );

        List<Blacklist> records = pageResult.getRecords();
        List<String> userIds = records.stream().map(item -> String.valueOf(item.getBlackUserId())).toList();

        Map<Long, String> nameInIdMap = userFeign.getNameInIds(userIds);
        Map<Long, String> imageInIdMap = userFeign.getImageInIds(userIds);

        List<BlackListVO> list = records.stream().map(item -> {
            BlackListVO blackListVO = new BlackListVO();
            blackListVO.setBlackUserId(String.valueOf(item.getBlackUserId()));
            if (nameInIdMap.containsKey(item.getBlackUserId())) {
                blackListVO.setUserName(nameInIdMap.get(item.getBlackUserId()));
            }
            if (imageInIdMap.containsKey(item.getBlackUserId())) {
                blackListVO.setUserHead(imageInIdMap.get(item.getBlackUserId()));
            }
            return blackListVO;
        }).toList();

        Page<BlackListVO> pageVO = new Page<>();
        pageVO.setRecords(list);
        pageVO.setTotal(pageResult.getTotal());

        return ResultUtil.success(pageVO);


    }

}
