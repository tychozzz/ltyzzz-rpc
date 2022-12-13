package com.ltyzzz.core.registry;

public interface RegistryService {

    void register(URL url);

    void unRegister(URL url);

    void subscribe(URL url);

    void unSubscribe(URL url);
}
