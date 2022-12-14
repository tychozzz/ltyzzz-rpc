package com.ltyzzz.interfaces.common;

import java.util.List;

public interface DataService {

    /**
     * 发送数据
     *
     * @param body
     */
    String sendData(String body);

    /**
     * 获取数据
     *
     * @return
     */
    List<String> getList();

    /**
     * 异常测试方法
     */
    String testErrorV2();
}
