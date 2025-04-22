package edu.jhu.order.t2c.staticd.transformer;

import edu.jhu.order.t2c.dynamicd.runtime.Assertion;
import edu.jhu.order.t2c.dynamicd.runtime.ConfigManager;
import edu.jhu.order.t2c.dynamicd.runtime.T2CHelper;
import edu.jhu.order.t2c.dynamicd.tscheduler.TestClassPool;
import edu.jhu.order.t2c.staticd.algorithm.MethodPurityAnalysis;
import edu.jhu.order.t2c.staticd.algorithm.ParamAnalysis;
import edu.jhu.order.t2c.staticd.analysis.PhaseInfo;
import edu.jhu.order.t2c.staticd.util.SootUtils;
import edu.jhu.order.t2c.staticd.util.T2CConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.javaToJimple.DefaultLocalGenerator;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.annotation.purity.PurityAnalysis;
import soot.tagkit.AnnotationTag;
import soot.tagkit.CodeAttribute;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.validation.ValidationException;

import java.util.*;

public class TestAssertionPacker extends SceneTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(TestAssertionPacker.class);

    static final String ASSERT_FUNC_PREFIX = "externalAssertFunc";

    //set need_call_graph as true to enable purity analysis
    public static final PhaseInfo PHASE_INFO = new PhaseInfo("wjtp", "testassertionpacker",
            "transform the test case and pack the assertion function", true, true);

    static int funcCounter = 0;
    static boolean INSERT_PRINTF_DEBUGGING = false; //force to insert a printf before each inserted stmt

    static private List<String> disallowList = new ArrayList<>();
    static
    {
//        disallowList.add(...)
    }

    //processing status of transformation
    enum TFResult {
        SUCCESS,
        SKIP,
        NO_ASSERT,
        SIMPLE,
        CONTROL_FLOW_ASSERT,
        MULTI_ASSERT,
        FAULT;
    }

    public static class AssertSuite
    {
        public boolean ifAssertFail=false;
        public Stmt oldAssertStmt;
        public List<Value> argsOfAssert;

        //where the assertion belongs to
        public Body body;
    }

    protected void internalTransform(String phaseName, Map<String, String> options) {
        if(ConfigManager.config.getBoolean(ConfigManager.IF_DO_PURITY_ANALYSIS_KEY))
            MethodPurityAnalysis.doAnalysis();

        if(T2CConfig.getInstance().test_name.equals("all") &&
                T2CConfig.getInstance().test_method.equals("all"))
            //for all
            internalTransformAll();
        else
            internalTransformSpecificClasses(
                new HashSet<String>() {{
                    add(T2CConfig.getInstance().test_name);
                }},
                new HashSet<String>() {{
                    add(T2CConfig.getInstance().test_method);
                }});
    }

    private void internalTransformAll() {
        System.out.println("internalTransformAll");

        //tried using reflections8 to analyze, but this cause problems of loading classes, should not do
        //ConfigManager.initConfig();
        //TestClassPool.registerAllClass();
        //internalTransformSpecificClasses(TestClassPool.getClasses(), new HashSet<>());
        Set<String> set = new HashSet<>();
        for(SootClass c: Scene.v().getApplicationClasses())
        {
            set.add(c.getName());
        }
        //empty method set means no need to filter
        internalTransformSpecificClasses(set, new HashSet<>());

    }

    private void internalTransformSpecificClasses(Collection<String> classes,
            Collection<String> selectedMethods) {

        int succCount = 0, faultCount = 0, noassertCount = 0, simpleCount =0,
                multiCount = 0, controlFlowCount = 0, totalCount = 0;

        int failedDumpClassNum = 0;
        int totalClassCount = 0;

        List<String> succMethodList = new ArrayList<>();
        List<String> nestedClasses = new ArrayList<>();
        List<String> succDump = new ArrayList<>();
        for (String cName : classes) {
            boolean ifAnyProcessed = false;
            boolean ifAnySucc = false;

            SootClass c = Scene.v().loadClassAndSupport(cName);
            SootUtils.dumpSootClassJimple(c,"old");
            List<SootMethod> methods = c.getMethods();
            //we may add new methods during analysis, so clone one list
            List<SootMethod> clonedMethods = new ArrayList<>(methods);
            for (SootMethod method : clonedMethods) {
                if (!method.hasActiveBody()) {
                    continue;
                }

                if (selectedMethods.size() > 0 && !selectedMethods.contains(method.getName())) {
                    continue;
                }

                if(disallowList.contains(cName+"#"+method.getName()))
                {
                    LOG.warn("Skip handling "+cName+"#"+method.getName()+" due to disallowlist");
                    continue;
                }

                boolean ifTestMethod = false;
                VisibilityAnnotationTag tag = (VisibilityAnnotationTag) method.getTag("VisibilityAnnotationTag");
                if (tag != null) {
                    for (AnnotationTag annotation : tag.getAnnotations()) {
                        if (annotation.getType().contains("Test")) {
                            ifTestMethod = true;
                            break;
                        }
                    }
                }
                if(!ifTestMethod) continue;

                ifAnyProcessed= true;

                TFResult result = internalTransform(method.getActiveBody(), false);
                switch (result)
                {
                    case SKIP:
                        break;
                    case SUCCESS:
                        succCount++;
                        ifAnySucc = true;
                        totalCount++;
                        succMethodList.add(cName+"#"+method.getName());
                        T2CHelper.getInstance().testwassertListLog(cName+"#"+method.getName());
                        T2CHelper.getInstance().testwassertsucListLog(cName+"#"+method.getName());
                        break;
                    case NO_ASSERT:
                        noassertCount++;
                        totalCount++;
                        break;
                    case FAULT:
                        faultCount++;
                        totalCount++;
                        T2CHelper.getInstance().testwassertListLog(cName+"#"+method.getName());
                        break;
                    case SIMPLE:
                        simpleCount++;
                        totalCount++;
                        break;
                    case MULTI_ASSERT:
                        multiCount++;
                        totalCount++;
                        T2CHelper.getInstance().testwassertListLog(cName+"#"+method.getName());
                        T2CHelper.getInstance().testwassertsucListLog(cName+"#"+method.getName());
                        break;
                    case CONTROL_FLOW_ASSERT:
                        controlFlowCount++;
                        totalCount++;
                        T2CHelper.getInstance().testwassertListLog(cName+"#"+method.getName());
                        T2CHelper.getInstance().testwassertsucListLog(cName+"#"+method.getName());
                        break;
                    default:
                        totalCount++;
                }
            }

            if(ifAnyProcessed)
                totalClassCount++;

            try {
                if (c.hasOuterClass() && !ifAnySucc){
                    nestedClasses.add(cName);
                }

                if(ifAnySucc)
                {
                    SootUtils.dumpSootClassJimple(c,"new");
                    boolean dumpStatus = SootUtils.dumpSootClass(c);
                    if(!dumpStatus){
                        failedDumpClassNum++;
                    } else {
                        succDump.add(cName);
                    }
                }
            }catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

        for (String cName: nestedClasses){
            SootClass c = Scene.v().loadClassAndSupport(cName);
            if (succDump.contains(c.getOuterClass().getName())){
                if (!SootUtils.dumpSootClass(c)) {
                    failedDumpClassNum++;
                }
            }
        }

        LOG.debug("Successful transformed methods:");
        for(String m: succMethodList)
        {
            LOG.debug(m);
        }

        LOG.info("[Transformation Summary]");
        LOG.info("Process "+totalClassCount+" classes");
        LOG.info("Transform "+succCount+"/"+totalCount+" methods successfully");
        LOG.info("SIMPLE: "+simpleCount);
        LOG.info("CONTROL_FLOW: "+controlFlowCount+", MULTI_ASSERT: "+multiCount);
        LOG.info("NO_ASSERT: "+noassertCount+", FAULT: "+faultCount);

        if(failedDumpClassNum!=0)
            LOG.warn("Failed to dump "+failedDumpClassNum+" classes! This may reduce the overall available class numbers");
    }

    /**
     * do the real transformation here for one test method
     *
     * @param body the test method body
     * @param ifSkipInsertRegisterHooks for testing use, set true when under testing
     */
    private TFResult internalTransform(Body body, boolean ifSkipInsertRegisterHooks) {
        //skip non-test
        if (!SootUtils.hasTestAnnotation(body.getMethod())) {
            //dump method body
            SootUtils.dumpBodyJimple(body, "skipped");
            return TFResult.SKIP;
        }

        //for debugging use
        //SootUtils.printBodyJimple(body);
        SootUtils.dumpBodyJimple(body, "old");

        try {
            List<AssertSuite> suites = getAssertSuites(body);
            if(suites.isEmpty())
            {
                LOG.info("did not find assertion in "+body.getMethod().getDeclaringClass().getName()+"#"+body.getMethod().getName());
                return TFResult.NO_ASSERT;
            }

            for(AssertSuite suite:suites)
            {
                //filter multiple and assert.fail type here
                //if(suites.size()>=2)
                //    return TFResult.MULTI_ASSERT;

                if(suite.ifAssertFail)
                    return TFResult.CONTROL_FLOW_ASSERT;

                boolean ifSimple = handleSingleAssert(suite.body, ifSkipInsertRegisterHooks, suite);
                if(ifSimple)
                {
                    LOG.info("Skip to wrap assertion as it is a simple assertion.");
                    return TFResult.SIMPLE;
                }
            }
        } catch (Exception ex) {
            LOG.error("Skip to generate for method " + body.getMethod().getName()
                    + " due to exception", ex);
            return TFResult.FAULT;
        }

        return TFResult.SUCCESS;
    }

    //return if the assertion is simple
    boolean handleSingleAssert(Body body, boolean ifSkipInsertRegisterHooks, AssertSuite assertSuite)
    {
        //testing purpose
        boolean IF_MODIFY = true;

        LocalGenerator lg = new DefaultLocalGenerator(body);
        ParamAnalysis paramAnalysis = new ParamAnalysis(body, assertSuite.argsOfAssert, assertSuite.oldAssertStmt, lg);

        //add register for variables generated for locals
        if(IF_MODIFY)
        {
            Stmt insertPoint = SootUtils.findFirstNonIdentityStmt(body);
            for (Stmt stmt : paramAnalysis.newLocalAssignStmts) {
                LOG.info("newLocalAssignStmts "+stmt);
                body.getUnits().insertBefore(stmt, insertPoint);
            }
        }

        //!!!!!skip now!
        //check if the assert can be just using single operation response check to replace
        if(false)
        {
            boolean ifSimple = false;
            for(Stmt stmt: paramAnalysis.tobeCopiedStmts)
            {
                if(stmt.containsInvokeExpr())
                {
                    LOG.info("scanning invokeExpr "+stmt.getInvokeExpr().getMethod().getDeclaringClass().getName());
                    if(stmt.getInvokeExpr().getMethod().getDeclaringClass().getName().equals("org.apache.zookeeper.ZooKeeper") ||
                            stmt.getInvokeExpr().getMethod().getDeclaringClass().getName()
                                    .equals("edu.jhu.order.t2c.staticd.cases.AssertionSample$SimpleAssertionTester"))
                    {
                        ifSimple = true;
                        break;
                    }
                }
            }
            if(ifSimple)
                return ifSimple;
        }


        //for debugging use
        LOG.info("baseArgs:");
        for (Value value : paramAnalysis.baseArgs) {
            LOG.info(value.toString());
        }

        //add wrappers for primtype locals
        List<Value> wrappedBaseArgs = new ArrayList<>();
        //these are for init primitive vars at the beginning, otherwise we may risk using them at the wrapper init
        //without initing these primtitive first, e.g. int x; Integer y = new Integer(x);
        //other wise you would see such JNI errors:
        //Error: A JNI error has occurred, please check your installation and try again
        //Exception in thread "main" java.lang.VerifyError: (class: edu/jhu/order/t2c/cases/AssertionSample, method: testForLoopAdvanced2 signature: ()V) Register 1 contains wrong type
        List<Stmt> wrappedInitStmts = new ArrayList<>();
        //assign primitives to init wrapper vars
        List<Stmt> wrappedAssignStmts = new ArrayList<>();
        genWrapperInfo(wrappedInitStmts, wrappedAssignStmts, wrappedBaseArgs, paramAnalysis,
                lg, body);
        for(Stmt stmt: wrappedInitStmts)
            LOG.info("add wrappedInitStmt "+stmt);
        for(Stmt stmt: wrappedAssignStmts)
            LOG.info("add wrappedAssignStmt "+stmt);

        //SootClass assertPoolClass = generateAssertClass();
        SootClass assertPoolClass = null;
        SootMethod assertMethod = null;
        if(IF_MODIFY)
        {
            assertPoolClass = body.getMethod().getDeclaringClass();
            assertMethod = generateAssertMethod(body, assertPoolClass, paramAnalysis, lg,
                    wrappedBaseArgs, ifSkipInsertRegisterHooks);

        }

        //this should be done before modifying original test case codes, otherwise goto targets would be messed up
        //update: disable now as we are using old class
        //SootUtils.SecureExportSootClass(assertPoolClass);

        if(IF_MODIFY) {
            modifyTestBody(body, wrappedInitStmts, wrappedAssignStmts, assertSuite.oldAssertStmt);
            //replace the assert with generated func
            Stmt newAssertStmt = generateAssertStmt(assertMethod, wrappedBaseArgs);
            body.getUnits().swapWith(assertSuite.oldAssertStmt, newAssertStmt);
            for (Stmt stmt : paramAnalysis.tobeCopiedStmts) {
                //body.getUnits().remove(stmt);
            }

            //for debugging use
            //SootUtils.printBodyJimple(body);
            SootUtils.dumpBodyJimple(body, "new");
            validate(body);
            body.validate();
        }

        return false;
    }

    //for debugging, copy of soot.Body.validate
    public void validate(Body body) {
        Iterator var3 = getAllUnitBoxes(body).iterator();

        UnitBox ub;
        do {
            if (!var3.hasNext()) {
                return;
            }

            ub = (UnitBox)var3.next();
        } while(body.getUnits().contains(ub.getUnit()));
        if(!body.getUnits().contains(ub.getUnit()))
        {
            //LOG.info("KKKKKKKKKKKKKKKKKKKK");
            //for(Unit unit:body.getUnits())
            //LOG.info("KKKKKKKKKKKKKKKKKKKK"+unit.toString()+System.identityHashCode(unit));
            //LOG.info("KKKKKKKKKKKKKKKKKKKK"+ub.getUnit().toString()+System.identityHashCode(ub.getUnit()));
        }

        throw new RuntimeException("Unitbox points outside unitChain! to unit : " + ub.getUnit() +System.identityHashCode(ub.getUnit()) + " in " + body.getMethod());
    }

    public List<UnitBox> getAllUnitBoxes(Body body) {
        ArrayList<UnitBox> unitBoxList = new ArrayList();
        Iterator it = body.getUnits().iterator();

        while(it.hasNext()) {
            Unit item = (Unit)it.next();
            unitBoxList.addAll(item.getUnitBoxes());
        }

        /*
        it = body.getTraps().iterator();

        while(it.hasNext()) {
            Trap item = (Trap)it.next();
            unitBoxList.addAll(item.getUnitBoxes());
        }

        it = body.getTags().iterator();

        while(it.hasNext()) {
            Tag t = (Tag)it.next();
            if (t instanceof CodeAttribute) {
                unitBoxList.addAll(((CodeAttribute)t).getUnitBoxes());
            }
        }
*/
        return unitBoxList;
    }

    static class UnitPair
    {
        Unit unit;
        Body body;

        public UnitPair(Unit unit, Body body) {
            this.unit = unit;
            this.body = body;
        }
    }
    public static List<AssertSuite> getAssertSuites(Body body) {

        List<AssertSuite> suites = new ArrayList<>();

        //avoid dead loop, e.g. in Zookeeper testSingleSerialize
        Set<String> processedSet = new HashSet<>();
        List<UnitPair> toprocessList = new ArrayList<>();
        for(Unit unit: body.getUnits())
            toprocessList.add(new UnitPair(unit,body));

        boolean isSkipped = false;
        while(!toprocessList.isEmpty())
        {
            UnitPair stmtPair = toprocessList.remove(0);
            Stmt stmt = (Stmt) stmtPair.unit;
            if (stmt.containsInvokeExpr()) {
                String className = stmt.getInvokeExpr().getMethod().getDeclaringClass().getName();
                if (className.equals("org.junit.Assert") ||
                        className.equals("junit.framework.Assert")
                                || stmt.getInvokeExpr().getMethod().getName().contains("verify") //zk
                        //|| stmt.getInvokeExpr().getMethod().getName().contains("assertRows")  //cassandra
                        || stmt.getInvokeExpr().getMethod().getName().contains("assert")  //test
                ) {
                    //<delete>right now we only focus on one assertion</delete>
                    //only generate for last assertion
                    //if (retStmt != null)
                    //    throw new RuntimeException("Multiple assertions found.");

                    //for now we only return the first twos
                    //TODO: support multiple asserts
                    if(suites.size()>=5)
                    {
                        //LOG.warn("multiple assertion detected, abort");
                        //the original warning message is wrong, we still include them not skipping them
                        LOG.info("multiple assertion detected, only include two for now");
                        break;
                    }

                    //we leave assert.fail in dynamic phase
                    //WARN: disable control-flow assert for now
                    if(false)
                    //if(stmt.getInvokeExpr().getMethod().getName().equals("fail"))
                    {

                        LOG.warn("skip assert.fail for stmt: "+stmt.toString());
                        AssertSuite suite = new AssertSuite();
                        suite.ifAssertFail = true;
                        suite.body = stmtPair.body;
                        suites.add(suite);
                        break;
                    }

                    //start from args of assert, then trace basic args
                    List<Value> argsOfAssert = new ArrayList<>();
                    if(stmt instanceof AssignStmt)
                    {
                        argsOfAssert.add(((AssignStmt) stmt).getLeftOp());
                    }
                    if(stmt.getInvokeExpr() instanceof VirtualInvokeExpr)
                    {
                        Value base = ((VirtualInvokeExpr) stmt.getInvokeExpr()).getBase();
                        if (base instanceof JimpleLocal) {
                            argsOfAssert.add(base);
                        }
                    }
                    for (int i=0;i< stmt.getInvokeExpr().getArgs().size();++i) {
                        Value arg  = stmt.getInvokeExpr().getArgs().get(i);
                        if(arg.toString().contains("waiting for server") || arg.toString().contains("Waiting for server")){
                            isSkipped = true;
                        }
                        if (arg instanceof JimpleLocal) {
                            argsOfAssert.add(arg);
                        }
                        /*
                        if (arg instanceof Constant) {
                            LocalGenerator lg = new DefaultLocalGenerator(stmtPair.body);
                            Local local = lg.generateLocal(arg.getType());
                            stmtPair.body.getUnits().insertBefore(Jimple.v().newAssignStmt(local,arg),stmt);
                            stmt.getInvokeExpr().setArg(i,local);
                            argsOfAssert.add(local);
                        }*/
                    }

                    if(isSkipped){
                        isSkipped = false;
                        continue;
                    }

                    AssertSuite suite = new AssertSuite();
                    suite.argsOfAssert = argsOfAssert;
                    suite.oldAssertStmt = stmt;
                    suite.body = stmtPair.body;
                    suites.add(suite);
                }
                else if(className.equals(body.getMethod().getDeclaringClass().getName())){
                    //handle inner function call in the same class
                    SootMethod tMethod = stmt.getInvokeExpr().getMethod();
                    if(!processedSet.contains(tMethod.getSignature()))
                        for(Unit unit: (tMethod.retrieveActiveBody().getUnits()))
                        {
                            if(unit instanceof InvokeStmt)
                            {
                                processedSet.add(tMethod.getSignature());
                                if(tMethod.getName().startsWith(ASSERT_FUNC_PREFIX))
                                    continue;
                                toprocessList.add(new UnitPair(unit,tMethod.retrieveActiveBody()));
                            }
                        }
                }
            }
        }
        return suites;
    }


    //we used to generate a separate class to save all assert methods,
    //however this would often cause dependency issues, which breaks execution
    //in both testing phase and prod phase, now we just save such methods back
    @Deprecated
    static SootClass generateAssertClass() {
        String assertPoolClassName = "t2c.AssertPool_" + funcCounter;
        SootClass assertPoolClass = new SootClass(
                assertPoolClassName,
                Modifier.PUBLIC);
        assertPoolClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));

        SootMethod clinit = new SootMethod("<clinit>",
                Collections.<Type>emptyList(),
                VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        JimpleBody clinitBody = Jimple.v().newBody(clinit);
        clinitBody.getUnits().add(Jimple.v().newReturnVoidStmt());
        clinit.setActiveBody(clinitBody);
        assertPoolClass.addMethod(clinit);

        Scene.v().addClass(assertPoolClass);
        return assertPoolClass;
    }

    static SootMethod generateAssertMethod(Body testBody, SootClass assertPoolClass,
            ParamAnalysis paramAnalysis,
            LocalGenerator lg, List<Value> wrappedBaseArgs, boolean ifSkipInsertRegisterHooks) {
        int funcId = funcCounter++;
        String assertMethodName = ASSERT_FUNC_PREFIX + (funcId);

        Set<Value> baseArgs = paramAnalysis.baseArgs;
        Set<Value> intermediateLocals = paramAnalysis.intermediateLocals;
        List<Stmt> tobeCopiedStmts = paramAnalysis.tobeCopiedStmts;

        LOG.info("tobeCopiedStmts:");
        for(Stmt stmt: tobeCopiedStmts)
        {
            LOG.info("\t"+stmt.toString());
        }

        List<Type> types = new ArrayList<Type>();
        for (Value val : baseArgs) {
            types.add(SootUtils.boxBasicJavaType(val.getType()));
        }

        SootMethod assertMethod = new SootMethod(
                assertMethodName, types, VoidType.v(),
                Modifier.PUBLIC | Modifier.STATIC);
        assertMethod.addException(Scene.v().getSootClass("java.lang.Exception"));

        JimpleBody body = Jimple.v().newBody(assertMethod);

        //init param ref
        int count = 0;
        for (Value val : wrappedBaseArgs) {
            Stmt stmt = Jimple.v()
                    .newIdentityStmt(val, Jimple.v().newParameterRef(SootUtils.boxBasicJavaType(val.getType()), count));
            body.getUnits().add(stmt);
            count++;
        }

        for (Value val : intermediateLocals) {
            if (val instanceof Local) {
                if(val.getType().equals(NullType.v()))
                    ((Local)val).setType(RefType.v("java.lang.Object"));
                body.getLocals().add((Local) val);
            }
            //we leave constants at next step to handle
        }

        //add locals
        for (Value value : wrappedBaseArgs) {
            if (!body.getLocals().contains(value)) {
                if(value.getType().equals(NullType.v()))
                    ((Local)value).setType(RefType.v("java.lang.Object"));
                body.getLocals().add((Local) value);
            }
        }

        //init the locals, otherwise cause VerifyError: Register X contains wrong type
//        for(Local local:body.getLocals())
//        {
//            if(local.getType().equals(RefType.v("java.lang.Integer")))
//            {
//                RefType t= (RefType)local.getType();
//                //body.getUnits().add(Jimple.v().newAssignStmt(local, SootUtils.getConstantForPrim(local.getType())));
//                body.getUnits().add(Jimple.v()
//                        .newAssignStmt(local, Jimple.v().newNewExpr(t)));
//                body.getUnits()
//                        .add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(local,
//                                t.getSootClass().getMethod(
//                                        SootUtils.getInitPrimMethodSignature(t))
//                                        .makeRef(), SootUtils.getConstantForPrim(t))));
//            }
//        }

        //retrieve prim values from wrapped
        count = 0;
        for (Value val : baseArgs) {
            Value boxVal = wrappedBaseArgs.get(count);
            Type boxType = boxVal.getType();
            if (val.getType().equals(boxType) || val.getType().equals(NullType.v()))
            //if same type or null
            {
                body.getUnits().add(Jimple.v().newAssignStmt(val, boxVal));
            } else {
                //else we need to retrieve back
                Stmt stmt = Jimple.v()
                        .newAssignStmt(val, Jimple.v().newVirtualInvokeExpr((Local) boxVal,
                                SootUtils.getBackPrimMethodName((RefType) boxType).makeRef()
                        ));
                body.getUnits().add(stmt);
            }

            count++;
        }

        Local argArray = lg.generateLocal(ArrayType.v(RefType.v("java.lang.Object"), 1));
        body.getLocals().add(argArray);

        body.getUnits().add(Jimple.v().newAssignStmt(argArray,
                Jimple.v().newNewArrayExpr(RefType.v("java.lang.Object"),
                        IntConstant.v(wrappedBaseArgs.size()))));
        for (int i = 0; i < wrappedBaseArgs.size(); ++i) {
            Value arg = new ArrayList<Value>(wrappedBaseArgs).get(i);
            body.getUnits().add(Jimple.v()
                    .newAssignStmt(Jimple.v().newArrayRef(argArray, IntConstant.v(i)), arg));
        }
        //we omit inserting this stmt when running execution tests
        if (!ifSkipInsertRegisterHooks) {
            //add assertion registration hooks
            String assertClassName = Assertion.class.getName();
            SootClass hookClass = Scene.v().loadClassAndSupport(assertClassName);
            hookClass.setApplicationClass();
            //for(SootClass m: Scene.v().getApplicationClasses())
            //    System.out.println(m.getName());
            if (hookClass.getMethods().size() == 0) {
                throw new RuntimeException("cannot find assertion class: " + assertClassName);
            }
            SootMethod hookMethod = hookClass.getMethodByName("appendCustomizedAssert");
            hookMethod.retrieveActiveBody();
            body.getUnits().add(Jimple.v()
                    .newInvokeStmt(Jimple.v().newStaticInvokeExpr(hookMethod.makeRef(),
                            StringConstant.v(assertPoolClass.getName()), IntConstant.v(funcId), argArray)));
        }

        copyStmtsToAssertMethod(testBody, body, tobeCopiedStmts);

        // this is old way: too naive to deal with control flow
        //reverse order, so..
        //for(int i=tobeCopiedStmts.size()-1;i>=0;--i)
        //{
        //    Stmt stmt = tobeCopiedStmts.get(i);
        //    if(!body.getUnits().contains(stmt))
        //        body.getUnits().add(stmt);
        //}

        body.getUnits().add(Jimple.v().newReturnVoidStmt());

        assertMethod.setActiveBody(body);
        assertPoolClass.addMethod(assertMethod);

        SootUtils.dumpBodyJimple(body);
        try {
            body.validate();
        } catch (Exception ex)
        {
            SootUtils.printBodyJimple(body);
            //this type of exception have false positives, see testException2
            //with this we will suppress no def check
            if(!ex.getMessage().contains("There is no path from a definition of")) //no defs for value
                throw ex;
        }

        return assertMethod;
    }

    static Stmt generateAssertStmt(SootMethod assertMethod, List<Value> argLst) {
        return Jimple.v()
                .newInvokeStmt(Jimple.v().newStaticInvokeExpr(assertMethod.makeRef(), argLst));
    }

    //copy-based approach, abandoned
    @Deprecated
    static void copyStmtsToAssertMethodOld(Body testBody, Body assertMethodBody, List<Stmt> tobeCopiedStmts)
    {
        SootMethod printMethod = getPrintMethod();
        int debuggingCounter = 0;
        //scan original test body and add all colored stmts
        for (Unit unit : testBody.getUnits()) {
            Stmt stmt = (Stmt) unit;
            if (tobeCopiedStmts.contains(stmt)) {
                if (!assertMethodBody.getUnits().contains(stmt)) {
                    assertMethodBody.getUnits().add(stmt);
                    if (INSERT_PRINTF_DEBUGGING) {
                        assertMethodBody.getUnits().add(genDebuggingStmt(printMethod, debuggingCounter));
                    }
                    debuggingCounter++;
                }
            }
        }

    }

    static int insertStmtWithDebugging(Body body, Stmt stmt, int debuggingCounter)
    {
        SootMethod printMethod = getPrintMethod();
        try{
            body.getUnits().add(stmt);
            if (INSERT_PRINTF_DEBUGGING) {
                body.getUnits().add(genDebuggingStmt(printMethod, debuggingCounter));
            }

        } catch (RuntimeException ex)
        {
            //ignore if already added (ideally should not happen)
            if(!ex.getMessage().contains("Chain already contains object"))
                throw ex;
        }

        debuggingCounter++;
        return debuggingCounter;
    }

    static Unit cloneOrFetchCachedUnit(Unit unit , Map<String, Unit> cachedUnitMap)
    {
        if(cachedUnitMap.containsKey(unit.toString()))
        {
            return cachedUnitMap.get(unit.toString());
        }
        else
        {
            Unit clonedUnit = (Unit)unit.clone();
            cachedUnitMap.put(clonedUnit.toString(),clonedUnit);
            return clonedUnit;
        }
    }

    //the target of goto should be adjusted to next available stmt
    static Unit adjustTargetStmt(Body testBody, Unit targetUnit, List<Stmt> tobeCopiedStmts)
    {
        Unit adjustedTarget = targetUnit;
        while(!tobeCopiedStmts.contains(adjustedTarget))
            adjustedTarget = testBody.getUnits().getSuccOf(adjustedTarget);

        return adjustedTarget;
    }

    //filter-based
    static void copyStmtsToAssertMethod(Body testBody, Body assertMethodBody, List<Stmt> tobeCopiedStmts)
    {
        int debuggingCounter = 0;
        Map<String, Unit> cachedUnitMap = new HashMap<String, Unit>();

        //scan original test body and add all colored stmts
        for (Unit unit : testBody.getUnits()) {
            Stmt stmt = (Stmt) (unit);

            if(stmt instanceof GotoStmt)
            {
                //adjust the target of goto, otherwise throw java.lang.RuntimeException: BlockGraph(): block tail mapped to null block!
                Unit targetUnit = ((GotoStmt)stmt).getTarget();
                Unit adjustedTarget = cloneOrFetchCachedUnit(adjustTargetStmt(testBody,targetUnit, tobeCopiedStmts),cachedUnitMap);
                Stmt clonedStmt = (Stmt)cloneOrFetchCachedUnit(stmt,cachedUnitMap);
                ((GotoStmt)clonedStmt).setTarget(adjustedTarget);
            }
            else if(stmt instanceof IfStmt)
            {
                //adjust the target of goto, otherwise throw java.lang.RuntimeException: BlockGraph(): block tail mapped to null block!
                Unit targetUnit = ((IfStmt)stmt).getTarget();
                Unit adjustedTarget = cloneOrFetchCachedUnit(adjustTargetStmt(testBody,targetUnit, tobeCopiedStmts),cachedUnitMap);
                Stmt clonedStmt = (Stmt)cloneOrFetchCachedUnit(stmt,cachedUnitMap);
                ((IfStmt)clonedStmt).setTarget(adjustedTarget);
            }
        }

        //scan original test body and add all colored stmts
        for (Unit unit : testBody.getUnits()) {
            Stmt stmt = (Stmt) (unit);

            //UPDATE: now we move the caching step to individual

            //cache is to avoid re-cloning the same statement
//            boolean ifCached = false;
//            for(Map.Entry<String, Unit> entry: cachedUnitMap.entrySet())
//            //if(cachedUnit!=null && stmt.toString().equals(cachedUnit.toString()))
//            {
//                if(entry.getKey().equals(stmt.toString()))
//                {
//                    //goto target is pre-cloned
//                    //LOG.info("3UUUUUUUUUUUUUUUUU"+System.identityHashCode((Stmt)entry.getValue()));
//                    //only insert when not inserted already
//                    if(!assertMethodBody.getUnits().contains(entry.getValue()))
//                        debuggingCounter = insertStmtWithDebugging(assertMethodBody, (Stmt)entry.getValue(), debuggingCounter);
//                    ifCached = true;
//                    break;
//                }
//            }
//
//            if(ifCached)
//                continue;

            if (tobeCopiedStmts.contains(stmt)) {
                //debuggingCounter = insertStmtWithDebugging(assertMethodBody, (Stmt)(Unit)stmt.clone(), debuggingCounter);
                debuggingCounter = insertStmtWithDebugging(assertMethodBody,
                        (Stmt)(cloneOrFetchCachedUnit(stmt,cachedUnitMap)), debuggingCounter);
            }
        }
    }

    static void modifyTestBody(Body testBody, List<Stmt> wrappedInitStmts,
            List<Stmt> wrappedAssignStmts, Stmt oldAssertStmt) {
        Stmt point = SootUtils.findFirstNonIdentityStmt(testBody);

        //UPDATE: it seems adding this would often incur latent type errors, for example:
        //java.lang.ArrayIndexOutOfBoundsException: -1
        //        at org.objectweb.asm.Frame.getConcreteOutputType(Frame.java:1139)
        //        at org.objectweb.asm.Frame.merge(Frame.java:1184)
        //        at org.objectweb.asm.MethodWriter.computeAllFrames(MethodWriter.java:1607)
        //        at org.objectweb.asm.MethodWriter.visitMaxs(MethodWriter.java:1543)
        //        at soot.AbstractASMBackend.generateMethods(AbstractASMBackend.java:405)
        //        at soot.AbstractASMBackend.generateByteCode(AbstractASMBackend.java:313)
        //        at soot.AbstractASMBackend.generateClassFile(AbstractASMBackend.java:263)
        //        at edu.jhu.order.t2c.staticd.util.SootUtils.dumpSootClass(SootUtils.java:292)
        //        at edu.jhu.order.t2c.staticd.transformer.TestAssertionPacker.internalTransformSpecificClasses(TestAssertionPacker.java:224)
        //        at edu.jhu.order.t2c.staticd.transformer.TestAssertionPacker.internalTransformAll(TestAssertionPacker.java:131)
        //        at edu.jhu.order.t2c.staticd.transformer.TestAssertionPacker.internalTransform(TestAssertionPacker.java:93)
        //        at soot.SceneTransformer.transform(SceneTransformer.java:36)
        //add them at head
        for (Stmt stmt : wrappedInitStmts) {
            //LOG.debug("wrappedInitStmts "+stmt);
            testBody.getUnits().insertBefore(stmt, point);
        }

//        for(Local local:testBody.getLocals())
//        {
//            if(local.getType().equals(RefType.v("java.lang.Integer")))
//            {
//                RefType t= (RefType)local.getType();
//                testBody.getUnits().insertBefore(Jimple.v()
//                        .newAssignStmt(local, Jimple.v().newNewExpr(t)),point);
//                testBody.getUnits()
//                        .insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(local,
//                                t.getSootClass().getMethod(
//                                        SootUtils.getInitPrimMethodSignature(t))
//                                        .makeRef(), SootUtils.getConstantForPrim(t))),point);
//            }
//        }

        //add them right before the assertion
        for (Stmt stmt : wrappedAssignStmts) {
            testBody.getUnits().insertBefore(stmt, oldAssertStmt);
        }
    }

    static void genWrapperInfo(List<Stmt> wrappedInitStmts, List<Stmt> wrappedAssignStmts,
            List<Value> wrappedBaseArgs, ParamAnalysis paramAnalysis, LocalGenerator lg, Body body) {
        for (Value arg : paramAnalysis.baseArgs) {
            //skip if args are included in the parameters
            List<IdentityStmt> istmts = SootUtils.findAllIdentityStmts(body);
            boolean skip = false;
            for(IdentityStmt stmt:istmts)
            {
                if(stmt.getLeftOp().equals(arg))
                {
                    skip=true;
                    break;
                }
            }

            /*if(arg instanceof Constant)
            {
                Type boxType = SootUtils.boxBasicJavaType(arg.getType());
                Value argOrNewLocal = arg;
                if (!(arg.getType() instanceof NullType)) {
                    Local newLocal = lg.generateLocal(boxType);
                    RefType refBoxType = (RefType) boxType;
                    wrappedAssignStmts.add(Jimple.v()
                            .newAssignStmt(newLocal, Jimple.v().newNewExpr(refBoxType)));

                    wrappedAssignStmts
                            .add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(newLocal,
                                    refBoxType.getSootClass().getMethod(
                                                    SootUtils.getInitPrimMethodSignature(refBoxType))
                                            .makeRef(), arg)));
                    argOrNewLocal = newLocal;
                }
                wrappedBaseArgs.add(argOrNewLocal);
            }*/
            if (SootUtils.ifPrimJavaType(arg.getType())) {
                Type boxType = SootUtils.boxBasicJavaType(arg.getType());
                Value argOrNewLocal = arg;
                if (!(arg.getType() instanceof NullType)) {
                    Local newLocal = lg.generateLocal(boxType);
                    RefType refBoxType = (RefType) boxType;
                    wrappedAssignStmts.add(Jimple.v()
                            .newAssignStmt(newLocal, Jimple.v().newNewExpr(refBoxType)));

                    if(!skip)
                    wrappedInitStmts.add(Jimple.v().newAssignStmt(arg, SootUtils.getConstantForPrim(arg.getType())));
                    //wrappedInitStmts.add(Jimple.v().newAssignStmt(newLocal, SootUtils.getConstantForPrim(arg.getType())));
//                    wrappedInitStmts.add(Jimple.v().newAssignStmt(newLocal, Jimple.v().newNewExpr(refBoxType)));
//                    wrappedInitStmts.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(newLocal,
//                            refBoxType.getSootClass().getMethod(
//                                        SootUtils.getInitPrimMethodSignature(refBoxType))
//                                        .makeRef(), SootUtils.getConstantForPrim(refBoxType))));
                    wrappedAssignStmts
                            .add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(newLocal,
                                    refBoxType.getSootClass().getMethod(
                                            SootUtils.getInitPrimMethodSignature(refBoxType))
                                            .makeRef(), arg)));
                    argOrNewLocal = newLocal;
                }
                wrappedBaseArgs.add(argOrNewLocal);
            } else if(arg.getType() instanceof RefType)
            {
                if (!skip) {
                    RefType t = (RefType) arg.getType();
                    if (t.getSootClass().isEnum())
                        wrappedInitStmts.add(Jimple.v().newAssignStmt(arg, NullConstant.v()));
                    else if (t.getClassName().equals("java.lang.Integer")) {
                        wrappedInitStmts.add(Jimple.v()
                                .newAssignStmt(arg, Jimple.v().newNewExpr(t)));
                        wrappedInitStmts
                                .add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr((Local) arg,
                                        t.getSootClass().getMethod(
                                                        SootUtils.getInitPrimMethodSignature(t))
                                                .makeRef(), IntConstant.v(0))));
                    } else if (t.getClassName().equals("java.lang.Long")) {
                        wrappedInitStmts.add(Jimple.v()
                                .newAssignStmt(arg, Jimple.v().newNewExpr(t)));
                        wrappedInitStmts
                                .add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr((Local) arg,
                                        t.getSootClass().getMethod(
                                                        SootUtils.getInitPrimMethodSignature(t))
                                                .makeRef(), LongConstant.v(0))));
                    }
                }
                wrappedBaseArgs.add(arg);
            } else if(arg.getType() instanceof ArrayType)
            {
                ArrayType t = (ArrayType) arg.getType();

                if(!skip)
                    if(t.baseType.equals(IntType.v()))
                {
                    wrappedInitStmts.add(Jimple.v()
                            .newAssignStmt(arg, Jimple.v().newNewArrayExpr(t.baseType, IntConstant.v(t.numDimensions))));
                    for(int i=0;i<t.numDimensions;++i)
                    {
                        wrappedInitStmts.add(Jimple.v()
                                .newAssignStmt(Jimple.v().newArrayRef(arg, IntConstant.v(i)), SootUtils.getConstantForPrim(t.baseType)));
                    }
                }

                wrappedBaseArgs.add(arg);
            }
            else {
                wrappedBaseArgs.add(arg);
            }
        }
    }

    static SootMethod getPrintMethod() {
        SootClass assertClass = Scene.v().loadClassAndSupport(Assertion.class.getName());
        assertClass.setApplicationClass();
        SootMethod printMethod = assertClass.getMethodByName("print");
        printMethod.retrieveActiveBody();
        return printMethod;
    }

    static Stmt genDebuggingStmt(SootMethod printMethod, int counter) {
        return Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(
                printMethod.makeRef(), StringConstant.v(Integer.toString(counter))));
    }

    public void internalTransformForTest(Body body) {
        TFResult result = internalTransform(body, false);
        if (!result.equals(TFResult.SUCCESS)
                && !result.equals(TFResult.SIMPLE))
            throw new RuntimeException("transform test fails!");
    }
}
