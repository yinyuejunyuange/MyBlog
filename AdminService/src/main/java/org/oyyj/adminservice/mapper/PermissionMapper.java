package org.oyyj.adminservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.oyyj.adminservice.pojo.Permission;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("select distinct permission.permission from admin_role" +
            " left join role on admin_role.role_id = role.id " +
            " left join role_permission on role.id = role_permission.role_id " +
            " left join permission on role_permission.permission_id=permission.id " +
            " where admin_id=#{adminId} and permission.is_using=1 and role.is_using=1 and admin_role.is_valid=1 and role_permission.is_valid=1")
    List<String> getAdminPermissions(@Param("adminId") Long adminId);  // 通过左连接查找所有有关的数据
}
