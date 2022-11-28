package com.ltyzzz.service;

public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHi(String name) {
        return "Hi, " + name;
    }
}
