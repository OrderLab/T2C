package edu.jhu.order.t2c.dynamicd.tscheduler;

import edu.jhu.order.t2c.dynamicd.runtime.GlobalState;
import edu.jhu.order.t2c.dynamicd.runtime.RuntimeTracer;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import edu.jhu.order.t2c.dynamicd.runtime.T2CHelper;
import edu.jhu.order.t2c.dynamicd.runtime.TemplateManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TestListener extends RunListener {

    enum MODE {
        CONTINOUS, ONESHOT, NONE;
    }

    private final static MODE mode = MODE.ONESHOT;

    private static final Description FAILED = Description.createTestDescription("failed", "failed");

    public void testStarted(Description description) {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("#### "+now.toString()+" Test " + description.getClassName() + ":" + description.getMethodName() + " started");

        String testName = description.getClassName() + "#" + description.getMethodName();
        T2CHelper.getInstance().setLastTestName(testName);
        TemplateManager.getInstance().startBuildTemplate(testName);

        //delete all futures, otherwise keep growing
        RuntimeTracer.getInstance().futures.clear();
        RuntimeTracer.getInstance().executedAssertionID.clear();
        RuntimeTracer.getInstance().historyTraces.clear();
        GlobalState.testStarted = true;
    }

    public void testFinished(Description description) {
        GlobalState.testStarted = false;
        LocalDateTime now = LocalDateTime.now();
        System.out.println("#### "+now.toString()+" Test " + description.getClassName() + ":" + description.getMethodName() + " ended");

        //try to delay if asserting not finished yet
        //if(GlobalState.mode.equals(GlobalState.T2CMode.ASSERTING))
        {
            try {
                Thread.sleep(1000);
            } catch (Exception ex){ex.printStackTrace();}
        }

        long startTime = System.currentTimeMillis();
        if(GlobalState.mode.equals(GlobalState.T2CMode.VALIDATION))
        {
            System.out.println("Pending checker task:"+ RuntimeTracer.getInstance().futures.size());
            int pending_count = 0;
            //blocking until all checkers finished
            for (Future<?> future : RuntimeTracer.getInstance().futures) {
                try {
                    pending_count++;
                    //System.out.println("#### Start to wait for No."+pending_count+" pending checker to finish");
                    future.get(100, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e ){
                    //System.out.println("#### Task timed out. Cancel No."+pending_count+" checker");
                    future.cancel(true);
                    continue;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //System.out.println("#### No."+pending_count+" pending checker finished");
            }
        }
        System.out.println("Blocker for pending checkers released: "+(System.currentTimeMillis()-startTime)/1000+" sec");

        String testName = description.getClassName() + "#" + description.getMethodName();

        if(GlobalState.mode.equals(GlobalState.T2CMode.VALIDATION))
        {
            if (description.getChildren().contains(FAILED))
            {
                //this test failed in the verifying phase, so we need to count that as well
                TemplateManager.getInstance().markTestFailed(testName);
            }
            else
                TemplateManager.getInstance().recordTemplatesResult(testName);

        }
        else if(GlobalState.mode.equals(GlobalState.T2CMode.BUILD))
        {
            TemplateManager.getInstance().finishBuildTemplate(testName);
        }
    }
}
