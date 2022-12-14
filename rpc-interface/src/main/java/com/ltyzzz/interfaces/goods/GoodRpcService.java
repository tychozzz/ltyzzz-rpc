package com.ltyzzz.interfaces.goods;

import java.util.List;

public interface GoodRpcService {

    /**
     * 扣减库存
     */
    boolean decreaseStock();

    /**
     * 根据用户id查询购买过的商品信息
     *
     * @return
     */
    List<String> selectGoodsNoByUserId(String userId);
}
