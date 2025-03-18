package org.oyyj.adminservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.adminservice.dto.MenuDTO;
import org.oyyj.adminservice.pojo.SysMenu;
import org.springframework.stereotype.Service;

import java.util.List;


public interface ISysMenuService extends IService<SysMenu> {

    List<MenuDTO> adminMenu(Long roleId);

}
