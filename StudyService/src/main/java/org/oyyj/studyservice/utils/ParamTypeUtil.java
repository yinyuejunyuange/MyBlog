package org.oyyj.studyservice.utils;

import org.apache.logging.log4j.util.Strings;

public class ParamTypeUtil {

    public static Long toLong(String value){
        if(value==null || Strings.isBlank(value)){
            return null;
        }
        return Long.valueOf(value);
    }



}
