package edu.jhu.order.t2c.dynamicd.runtime;

import edu.jhu.order.t2c.dynamicd.tscheduler.TestEngine;

import java.lang.reflect.Method;
import java.util.Arrays;


/**
 *  Main entry for wrapped system instance
    we need to do dynamic instrumentation before everything else, so the production system must be started using this entry
    systems usually have their own startup scripts, for example,
    zookeeper: modify zkServer.sh,
          ZOOMAIN="org.apache.zookeeper.server.quorum.QuorumPeerMain"
              ->
          ZOOMAIN="edu.jhu.order.t2c.dynamicd.runtime.MainWrapper org.apache.zookeeper.server.quorum.QuorumPeerMain"
    hdfs: modify libexec/hadoop-functions.sh,
          in function hadoop_start_daemon
          exec "${JAVA}" "-Dproc_${command}" ${HADOOP_OPTS} "${class}" "$@"
          ->
          exec "${JAVA}" "-Dproc_${command}" ${HADOOP_OPTS} "oathkeeper.engine.MainWrapper" "${class}" "$@"
 */

public class MainWrapper {
    private static void invokeMainClass(String[] args) {
        //invoke original main class
        try {
            Class<?> cls = Class.forName(args[0]);
            Method meth = cls.getMethod("main", String[].class);
            String[] params = Arrays.copyOfRange(args, 1, args.length);
            meth.invoke(null, (Object) params);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Check the main class arg!");
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Cannot find main method in target class" + args[0]);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Exception when trying to invoke");
        }
    }

    void start()
    {
        ConfigManager.initConfig();

        //load runtime mode
        String runtimeMode = System.getProperty("t2c.mode");
        if (runtimeMode == null || runtimeMode.equals("") || runtimeMode.equals("prod"))
        {
            //default setting with t2c checkers, simply run the system with operations instrumented
            TestEngine.modifyOperationEntry();
        }
        else if (runtimeMode.equals("invivo"))
        {

            System.out.println("In-vivo runtime checker mode activated.");

            Runnable testEngineRunnable = () -> {
                //https://github.com/OrderLab/zookeeper/compare/release-3.6.1...test-checker-3.6.1
                while(true)
                {
                    // Sleep 10 seconds after a round is finished and at the start
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    TestEngine.runAllTests();
                }
            };

            Thread testEngineThread = new Thread(testEngineRunnable);
            testEngineThread.start();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("No enough args!");
        }

        System.out.println("Starting using MainWrapper.");

        MainWrapper mainWrapper = new MainWrapper();
        mainWrapper.start();

        invokeMainClass(args);

        System.out.println("Success: "+RuntimeTracer.getInstance().success.get());
        System.out.println("Fail: "+RuntimeTracer.getInstance().fail.get());
        // System.out.println("Skip: "+RuntimeTracer.getInstance().skip.get());
    }
}
