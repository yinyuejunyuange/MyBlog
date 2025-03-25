package org.oyyj.adminservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.adminservice.dto.AddMenuDTO;
import org.oyyj.adminservice.dto.MenuAdminDTO;
import org.oyyj.adminservice.dto.MenuDTO;
import org.oyyj.adminservice.dto.ModifyMenuDTO;
import org.oyyj.adminservice.pojo.SysMenu;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


public interface ISysMenuService extends IService<SysMenu> {

    List<MenuDTO> adminMenu(Long roleId);

    List<MenuAdminDTO> adminMenuAdmin();

    Map<String,Object> modifyMenu(ModifyMenuDTO modifyMenuDTO);

    Map<String,Object> modifySonMenu(ModifyMenuDTO modifyMenuDTO);

    Map<String,Object> addMenu(AddMenuDTO addMenuDTO);

    Map<String,Object> addSonMenu(AddMenuDTO addMenuDTO);

}
