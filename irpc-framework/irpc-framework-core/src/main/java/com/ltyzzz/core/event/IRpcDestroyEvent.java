package com.ltyzzz.core.event;

/**
 * 服务销毁事件
 *
 * @Author linhao
 * @Date created in 3:20 下午 2022/1/8
 */
public class IRpcDestroyEvent implements IRpcEvent{

    private Object data;

    public IRpcDestroyEvent(Object data) {
        this.data = data;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public IRpcEvent setData(Object data) {
        this.data = data;
        return this;
    }
}
