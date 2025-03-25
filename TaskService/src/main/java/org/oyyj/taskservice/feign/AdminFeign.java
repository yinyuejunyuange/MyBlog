package org.oyyj.taskservice.feign;

import org.oyyj.taskservice.config.FeignUserConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "AdminService",configuration = FeignUserConfiguration.class)
public interface AdminFeign {
    @GetMapping("/admin/getAdminIdByNameOrPhone")
    List<Long> getAdminIdByNameOrPhone(@RequestParam("admin") String admin);
}
