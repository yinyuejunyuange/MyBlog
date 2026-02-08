package org.oyyj.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.userservice.pojo.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    Integer userFunS(Long userID);

}
