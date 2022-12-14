package com.ltyzzz.core.service;



import com.ltyzzz.interfaces.common.DataService;

import java.util.ArrayList;
import java.util.List;

public class DataServiceImpl implements DataService {

    @Override
    public String sendData(String body) {
        System.out.println("己收到的参数长度："+ body.length() + "，内容：" + body);
        return "success";
    }

    @Override
    public List<String> getList() {
        ArrayList arrayList = new ArrayList();
        arrayList.add("idea1");
        arrayList.add("idea2");
        arrayList.add("idea3");
        return arrayList;
    }

    @Override
    public String testErrorV2() {
        //throw new RuntimeException("测试异常");
        return "three";
    }
}
