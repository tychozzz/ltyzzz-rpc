package com.ltyzzz.provider.service;

import com.ltyzzz.interfaces.goods.GoodRpcService;
import com.ltyzzz.spring.common.IRpcService;

import java.util.Arrays;
import java.util.List;

/**
 * @Author linhao
 * @Date created in 10:59 上午 2022/3/19
 */
@IRpcService
public class GoodRpcServiceImpl implements GoodRpcService {

    @Override
    public boolean decreaseStock() {
        return true;
    }

    @Override
    public List<String> selectGoodsNoByUserId(String userId) {
        return Arrays.asList(userId + "-good-01", userId + "-good-02");
    }
}
