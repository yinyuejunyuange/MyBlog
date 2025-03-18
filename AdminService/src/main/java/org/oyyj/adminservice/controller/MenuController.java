package org.oyyj.adminservice.controller;

import org.oyyj.adminservice.dto.MenuDTO;
import org.oyyj.adminservice.service.IAdminService;
import org.oyyj.adminservice.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.security.sasl.AuthenticationException;
import java.rmi.MarshalledObject;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/menu")
public class MenuController {

    @Autowired
    private IAdminService adminService;

    @GetMapping("/getMenu")
    @PreAuthorize("hasAnyAuthority('admin','super_admin')")
    public Map<String,Object> getMenu() throws AuthenticationException {
        List<MenuDTO> menuDTO = adminService.getMenuDTO();
        return ResultUtil.successMap(menuDTO,"查询成功");
    }

}
