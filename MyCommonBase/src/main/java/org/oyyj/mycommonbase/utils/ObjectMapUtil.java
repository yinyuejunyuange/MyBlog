package org.oyyj.mycommonbase.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * 对象和类两者的转换
 */
public class ObjectMapUtil {

    private final static ObjectMapper mapper = new ObjectMapper();

    /**
     * 将类转换成字符串
     * @param classInfo
     * @param obj
     * @return
     * @param <T>
     */
    public static <T> Map<String,String> toMap(Class<T> classInfo, Object obj){
        if(obj==null){
            throw new NullPointerException("传入对象为空！");
        }
        if(classInfo==null){
            throw new NullPointerException("传入类型为空！");
        }
        if(!classInfo.isInstance(obj)){
            throw new ClassCastException("类变量和对象类型无关！");
        }
        Field[] fields = classInfo.getDeclaredFields();
        Map<String,String> map = new HashMap<>();
        for(Field field:fields){
            try {
                field.setAccessible(true); // 允许访问私有变量
                String name = field.getName();
                Object value = field.get(obj);

                String strValue = convertObjectToString(value, field.getType());
                map.put(name,strValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("读取字段[" + field.getName() + "]值失败", e);
            }
        }
        return map;
    }

    /**
     * 将map转换成类
     * @param classInfo
     * @param map
     * @return
     * @param <T>
     */
    public static <T> T toBean(Class<T> classInfo, Map<String,String> map){
        if(map==null){
            throw new NullPointerException("传入对象为空！");
        }
        if(classInfo==null){
            throw new NullPointerException("传入类型为空！");
        }
        try {
            T object = classInfo.getDeclaredConstructor().newInstance();
            Field[] fields = classInfo.getDeclaredFields();
            for(Field field:fields){
                field.setAccessible(true);
                String name = field.getName();
                String valueStr = map.get(name);
                Object value = convertStringToObject(valueStr, field.getType());
                field.set(object,value);
            }
            return object;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("map转换失败");
        }
    }


    /**
     * 辅助方法：将任意对象转为字符串（适配Redis Hash的字符串值）
     * @param obj       要转换的对象
     * @param fieldType 字段的类型
     * @return 字符串值（null返回空字符串）
     */
    private static String convertObjectToString(Object obj, Class<?> fieldType) {
        if (obj == null) {
            return "";
        }
        // 基本类型/包装类直接转字符串
        if (fieldType.isPrimitive() || fieldType == String.class ||
                fieldType == Integer.class || fieldType == Long.class ||
                fieldType == Boolean.class || fieldType == Double.class ||
                fieldType == Float.class || fieldType == Short.class ||
                fieldType == Byte.class) {
            return obj.toString();
        }else{
            try {
                return mapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                // 如需支持更多类型（如Date），可在此扩展（例：Date转时间戳字符串）
                throw new IllegalArgumentException("不支持的字段类型：" + fieldType.getName());
            }
        }

    }

    /**
     * 辅助方法：将字符串转为指定类型的对象
     * @param valueStr  字符串值
     * @param fieldType 目标类型
     * @return 转换后的对象
     */
    private static Object convertStringToObject(String valueStr, Class<?> fieldType) {
        // 处理基本类型（int → Integer，long → Long等）
        Class<?> wrapType = fieldType.isPrimitive() ? getPrimitiveWrapperType(fieldType) : fieldType;

        // 按类型转换
        if (wrapType == String.class) {
            return valueStr;
        } else if (wrapType == Integer.class) {
            return Integer.parseInt(valueStr);
        } else if (wrapType == Long.class) {
            return Long.parseLong(valueStr);
        } else if (wrapType == Boolean.class) {
            return Boolean.parseBoolean(valueStr);
        } else if (wrapType == Double.class) {
            return Double.parseDouble(valueStr);
        } else if (wrapType == Float.class) {
            return Float.parseFloat(valueStr);
        } else if (wrapType == Short.class) {
            return Short.parseShort(valueStr);
        } else if (wrapType == Byte.class) {
            return Byte.parseByte(valueStr);
        }else{
            try {
                return mapper.readValue(valueStr, wrapType);
            } catch (JsonProcessingException e) {
                // 如需支持更多类型（如Date），可在此扩展（例：Date转时间戳字符串）
                throw new IllegalArgumentException("不支持的字段类型：" + fieldType.getName());
            }
        }
    }

    /**
     * 辅助方法：获取基本类型对应的包装类 获取其对应的转换方法
     * 例：int.class → Integer.class，long.class → Long.class
     */
    private static Class<?> getPrimitiveWrapperType(Class<?> primitiveType) {
        if (primitiveType == int.class) {
            return Integer.class;
        } else if (primitiveType == long.class) {
            return Long.class;
        } else if (primitiveType == boolean.class) {
            return Boolean.class;
        } else if (primitiveType == double.class) {
            return Double.class;
        } else if (primitiveType == float.class) {
            return Float.class;
        } else if (primitiveType == short.class) {
            return Short.class;
        } else if (primitiveType == byte.class) {
            return Byte.class;
        } else if (primitiveType == char.class) {
            return Character.class;
        }
        return primitiveType;
    }

}
