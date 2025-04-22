package edu.jhu.order.t2c.dynamicd.tscheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import edu.jhu.order.t2c.dynamicd.runtime.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import edu.jhu.order.t2c.staticd.tool.TemplateMetricsAnalyzer;
import edu.jhu.order.t2c.staticd.tool.TemplateValidationMerger;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;

//how to run: 1) ant jar && ant test (to force compile test classes, no need to run tests) 2) run our engine
//java -cp "build/test/classes/:lib/*:build/classes/:build/lib/*:build/test/lib/*" edu.jhu.order.t2c.dynamicd.tscheduler.TestEngine

/*
order here is extremely important!
previously we met a bug that some methods cannot be found, but they are indeed in the package, later
we found out somehow in the build/lib/jars/ there are some same packages but older versions, cause cannot find methods
*/

/**
 * the offline test case engine
 * the purpose of this part is to track each test and do two things
 * 1) divide execution of each test so we are when we have some outputs what are the current running test, by
 * printing out the test class and method name
 * 2) start our checker at the beginning and do periodically checking
 */
public class TestEngine {
    static{
        ConfigManager.initConfig();
    }

    public static void spawnProcess(String klass) throws IOException,
            InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = TestEngine.class.getName();

        List<String> cmds = new ArrayList<>(Arrays.asList(javaBin, "-cp", classpath,
                 "-Dconf="+ConfigManager.CONFIG_FILE_NAME,
                 "-Dlog4j.configuration="+System.getProperty("log4j.configuration"),
                 "-noverify",
                 "-Dt2c.testname="+klass,
                 "-Dt2c.mode="+System.getProperty("t2c.mode"),
                 "-Dt2c.conf="+System.getProperty("t2c.conf"),
                 "-Dt2c.ticket_id="+System.getProperty("t2c.ticket_id"), //e.g. ZK-1208, this is specified implicitly by property file name
                 "-Dt2c.t2c_root_abs_path="+System.getProperty("t2c.t2c_root_abs_path"),
                 "-Dt2c.target_system_abs_path="+System.getProperty("t2c.target_system_abs_path"),
                 "-Dt2c.test_trace_prefix="+System.getProperty("t2c.test_trace_prefix")));
        String jvm_args = ConfigManager.config.getString(ConfigManager.JVM_ARGS_FOR_TESTS_KEY);
        if(!jvm_args.equals(""))
            cmds.addAll(Arrays.asList(jvm_args.split("\\s+")));
        cmds.add(className);
        ProcessBuilder builder = new ProcessBuilder(cmds);

