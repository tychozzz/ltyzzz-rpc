package com.ltyzzz.core.event;

public class IRpcNodeChangeEvent implements IRpcEvent {

    private Object data;

    public IRpcNodeChangeEvent(Object data) {
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
