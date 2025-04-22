package edu.jhu.order.t2c.staticd.analysis;

import edu.jhu.order.t2c.staticd.T2CTest;
import edu.jhu.order.t2c.staticd.TestHelper;
import edu.jhu.order.t2c.staticd.algorithm.ParamAnalysis;
import edu.jhu.order.t2c.staticd.cases.AssertionSample;
import edu.jhu.order.t2c.staticd.cases.AssertionSampleZK;
import edu.jhu.order.t2c.staticd.transformer.TestAssertionPacker;
import edu.jhu.order.t2c.staticd.util.SootUtils;
import org.junit.Assert;
import org.junit.Test;
import soot.*;
import soot.javaToJimple.DefaultLocalGenerator;
import soot.jimple.Stmt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * you can use this to see the generated result
 * java -jar lib/procyon-decompiler-0.5.30.jar sootTestOutput/edu/jhu/order/t2c/staticd/cases/AssertionSample.class
 *
 * you can test the execution of generated test manually by:
 * java -cp sootTestOutput/:./target/test-classes/:./target/t2c-1.0-SNAPSHOT-jar-with-dependencies.jar edu.jhu.order.t2c.staticd.cases.AssertionSample testEnum
 */
public class ParamAnalysisTest extends T2CTest {

    /**
     * test the simplest case
     */
    //@Test
    //disable for now
    public void testSimple() {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testSimple");
        LocalGenerator lg = new DefaultLocalGenerator(body);
        TestAssertionPacker.AssertSuite assertSuite = TestAssertionPacker.getAssertSuites(body).get(0);
        ParamAnalysis paramAnalysis = new ParamAnalysis(body, assertSuite.argsOfAssert, assertSuite.oldAssertStmt, lg);

        int expected_size1 = 9;
        Assert.assertTrue("intermediateLocals size does not match! Expected: " + expected_size1 + ", Actual: " +
                paramAnalysis.intermediateLocals, paramAnalysis.intermediateLocals.size() == expected_size1);
        //constants should also be marked because we don't know if that's field access
        int expected_size2 = 5;//if not consider @this, should be 4
        Assert.assertTrue("baseArgs size does not match! Expected: " + expected_size2 + ", Actual: " +
                paramAnalysis.baseArgs, paramAnalysis.baseArgs.size() == expected_size2);

    }


