package org.oyyj.blogservice.config.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.config.pojo.ApiConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IApiConfigService extends IService<ApiConfig> {
    List<ApiConfig> findByIsActiveTrue();

    boolean subVipNum(Long userId);
}
