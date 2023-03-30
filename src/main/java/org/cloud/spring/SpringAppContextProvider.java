package org.cloud.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringAppContextProvider {

    private SpringAppContextProvider() {
    }

    public static ApplicationContext getApplicationContext(String fileName) {
        final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(fileName);
        ctx.registerShutdownHook();
        return ctx;
    }
}
