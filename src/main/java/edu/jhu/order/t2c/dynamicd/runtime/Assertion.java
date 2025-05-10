package edu.jhu.order.t2c.dynamicd.runtime;

import com.google.gson.annotations.Expose;
import edu.jhu.order.t2c.dynamicd.util.ForcedException;
import soot.jimple.spark.ondemand.genericutil.Stack;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Assertion {

    public int getAssertionLocation() {
        return assertionLocation;
    }

    public enum AssertType {
        EQUALS("EQUALS"),
        NOTEQUALS("NOTEQUALS"),
        TRUE("TRUE"),
        FALSE("FALSE"),
        NULL("NULL"),
        SAME("SAME"),
        NOTSAME("NOTSAME"),
        TESTBODYBEGIN("TESTBODYBEGIN"),
        TESTBODYEND("TESTBODYEND"),
        CUSTOMIZED("CUSTOMIZED"),
        FAIL("FAIL"),

        ILLEGAL("ILLEGAL");

        private final String text;

        /**
         * @param text
         */
        AssertType(final String text) {
            this.text = text;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
        }
    }

    public enum AssertStatus {
        MISSING_OP("MISSING_OP"),
        MISSING_EXPECTED("MISSING_EXPECTED"),
        INCOMPLETE_SYMMAP("INCOMPLETE_SYMMAP"),
        NORMAL("NORMAL");

        private final String text;

        /**
         * @param text
         */
        AssertStatus(final String text) {
            this.text = text;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
        }

    }
    
    public enum AssertState {
        PASS,
        INACTIVE,
        FAIL,
        SKIP,
        ILLEGAL;
    }

    public enum ExpectedType {
        String("String"),
        Integer("Integer"),
        Null("Null"),
        Matrix("Matrix"),

        ILLEGAL("ILLEGAL");

        private final String text;

        /**
         * @param text
         */
        ExpectedType(final String text) {
            this.text = text;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
        }
    }

    public class Expected {

        Map<String, Symbol> symbolMap;
        Object val;
        ExpectedType type;

        Expected(Object obj) {
            Symbol.SymbolBuilder builder = new Symbol.SymbolBuilder(CheckerTemplate.symId, CheckerTemplate.cache);
            this.type = ExpectedType.ILLEGAL;

            if (obj instanceof Integer) {
                String symbol = builder.getSymbol(obj.toString());
                this.val = symbol;
                this.type = ExpectedType.Integer;
            } else if (obj instanceof Object[][]) {
                String[][] symbolMatrix = builder.convert2DArray(obj);
                this.val = symbolMatrix;
                this.type = ExpectedType.Matrix;
            } else if (obj==null)
            {
                this.val = null;
                this.type = ExpectedType.Null;
            }
            this.symbolMap = builder.getSymbolMap();
        }

        void removeAlias(String oldSym, String newSym) {
            if (val instanceof String) {
                if (Symbol.isSymbol((String) val)) {
                    if(val.equals(oldSym))
                        val = newSym;
                }
            } else if (val instanceof String[][]) {
                String[][] matrix = (String[][])val;
                for(int i=0;i<matrix.length;++i)
                    for(int j=0;j<matrix[0].length;++j)
                    {
                        String sym = matrix[i][j];
                        if(Symbol.isSymbol(sym))
                        {
                            if(sym.equals(oldSym))
                                matrix[i][j] = newSym;
                        }
                    }

            }

            Operation.removeAlias(symbolMap, oldSym, newSym);
        }

        void substitute(Map<String, Symbol> symbolMap)
        {
            this.symbolMap = symbolMap;
        }

    }

    public static String CHECKER_THREAD_NAME = "T2CChecker";

    AssertType assertType;
    int assertionLocation = 0;


    //each assert should has two parts:
    //e.g. assertRowCount(execute("SELECT v FROM %s WHERE k = 0"), 10);
    //  "SELECT v FROM %s WHERE k = 0": the query string to exec
    //  10: the result to expect
    Operation assertQueryOp;

    //Expected expected;

    AssertStatus status;
    AssertState state = AssertState.INACTIVE;

    //a checker consisting of many operations could have assertions in the middle
    //e.g.
    // op1 -> op2 -> assert1 (location=2) -> op3 -> assert2 (location=3)
    //cannot simply put a location, because we will later do truncate, instead we record last op
    @Expose(serialize = false, deserialize = false)
    public int lastTriggerOpHash = -1;

    public Assertion(AssertType assertType, Object val) {
        this.assertType = assertType;
        //we cannot decide at this time
        //this.expected = new Expected(val);
        this.status = AssertStatus.NORMAL;
    }

    public Assertion(AssertType assertType, boolean v) {
        this(assertType, new Boolean(v));
    }

    public Assertion(AssertType assertType, int v) {
        this(assertType, new Integer(v));
    }

    public Assertion(AssertType assertType, double v) {
        this(assertType, new Double(v));
    }

    public Assertion(AssertType assertType, float v) {
        this(assertType, new Float(v));
    }

    public Assertion(AssertType assertType, long v) {
        this(assertType, new Long(v));
    }

    public Assertion(AssertType assertType, short v) {
        this(assertType, new Short(v));
    }

    public Assertion(AssertType assertType, char v) {
        this(assertType, new Character(v));
    }

    public Operation getAssertQueryOp() {
        return assertQueryOp;
    }

    public void addAssertQuery(Operation op) {
        assertQueryOp = op;
    }

    public void substitute(Map<String, Symbol> symbolMap) {
        assertQueryOp.substitute(symbolMap);
        //expected.substitute(symbolMap);
    }

    public static boolean appendAssert(Assertion assertion)
    {
        switch (GlobalState.mode)
        {
            case BUILD:
            case UNITTEST:
                TemplateManager.getInstance().addAssert(assertion);
                return true;
            case ILLEGAL:
                throw new RuntimeException("mode not set!");
        }
        return false;
    }

    public static void appendCustomizedAssert(String testClassName, int id, Object... args)
    {
        Assertion assertion = new Assertion(Assertion.AssertType.CUSTOMIZED,null);
        if(appendAssert(assertion))
            //only generate op when needed
            assertion.addAssertQuery(Operation.createCustomizedOperation(testClassName, id, args));
        //System.out.println("#### AppendCustomizedAssert ended");
    }

    //for debugging use
    public static void print(String message)
    {
        //will be replaced with generated content anyway
        if(true)
            throw new RuntimeException("Impossible");
    }

    public class CheckTask implements Runnable {
        final CheckerTemplate template;

        public CheckTask(CheckerTemplate template) {
            this.template = template;
        }

        public void run() {
//            T2CHelper.prodLogInfo("Running check for "+template.templateSource);

            //synchronized(template)
            {
                if (state.equals(AssertState.INACTIVE))
                    state = AssertState.ILLEGAL;
                GlobalState.ifAsserting = true;

                try {
                    try {
//                        T2CHelper.prodLogInfo("Start to assert for " + template.templateSource + "-"
//                                + assertQueryOp.customizedOpID);
                        // T2CHelper.prodLogInfo("Test1mod");

                        if (state.equals(AssertState.SKIP)) {
                            throw new ForcedException("Marked as skip");
                        }

                        // delay a bit
                        Thread.sleep(20);

                        assertQueryOp.execute();

                        // T2CHelper.prodLogInfo("Assert passed from "+template.templateSource);
                        state = AssertState.PASS;
                        // System.out.println("DIMAS: Success " + assertQueryOp.customizedOpID + " " +
                        // state.toString());
                        // T2CHelper.prodLogInfo("DIMAS: Success " + assertQueryOp.customizedOpID + " "
                        // + state.toString());
                        RuntimeTracer.getInstance().success.incrementAndGet();
                        RuntimeTracer.getInstance().successMap.compute(
                                template.templateSource + "-" + assertQueryOp.customizedOpID,
                                (k, v) -> (v == null) ? 1 : v + 1);
                    } catch (Throwable ex) {
                        if (ex instanceof ForcedException) {
                            // if (!state.equals(AssertState.PASS))
                                state = AssertState.SKIP;
                            RuntimeTracer.getInstance().skip.incrementAndGet();
                            RuntimeTracer.getInstance().skipMap.compute(template.templateSource + "-"
                                    + assertQueryOp.customizedOpID,
                                    (k, v) -> (v == null) ? 1 : v + 1);
                        } else if (ex instanceof java.lang.VerifyError) {
//                            String msg = "Assert 1 " + template.templateSource + " failed with exception:"
//                                    + ex.getClass().getName() + "\n";
//                            if (ex.getMessage() != null) {
//                                msg += ex.getMessage() + "\n";
//                            }
//                            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
//                                msg += ex.getCause().getMessage();
//                            }
//                            T2CHelper.prodLogInfo(msg);
                            // if (!state.equals(AssertState.PASS))
                                state = AssertState.SKIP;
                            RuntimeTracer.getInstance().skip.incrementAndGet();
                            RuntimeTracer.getInstance().skipMap.compute(template.templateSource + "-"
                                    + assertQueryOp.customizedOpID + "-verifyerror",
                                    (k, v) -> (v == null) ? 1 : v + 1);
                        } else if (ex.getCause() != null && ex.getCause().getMessage() != null
                                && ex.getCause().getMessage().startsWith(
                                        "waiting for server")) {
                            // if (!state.equals(AssertState.PASS))
                                state = AssertState.SKIP;
                            RuntimeTracer.getInstance().skip.incrementAndGet();
                            RuntimeTracer.getInstance().skipMap.compute(template.templateSource + "-"
                                    + assertQueryOp.customizedOpID + "-waiting",
                                    (k, v) -> (v == null) ? 1 : v + 1);
                        } else if (ex.getCause() instanceof ClassNotFoundException
                                || ex.getCause() instanceof NoClassDefFoundError || ex.getCause() instanceof VerifyError
                                || ex.getCause() instanceof RuntimeException) {
//                            String msg = "Assert 1 " + template.templateSource + " failed with exception:"
//                                    + ex.getClass().getName() + "\n";
//                            if (ex.getMessage() != null) {
//                                msg += ex.getMessage() + "\n";
//                            }
//                            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
//                                msg += ex.getCause().getMessage();
//                            }
//                            T2CHelper.prodLogInfo(msg);
                            // if (!state.equals(AssertState.PASS))
                                state = AssertState.SKIP;
                            RuntimeTracer.getInstance().skip.incrementAndGet();
                            RuntimeTracer.getInstance().skipMap.compute(template.templateSource + "-"
                                    + assertQueryOp.customizedOpID,
                                    (k, v) -> (v == null) ? 1 : v + 1);
                        } else {
                            String msg = "Assert 1 " + template.templateSource + " failed with exception:"
                                    + ex.getClass().getName() + "\n";
                            if (ex.getMessage() != null) {
                                msg += ex.getMessage() + "\n";
                            }
                            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                                msg += ex.getCause().getMessage();
                            }
                            T2CHelper.prodLogInfo(msg);
                            
                            if (!state.equals(AssertState.PASS))
                                state = AssertState.FAIL;

                            RuntimeTracer.getInstance().fail.incrementAndGet();
                            RuntimeTracer.getInstance().failMap.compute(template.templateSource + "-"
                                    + assertQueryOp.customizedOpID,
                                    (k, v) -> (v == null) ? 1 : v + 1);
                        }

                    }
                } catch (Throwable ex) {
                    if (!state.equals(AssertState.PASS))
                        state = AssertState.FAIL;

                    String msg = "Assert 2 " + template.templateSource + " failed with exception:"+ ex.getClass().getName() + "\n";
                    if (ex.getMessage() != null) {
                        msg += ex.getMessage() + "\n";
                    }
                    if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                        msg += ex.getCause().getMessage();
                    }
                    T2CHelper.prodLogInfo(msg);

                    RuntimeTracer.getInstance().fail.incrementAndGet();
                    RuntimeTracer.getInstance().failMap.compute(template.templateSource + "-"
                            + assertQueryOp.customizedOpID,
                            (k, v) -> (v == null) ? 1 : v + 1);
                    System.out.println("CRITICAL ERROR: ");
                    ex.printStackTrace();

                }
                if (state.equals(AssertState.ILLEGAL)) {
                    System.out.println("CRITICAL ERROR: " + template.templateSource + " state illegal");
                }
                GlobalState.ifAsserting = false;
            }
            //skip for now
            //throw new RuntimeException("Assert type not supported!");
            // System.out.println("Success: "+RuntimeTracer.getInstance().success.get()+" Fail: "+RuntimeTracer.getInstance().fail.get()+" Skip: "+RuntimeTracer.getInstance().skip.get());
            // T2CHelper.prodLogInfo("Success: "+RuntimeTracer.getInstance().success.get()+" Fail: "+RuntimeTracer.getInstance().fail.get()+" Skip: "+RuntimeTracer.getInstance().skip.get());
           if(RuntimeTracer.getInstance().execCountdown.decrementAndGet()<=0){
               T2CHelper.prodLogInfo("Success: " + RuntimeTracer.getInstance().success.get() + " Fail: "
                       + RuntimeTracer.getInstance().fail.get()
                       + "\n" + "SuccessMap: " + RuntimeTracer.getInstance().successMap + "\n" + "FailMap: "
                       + RuntimeTracer.getInstance().failMap);
               RuntimeTracer.getInstance().execCountdown.set(0);
           }
            
            long now = System.currentTimeMillis();
            if(RuntimeTracer.getInstance().rateRefresher.containsKey(assertQueryOp.customizedOpID)){
                if((now-RuntimeTracer.getInstance().rateRefresher.get(assertQueryOp.customizedOpID)) > RuntimeTracer.getInstance().rateLimitRefresh &&
                   RuntimeTracer.getInstance().rateLimiter.get(assertQueryOp.customizedOpID).get()<RuntimeTracer.getInstance().rateLimitSize){
                    RuntimeTracer.getInstance().rateLimiter.get(assertQueryOp.customizedOpID).incrementAndGet();
                }
            }
            RuntimeTracer.getInstance().rateRefresher.put(assertQueryOp.customizedOpID, now);
        }
    }

    public void check(CheckerTemplate template) {
        //TODO: Singlenode mode
        //T2CHelper.prodLogInfo("Initializing check for "+template.templateSource);
        CheckTask task = new CheckTask(template);
        if(GlobalState.mode.equals(GlobalState.T2CMode.PRODUCTION) || !RuntimeTracer.getInstance().executedAssertionID.contains(assertQueryOp.customizedOpID))
        {
            RuntimeTracer.getInstance().executedAssertionID.add(assertQueryOp.customizedOpID);
            RuntimeTracer.getInstance().futures.add(RuntimeTracer.getInstance().executor.submit(task));
        }
        //Thread t = new Thread(task);
        //t.setName(CHECKER_THREAD_NAME);
        //t.start();
        //See https://github.com/OrderLab/zookeeper/commit/0d721b651f300101e9d317c626f4cad597a3b0e5
        //Proxy mode
        /*
        for(String hostport:Proxy.getQuorumList())
        {
            CheckTask task = new CheckTask(hostport);
            Thread t = new Thread(task);
            t.start();

        }
        */
    }
}
