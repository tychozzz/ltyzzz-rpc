package com.ltyzzz.core.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommonUtils {

    /**
     * 获取目标对象的实现接口
     *
     * @param targetClass
     * @return
     */
    public static List<Class<?>> getAllInterfaces(Class targetClass){
        if(targetClass==null){
            throw new IllegalArgumentException("targetClass is null!");
        }
        Class[] clazz = targetClass.getInterfaces();
        if(clazz.length==0){
            return Collections.emptyList();
        }
        List<Class<?>> classes = new ArrayList<>(clazz.length);
        for (Class aClass : clazz) {
            classes.add(aClass);
        }
        return classes;
    }
}
