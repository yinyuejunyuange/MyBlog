package org.oyyj.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.userservice.dto.HotSearchDTO;
import org.oyyj.userservice.pojo.Search;

import java.util.List;

public interface ISearchService extends IService<Search> {

    List<HotSearchDTO> getHotSearch();
}
