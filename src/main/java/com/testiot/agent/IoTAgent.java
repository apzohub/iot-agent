package com.testiot.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

public abstract class IoTAgent implements Runnable {
    protected final Logger logger = LoggerFactory.getLogger(IoTAgent.class);

    @Override
    public void run() {
        logger.info( "Starting IoTAgent!" );

        addShutdownHook();

        once();

        while (!Thread.currentThread().isInterrupted()){
            exec();
        }
    }

    protected abstract void once();
    protected abstract void exec();

    void addShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread() {
            String pid = getPID();

            @Override
            public void run() {
                logger.info("Shutting down IoTAgent with PID " + pid);
            }
        });
    }

    String getPID(){
        try {
            return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        }catch(Exception e){
            logger.error("Unable to aquire PID", e);
        }
        return ManagementFactory.getRuntimeMXBean().getName();
    }


    public static void main( String[] args ) throws InterruptedException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class cl = Class.forName(Utils.props.getProperty("agent.type"));
        Thread t = new Thread((Runnable) cl.newInstance());
        t.start();
        t.join();
    }

}

