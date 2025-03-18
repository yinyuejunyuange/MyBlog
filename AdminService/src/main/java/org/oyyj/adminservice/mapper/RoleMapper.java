package org.oyyj.adminservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.oyyj.adminservice.pojo.Role;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("select distinct role.admin_type from admin_role" +
            " left join role on admin_role.role_id = role.id " +
            " where admin_id=#{adminId} and role.is_using=1 and admin_role.is_valid=1")
    List<String> getAdminRole(@Param("adminId") Long adminId);  // 通过左连接查找所有有关的数据
}
