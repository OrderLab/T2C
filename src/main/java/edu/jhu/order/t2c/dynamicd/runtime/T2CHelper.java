package edu.jhu.order.t2c.dynamicd.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class T2CHelper {
    //for efficient run use
    private static final boolean DISABLE_PRODINFO = false;

    private static final Logger LOG = LoggerFactory.getLogger(T2CHelper.class);
    private static T2CHelper helper = new T2CHelper();

    static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static final String LOGFILEPREFIX = "t2clog_";
    public String lastTestName = "";

    //log for each test case
    private Writer logwriter;
    private String logFileName;
    private PrintStream logStream;

    //shared log for all test cases
    private Writer globalLogwriter;
    public String globalLogFileName = "t2ctests.summary";

    //runtime log in production
    public Writer productionLogwriter;
    public String productionLogFileName = "t2c.prod.log";

    //util log for some metrics

    private Writer testwopListwriter;
    public static String testwopListFileName = "t2c.tests.with.op";
    private Writer testwassertListwriter;
    public static String testwassertListFileName = "t2c.tests.with.assert";
    private Writer testwassertsucListwriter;
    public static String testwassertsucListFileName = "t2c.tests.with.assert.suc";

    private void init() {
        // init in lazyway because we need to wait until test case to feed us log name
        try {
            //in cassandra we didn't get per-test name, we have to run one-by-one manually
            logFileName = LOGFILEPREFIX + lastTestName;
            logStream =  new PrintStream(new FileOutputStream(logFileName));
            logwriter = new BufferedWriter(new OutputStreamWriter(logStream));

            globalLogwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(globalLogFileName)));

            productionLogwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(productionLogFileName)));

            testwopListwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(testwopListFileName,true)));

            testwassertListwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(testwassertListFileName,true )));

            testwassertsucListwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(testwassertsucListFileName,true )));
        } catch (IOException ex) {
            LOG.error("Experience exception when logging", ex);
        }
    }

    public static T2CHelper getInstance() {
        return helper;
    }

    void logInfoInternal(String str) {
        //if we move to another test, re-init
        if (logFileName == null || !logFileName.equals(LOGFILEPREFIX + lastTestName))
            init();

        try {
            logwriter.write(str + "\n");
            logwriter.flush();
        } catch (IOException ex) {
            LOG.error("Experience exception when logging", ex);
        }
    }

    public static void logInfo(String str) {
        getInstance().logInfoInternal(str);

    }

    public PrintStream getLogStream()
    {
        return logStream;
    }

    void globallogInfoInternal(String str) {
        if (globalLogwriter == null)
            init();

        try {
            globalLogwriter.write(str + "\n");
            globalLogwriter.flush();
        } catch (IOException ex) {
            LOG.error("Experience exception when logging", ex);
        }
    }

    public PrintWriter getGlobalLogWriter()
    {
        return new PrintWriter(globalLogwriter);
    }

    public static void globalLogInfo(String str) {
        getInstance().globallogInfoInternal(str);

    }

    public Writer getProdlogInfoInternal()
    {
        Writer writer = null;
        if (productionLogwriter == null || logwriter == null)
            init();
        if(GlobalState.mode.equals(GlobalState.T2CMode.PRODUCTION))
        {
            writer = productionLogwriter;
        }
        else if(GlobalState.mode.equals(GlobalState.T2CMode.VALIDATION))
        {
            writer = logwriter;
        }
        else if(GlobalState.mode.equals(GlobalState.T2CMode.UNITTEST))
        {
            writer = productionLogwriter;
        }
        else {
            writer = productionLogwriter;
            //System.out.println("WARNING: prodlogInfoInternal into weird branch");
        }
        return writer;
    }

    void prodlogInfoInternal(String str) {
        Writer writer = getProdlogInfoInternal();

        try {
            String timeStamp = "["+dateFormat.format(Calendar.getInstance().getTime())+"] ";
            writer.write(timeStamp+ str + "\n");
            writer.flush();
        } catch (IOException ex) {
            LOG.error("Experience exception when logging", ex);
        }
    }

    public static void prodLogInfo(String str) {
        if(DISABLE_PRODINFO)
            return;

        getInstance().prodlogInfoInternal(str);

    }

    public void testwopListLog(String str)
    {
        if(testwopListwriter==null)
            init();

        try {
            testwopListwriter.write(str + "\n");
            testwopListwriter.flush();
        } catch (IOException ex) {
            LOG.error("Experience exception when logging", ex);
        }
    }

    public void testwassertListLog(String str)
    {
        if(testwassertListwriter==null)
            init();

        try {
            testwassertListwriter.write(str + "\n");
            testwassertListwriter.flush();
        } catch (IOException ex) {
            LOG.error("Experience exception when logging", ex);
        }
    }

    public void testwassertsucListLog(String str)
    {
        if(testwassertsucListwriter==null)
            init();

        try {
            testwassertsucListwriter.write(str + "\n");
            testwassertsucListwriter.flush();
        } catch (IOException ex) {
            LOG.error("Experience exception when logging", ex);
        }
    }

    public void setLastTestName(String name) {
        lastTestName = name;
        try {
            logFileName = LOGFILEPREFIX + lastTestName;
            logwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFileName)));
        }catch (IOException ex)
        {
            LOG.error("Experience exception when logging", ex);
        }
    }

    public interface MiniLogger
    {
        void logInfo(String str);
    }

}
