package org.oyyj.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.oyyj.userservice.pojo.SysRole;
import org.springframework.security.core.parameters.P;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Insert("insert into user_role(user_id, role_id) value(#{userId},#{roleId})")
    Integer defaultSetUser(@Param("userId")Long userId, @Param("roleId") Long roleId); //默认所有新注册的用户权限 都是user

    @Select("select id from sys_role where role_name=#{name}")
    Long selectRoleBuName(@Param("name") String name);

}
