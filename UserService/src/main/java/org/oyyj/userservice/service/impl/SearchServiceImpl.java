package org.oyyj.userservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.userservice.DTO.HotSearchDTO;
import org.oyyj.userservice.mapper.SearchMapper;
import org.oyyj.userservice.pojo.Search;
import org.oyyj.userservice.service.ISearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchServiceImpl extends ServiceImpl<SearchMapper, Search> implements ISearchService {

    @Autowired
    private SearchMapper searchMapper;

    @Override
    public List<HotSearchDTO> getHotSearch() {
        return searchMapper.getHotSearch();
    }
}
