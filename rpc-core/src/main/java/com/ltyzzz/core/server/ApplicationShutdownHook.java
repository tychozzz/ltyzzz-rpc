package com.ltyzzz.core.server;

import com.ltyzzz.core.event.IRpcDestroyEvent;
import com.ltyzzz.core.event.IRpcListenerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationShutdownHook {

    public static Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownHook.class);

    public static void registryShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("[registryShutdownHook] ==== ");
                IRpcListenerLoader.sendSyncEvent(new IRpcDestroyEvent("destroy"));
                System.out.println("destroy");
            }
        }));
    }
}
