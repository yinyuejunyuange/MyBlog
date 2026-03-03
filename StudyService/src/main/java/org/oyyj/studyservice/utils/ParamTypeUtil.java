package org.oyyj.studyservice.utils;

public class ParamTypeUtil {

    public static Long toLong(String value){
        if(value==null){
            return null;
        }
        return Long.valueOf(value);
    }



}
