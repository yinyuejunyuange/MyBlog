package org.oyyj.gatewaydemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.oyyj.gatewaydemo.pojo.SysPermissions;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissions> {

    // 通过用户id查询 所有权限
    @Select("select distinct sys_permissions.permissions_name from user_role \n" +
            "left join sys_role  on user_role.role_id=sys_role.id\n" +
            "left join role_permissions on user_role.role_id=role_permissions.role_id\n" +
            "left join sys_permissions on role_permissions.permissions_id=sys_permissions.id\n" +
            "where user_id=#{userId} and sys_role.is_delete=0 and sys_role.is_stop=0 and sys_permissions.is_stop=0 and sys_permissions.is_delete=0;")
    public List<String> getPermissionsByUserId(@Param("userId") Long userId);
}
