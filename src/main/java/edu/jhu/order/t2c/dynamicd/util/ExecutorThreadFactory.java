package edu.jhu.order.t2c.dynamicd.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import edu.jhu.order.t2c.dynamicd.runtime.RuntimeTracer;
import edu.jhu.order.t2c.dynamicd.runtime.T2CHelper;

public class ExecutorThreadFactory implements ThreadFactory {
    private int threshold = 10;
    private final AtomicInteger threadsNum;

    public ExecutorThreadFactory() {
        threadsNum = new AtomicInteger(threshold);
    }

    public ExecutorThreadFactory(int newThreshold) {
        threshold = newThreshold;
        threadsNum = new AtomicInteger(threshold);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        if (threadsNum.decrementAndGet() <= 0) {
            T2CHelper.prodLogInfo("Success: " + RuntimeTracer.getInstance().success.get() + " Fail: "
                    + RuntimeTracer.getInstance().fail.get() +"\n"+"SuccessMap: "+RuntimeTracer.getInstance().successMap+"\n"+"FailMap: "+RuntimeTracer.getInstance().failMap);
            threadsNum.set(threshold);
        }
        return new Thread(runnable);
    }
}