        Process process = builder.inheritIO().start();
        //set the timeout threshold to be
        boolean exited = process.waitFor(10, TimeUnit.MINUTES);
        if(!exited) {
            LocalDateTime now = LocalDateTime.now();
            System.out.println("#### "+now.toString()+" Test "+klass+" timeout, terminates it forcibly.");
            process.destroyForcibly();
        }
    }

    public static void execSingleTest(String className) {
        JUnitCore core = new JUnitCore();
        core.addListener(new edu.jhu.order.t2c.dynamicd.tscheduler.TestListener());
        core.addListener(new TextListener(System.out));//T2CHelper.getInstance().getLogStream()));

        try {
            //iterate
            Result result = core.run(edu.jhu.order.t2c.dynamicd.tscheduler.TestClassPool.getClass(className));
            T2CHelper.globalLogInfo(className + " failures: " + result.getFailureCount() + "/" + result.getRunCount());
            if(GlobalState.mode.equals(GlobalState.T2CMode.BUILD))
                TemplateManager.getInstance().flushAllTemplates();

            //do clean exit
            System.exit(0);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //we only use customized assertion now
    @Deprecated
    static void modifyAssert()
    {
        try{
            ClassPool pool = ClassPool.getDefault();
            CtClass cc = pool.get("org.junit.Assert");

            Map<String, String> map = new HashMap<>();

            map.put("assertEquals", "EQUALS,$2");
            map.put("assertNotEquals", "NOTEQUALS,$2");
            map.put("assertTrue", "TRUE,$2");
            map.put("assertFalse", "FALSE,$2");
            map.put("assertNull", "NULL,null");
            map.put("assertSame", "SAME,null");
            map.put("assertNotSame", "NOTSAME,null");

            String assertionClassName = Assertion.class.getName();
            for(Map.Entry<String, String> entry:map.entrySet())
            {
                CtMethod m = cc.getDeclaredMethod(entry.getKey());
                m.insertBefore("{"+assertionClassName+".appendAssert(new "+assertionClassName+
                        "("+assertionClassName+".AssertType."+entry.getValue()+"));}");
            }

            cc.toClass();
        } catch (Exception ex)
        {
            System.err.println(ex);
            ex.printStackTrace();
        }
    }

    static void modifyTestBody(String className)
    {
        if(GlobalState.mode.equals(GlobalState.T2CMode.BUILD))
            OperationRegister.registerAssertFail(className);

        String assertionClassName = Assertion.class.getName();
        String operationClassName = Operation.class.getName();
        String templateManagerClassName = TemplateManager.class.getName();

        try{
            ClassPool pool = ClassPool.getDefault();
            CtClass clazz = pool.get(className);
            List<CtClass> ccList = new ArrayList<>();

            //we cannot only register for one class, because some test class might
            //extends others, for example, org.apache.zookeeper.test.ChrootAsyncTest
            //in this case we need to instrument for superclass, otherwise the starting
            //and ending point cannot be correctly marked
            while(clazz.getSuperclass()!=null)
            {
                ccList.add(0,clazz);
                clazz = clazz.getSuperclass();
            }

            for(CtClass cc:ccList) {
                for (CtMethod m : cc.getMethods()) {
                    if (m.hasAnnotation("org.junit.Test")) {

                        //add sysworkload here, see SysWorkloadPool.java approach 2 for more explanation
                        //m.insertBefore("{"+SysWorkloadPool.class.getName()+".setSysWorkload(\""+ cc.getName()+"\", this);}");

                        m.insertBefore(
                                "{" + templateManagerClassName + ".getInstance().addOp(new " +
                                        operationClassName + "(" + operationClassName
                                        + ".OpTypeBasicImpl.TESTBODYBEGIN,\"\"));}");
                        m.insertBefore(
                                "{" + templateManagerClassName + ".getInstance().addAssert(new " +
                                        assertionClassName + "(" + assertionClassName
                                        + ".AssertType.TESTBODYBEGIN,null));}");
                        m.insertAfter("{" + templateManagerClassName + ".getInstance().addOp(new " +
                                operationClassName + "(" + operationClassName
                                + ".OpTypeBasicImpl.TESTBODYEND,\"\"));}");
                        m.insertAfter(
                                "{" + templateManagerClassName + ".getInstance().addAssert(new " +
                                        assertionClassName + "(" + assertionClassName
                                        + ".AssertType.TESTBODYEND,null));}");
                    }
                }
            }

            //freeze all modified classes at the end to avoid dependency issue
            for(CtClass cc:ccList) {
                cc.toClass();
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public static void modifyOperationEntry()
    {
        //those classes include operation impl methods
        String[] interfaceClassList = ConfigManager.config.getStringArray(ConfigManager.OP_INTERFACE_CLASS_LIST_KEY);
        //you can configure which methods need to be registered
        String[] interfaceMethodList = ConfigManager.config.getStringArray(ConfigManager.OP_INTERFACE_METHOD_LIST);
        //System.out.println("those methods will be registered: " + Arrays.toString(interfaceMethodList));
        Map<String, Set<String>> instMap = new HashMap<String, Set<String>>();
        if(interfaceClassList.length > 0)
            for(String clazz: interfaceClassList)
                instMap.put(clazz,new HashSet<String>());
        if(interfaceMethodList.length > 0)
            for(String method: interfaceMethodList)
            {
                String[] parts = method.split("#");
                if(parts.length != 2)
                    continue;
                String className = parts[0];
                String methodName = parts[1]; 
                //System.out.println("#### ClassName is "+className+", methodname is "+methodName);         
                if (!instMap.containsKey(className)) {
                    Set<String> methodSet = new HashSet<>();
                    methodSet.add(methodName);
                    instMap.put(className, methodSet);
                }
                else
                    instMap.get(className).add(methodName);
            }
        if(instMap.isEmpty()) {
            throw new RuntimeException("interfaceClassList and interfaceMethodList are all empty!");
        }
        OperationRegister.registerAll(instMap);
    }

    static void modifyTestSetupMethod(String className)
    {
        OperationRegister.registerSystemConfigConstraints(className);
    }

    public static void runAllTests()
    {
        try {
            //register all test classes by default
            TestClassPool.registerAllClass();

            int count = 0;
            int testLimit = ConfigManager.config.getInt(ConfigManager.VERIFY_ABORT_AFTER_N_TESTS_KEY);
            for (String clazz : edu.jhu.order.t2c.dynamicd.tscheduler.TestClassPool.getClasses()) {
                if(GlobalState.mode.equals(GlobalState.T2CMode.VALIDATION) && testLimit <=count && testLimit!=0)
                {
                    System.out.println("Abort validating all tests due to "+ConfigManager.VERIFY_ABORT_AFTER_N_TESTS_KEY+" set as "+testLimit);
                    break;
                }

                System.out.println("Spawn test for "+(count+1)+"/"+ edu.jhu.order.t2c.dynamicd.tscheduler.TestClassPool.getClasses().size());
                spawnProcess(clazz);
                count++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String... args) {

        //set the mode
        GlobalState.mode = GlobalState.T2CMode.BUILD;
        if((System.getProperty("t2c.mode")!=null && System.getProperty("t2c.mode").equals("validate")))
        {
            System.out.println("Running in validation mode.");
            GlobalState.mode = GlobalState.T2CMode.VALIDATION;
        }

        //spawned process
        String className = System.getProperty("t2c.testname");
        if (className!=null && !className.equals("")){
            modifyTestSetupMethod(className);
            modifyTestBody(className);
            //modifyAssert();
            modifyOperationEntry();

            execSingleTest(className);
        }
        else {
            //if in the verify mode, backup results in previous runs if any
            if(GlobalState.mode.equals(GlobalState.T2CMode.VALIDATION))
            {
                FileLayoutManager.backupDir(FileLayoutManager.getPathForVerifiedInvOutputDir());
            }

            //main entry
            runAllTests();

            //if in the verify mode, aggregate the verify result
            if(GlobalState.mode.equals(GlobalState.T2CMode.VALIDATION))
            {
                TemplateValidationMerger merger = new TemplateValidationMerger();
                merger.output_dir = FileLayoutManager.getPathForVerifiedInvOutputDir();
                merger.aggregate();
            }
            else if (GlobalState.mode.equals(GlobalState.T2CMode.BUILD))
            {
                //at the end we would like to count total generated templates, by loading generated templates back to memory,
                //cause previously we dump in seperate processes
                TemplateMetricsAnalyzer.collectGlobalMetrics(TemplateManager.templateDir);
            }
            else{
                throw new RuntimeException("Impossible!");
            }

            //force the process to finish, otherwise may still hang after job finishes
            System.exit(0);
        }
    }
}
