package org.oyyj.gatewaydemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.gatewaydemo.pojo.User;


@Mapper
public interface UserMapper extends BaseMapper<User> {
}