    @Test
    public void testSimpleExec() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testSimpleCopy");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try{
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testSimpleCopy");
    }

    @Test
    public void testConstant() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testConstant");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try{
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testConstant");
    }

    @Test
    public void testConstant2() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testConstant2");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);

        try{
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testConstant2");
    }

    /**
     * test the case with jumps
     */
    @Test
    public void testJump() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testJump");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try{
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testJump");
    }

    /**
     * test the case with loops
     */
    @Test
    public void testLoop() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testForLoop");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testForLoop");
    }


    /**
     * test the case with loops
     */
    @Test
    public void testLoopAdvanced() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testForLoopAdvanced");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testForLoopAdvanced");
    }

    /**
     * test the case with loops
     */
    @Test
    public void testLoopAdvanced2() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testForLoopAdvanced2");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testForLoopAdvanced2");
    }

    /**
     * test the case with loops
     */
    @Test
    public void testLoopAdvanced3() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testForLoopAdvanced3");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testForLoopAdvanced3");


    }

    /**
     * test the case with loops
     */
    @Test
    public void testLoopAdvanced4() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testForLoopAdvanced4");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testForLoopAdvanced4");

    }

    @Test
    public void testBranches() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testBranches");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testBranches");

    }

    @Test
    public void testException() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testException");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testException");

    }

    @Test
    public void testException2() throws Exception {
        String testName = "testException2";
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), testName);
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute(testName);

    }

    @Test
    public void testForMultipleAssertions() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testForMultipleAssertions");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testForMultipleAssertions");


    }

    @Test
    public void testForNullType() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testForNullType");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testForNullType",false,false,true);

    }

    @Test
    public void testEnum() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testEnum");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testEnum");

    }

    @Test
    public void testMultiEnum() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testMultiEnum");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testMultiEnum");

    }

    @Test
    public void testEnumNotInitialized() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testEnumNotInitialized");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testEnumNotInitialized");
    }

    @Test
    public void testObjNotInitialized() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testObjNotInitialized");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testObjNotInitialized");

    }

    @Test
    public void testDouble() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testDouble");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testDouble");

    }

    @Test
    public void testLong() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testLong");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testLong");

    }

    @Test
    public void testIntArray() throws Exception {
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), "testIntArray");
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute("testIntArray");

    }

    @Test
    public void testInterFunctionAssert() throws Exception {
        String testName = "testInterFunctionAssert";
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), testName);
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute(testName);

    }

    @Test
    public void testInterFunctionAssert2() throws Exception {
        String testName = "testInterFunctionAssert2";
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), testName);
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute(testName);

    }

    @Test
    public void testInterFunctionAssert3() throws Exception {
        String testName = "testInterFunctionAssert3";
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), testName);
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute(testName,true,false,false);

    }

    @Test
    public void testIncludeAllRelatedOperations() throws Exception {
        String testName = "testIncludeAllRelatedOperations";
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), testName);
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute(testName);

    }

    @Test
    public void testExcludeImpureOperations() throws Exception {
        String testName = "testExcludeImpureOperations";
        Body body = helper.getBody(AssertionSample.class.getCanonicalName(), testName);
        TestAssertionPacker packer = new TestAssertionPacker();
        helper.recoverAllMethodBody();
        packer.internalTransformForTest(body);
        try {
            //dumped jimple not updated, not sure why
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes

        testExecute(testName);

    }

    @Test
    public void testZKCase() throws Exception {
        Body body = helper.getBody(AssertionSampleZK.class.getCanonicalName(), "testSimple");
        TestAssertionPacker packer = new TestAssertionPacker();
        packer.internalTransformForTest(body);
        try{
            //SootUtils.dumpSootClassJimple(body.getMethod().getDeclaringClass());
            SootUtils.dumpSootClass(body.getMethod().getDeclaringClass());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            SootUtils.printSootClassJimple(body.getMethod().getDeclaringClass());
            throw ex;
        }
        //this test checks if dump legal classes
    }

    //default checking config
    void testExecute(String testName) throws Exception
    {
        testExecute(testName,true,true,false);
    }

    void testExecute(String testName, boolean checkAssertError, boolean checkCustomizedFunc,boolean checkNull) throws Exception
    {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec("java -cp sootTestOutput/:./target/test-classes/:./target/t2c-1.0-SNAPSHOT-jar-with-dependencies.jar edu.jhu.order.t2c.staticd.cases.AssertionSample "+testName);
        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

        String line = null;
        StringBuilder output = new StringBuilder();

        boolean ifAssertError = false;
        boolean ifCustomizedFunc = false;
        boolean ifNullEx = false;
        while ((line = input.readLine()) != null) {
            output.append(line);
            ifAssertError = line.contains("java.lang.AssertionError") || ifAssertError;
            ifCustomizedFunc = line.contains("externalAssertFunc") || ifCustomizedFunc;
            ifNullEx = line.contains("java.lang.NullPointerException") || ifNullEx;
        }
        if(checkAssertError)
            if (!ifAssertError)
            {
                throw new RuntimeException("Should get assertion error instead of: "+ output);
            }
        if(checkCustomizedFunc)
            if (!ifCustomizedFunc)
            {
                throw new RuntimeException("Should throw error from customized func instead of: "+output);
            }
        if(checkNull)
            if (!ifNullEx)
            {
                throw new RuntimeException("Should get NullPointerException instead of: "+ output);
            }
    }
}