package com.ltyzzz.provider.service;

import com.ltyzzz.interfaces.pay.PayRpcService;
import com.ltyzzz.spring.common.IRpcService;

import java.util.List;

@IRpcService
public class PayRpcServiceImpl implements PayRpcService {
    @Override
    public boolean doPay() {
        return false;
    }

    @Override
    public List<String> getPayHistoryByGoodNo(String goodNo) {
        return null;
    }
}
