package org.oyyj.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.oyyj.userservice.dto.User12MonthDTO;
import org.oyyj.userservice.pojo.User;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    Integer userFunS(Long userID);

    Integer userAttention(Long userID);

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as count " +
            "FROM user " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
            "AND is_delete = 0 " +
            "GROUP BY month " +
            "ORDER BY month ASC")
    List<User12MonthDTO> selectUserGrowthLast12Months();

}
