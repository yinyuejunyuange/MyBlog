package org.oyyj.mycommonbase.utils;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ResultUtil<T> {
    private int code;
    private String message;
    private T data;

    public ResultUtil(T data) {
        this.code = 200;
        this.message = "success";
        this.data = data;
    }

    public ResultUtil(T data, boolean success, String message) {
        if (success) {
            this.code = 200;
            this.message = "success";
        } else {
            this.code = 400;
            this.message = message;
        }
        this.data = data;
    }

    public ResultUtil(int code, String message) {
        this.code = code;
        this.message = message;
        this.data = null;
    }

    public static <T> ResultUtil<T> success(T data) {
        return new ResultUtil<>(data);
    }

    public static <T> ResultUtil<T> fail(String message) {
        return new ResultUtil<>(400, message);
    }

    public static <T> ResultUtil<T> fail(int code, String message) {
        return new ResultUtil<>(code, message);
    }

    public static Map<String,Object> failMap(String message) {
        Map<String,Object>map =new HashMap<>();

        map.put("code",400);
        map.put("msg",message);

        return map;
    }

    public static <T>Map<String,Object> successMap(T data,String message) {
        Map<String,Object>map =new HashMap<>();


        map.put("code",200);
        map.put("msg",message);
        map.put("data",data);


        return map;
    }


}
