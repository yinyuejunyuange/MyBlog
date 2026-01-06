package org.oyyj.mycommon.utils;

import cn.hutool.core.lang.Snowflake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeUtil {
    @Autowired
    private Snowflake snowflake;

    public  String getSnowflakeId(){
        long id = snowflake.nextId();
        return String.valueOf(id);
    }
}
