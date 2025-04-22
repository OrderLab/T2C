package edu.jhu.order.t2c.staticd.cases;

import org.junit.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

public class AssertionSample {
    int a = 0;
    int b = 0;
    int c = 0;
    int[] arrA = {0,1,2};

    //invocation of this class methods will be recognized as "simple" assertions
    static class SimpleAssertionTester{
        static boolean op(int a, int b)
        {
            return true;
        }

        static long size(int a, int b)
        {
            return 3000L;
        }
    }

    public class DummySubClass{
        public int subCall()
        {
            return 1;
        }
    }

    public static class SubClass{
        static int count = 0;
        public int create()
        {
            if(count>0)
                throw new Error("this function should not be called twice");
            count++;
            return 1;
        }

        public int read()
        {
            return 1;
        }
    }

    public static class SubClass2{
        static int count = 0;

        boolean prepared = false;
        public int create()
        {
            if(count>0)
                throw new Error("this function should not be called twice");
            count++;
            return 1;
        }

        public void prepare()
        {
            prepared = true;
        }

        public int read()
        {
            if(!prepared)
            {
                throw new Error("Object not prepared!");
            }
            return 1;
        }
    }

    public enum DummyEnum {
        D1, D2;
    }

    public static int foo0(int x, int y) {
        return x + y;
    }

    public static Integer foo1(Integer x, Integer y) {
        return x + y;
    }

    public static DummyEnum foo2(DummyEnum x) {
        return x;
    }

    public static Long foo3(Long x, Integer y) {
        return x + y;
    }

    //this method is not actually used, but we found it can trigger an interesting bug due to sharing the same name
    //existing methods. the caused exception looks like:
    // [main] ERROR edu.jhu.order.t2c.staticd.util.SootUtils - Exception when writing class edu.jhu.order.t2c.staticd.cases.AssertionSample
    // java.lang.RuntimeException: Attempting to use 'this' in static method
    //Update: it seems to be a bug in Soot. We compared the output with and without this function, and both of them generate similar jimple code.
    /**
    public static int foo4(Integer x, Integer y) {
        if(true)
            throw new NullPointerException();

        return 1;
    }
    **/
    public static int foo8(Integer x, Integer y) {
        if(true)
            throw new NullPointerException();

        return 1;
    }

    public static void testSimple2(int x, int y, int z) throws Exception {

        int d = z + 1;

        assertThat(foo0(x, y), is(d));
    }

    @Test
    public void testSimple() throws Exception {

        int d = c + 1;

        assertThat(foo0(a, b), is(d));
    }

    @Test
    public void testSimpleCopy() throws Exception {

        int d = c + 1;

        assertThat(foo0(a, b), is(d));
    }

    @Test
    public void testConstant() throws Exception {

        int d = c + 1;

        assertTrue(!SimpleAssertionTester.op(d, 2));
    }
    @Test
    public void testConstant2() throws Exception {

        int d = c + 1;

        assertEquals(SimpleAssertionTester.size(d, 2),5000L);
    }

    @Test
    public void testJump() throws Exception {

        int d = c + 1;
        boolean cond = false;
        cond = foo0(a, b) > d;
        assertTrue(cond);
    }

    //should not throw assert error
    @Test
    public void testJumpNotAssert() throws Exception {

        int d = c + 1;
        boolean cond = false;
        cond = foo0(a, b) < d;
        assertTrue(cond);
    }

    @Test
    public void testForLoop() throws Exception {

        boolean cond = true;
        for (int i = 0; i < 10; ++i)
            cond = false;
        assertTrue(cond);
    }

    @Test
    public void testForLoopAdvanced() throws Exception {

        int counter = 0;
        for (int i = 0; i < 10; ++i)
            counter++;
        assertTrue(counter < 10);
    }

    @Test
    public void testForLoopAdvanced2() throws Exception {

        boolean cond = false;
        for (int i = 0; i < 10 && !cond; ++i)
            cond = foo0(a, b) < 0;
        assertTrue(cond);
    }

    @Test
    public void testForLoopAdvanced3() throws Exception {

        boolean cond = false;
        for (int i = 0; i < 10; ++i)
        {
            cond = foo0(a, b) < 0;
            assertTrue(cond);
        }
    }

    @Test
    public void testForLoopAdvanced4() throws Exception {

        boolean cond = false;
        for (int i = 0; i < 10; ++i)
        {
            foo1(a,b);
            cond = foo0(a+i, b) < 0;
            assertTrue(cond);
        }
    }

    @Test
    public void testBranches() throws Exception {

        int d = c + 1;
        boolean cond = false;
        if(d<10)
        {
            cond = foo0(a, b) > d;
            assertTrue(cond);
        }
    }

    @Test
    public void testException() throws Exception {

        int d = foo0(a, b);
        boolean cond = false;
        try {
            cond = foo0(a, b) > 2;
            assertTrue(cond);
        } finally {
            if(d==5)
                foo0(a,b);
        }
    }


    //simplified version from testSplitFlushCompactUnknownTable in HBase
    @Test
    public void testException2() throws Exception {
        Exception exception = null;
        try {
            foo8(a,b);
        } catch (Exception e) {
            exception = e;
        }
        assertTrue(exception instanceof NullPointerException);
    }

    @Test
    public void testForMultipleAssertions() throws Exception {
        int d = c + 1;
        boolean cond = false;
        cond = foo0(a, b) > d;
        assertFalse(!cond);
        assertTrue(cond);
    }

    @Test
    public void testForNullType() throws Exception {
        int d = c + 1;
        boolean cond = false;
        cond = foo1(null, null) < d;
        assertTrue(cond);
    }

    @Test
    public void testEnum() throws Exception {
        boolean cond = false;
        cond = DummyEnum.D2.equals(DummyEnum.D1);
        assertTrue(cond);
    }

    @Test
    public void testMultiEnum() throws Exception {
        boolean cond = false;
        cond = foo2(DummyEnum.D2).equals(DummyEnum.D1);
        assertTrue(cond);
    }

    @Test
    public void testEnumNotInitialized() throws Exception {
        boolean cond = false;
        if(foo1(3,5)>10)
            cond = foo2(null).equals(DummyEnum.D1);
        assertTrue(cond);
    }

    @Test
    public void testObjNotInitialized() throws Exception {
        boolean cond = false;
        DummySubClass c = new DummySubClass();
        if(foo1(3,5)>10)
            cond = foo2(null).equals(c.subCall());
        assertTrue(cond);
    }

    @Test
    public void testDouble() throws Exception {

        double d = c + 1.0;

        assertThat(foo0(a, b), is(d));
    }

    @Test
    public void testLong() throws Exception {

        Long e;
        if(foo0(1,2)>0)
        {
            long d = c + 1;
            e = d;
            assertThat(foo3(e, 1), is(a));
        }
    }

    @Test
    public void testIntArray() throws Exception {

        int e;
        if(foo0(1,2)<0)
        {
            int d = c + 1;
            e = d;
        }
        else{

            assertThat(arrA[0]+1, is(a));
        }
    }

    @Test
    public void testInterFunctionAssert() throws Exception {
        foo4();
    }

    @Test
    public void testInterFunctionAssert2() throws Exception {
        boolean cond = false;
        assertFoo6(cond);
    }

    @Test
    public void testInterFunctionAssert3() throws Exception {
        foo9(new byte[1],new byte[1][1],new int[1],1,1,true);
    }

    void foo4() throws Exception {
        int x = 10;
        foo5(x);
    }

    void foo5(int x) throws Exception {

        boolean cond = foo0(x,10)<5;
        assertTrue(cond);
    }

    void assertFoo6(boolean cond) throws Exception {
        cond = DummyEnum.D2.equals(DummyEnum.D1);
        assertTrue(cond);
    }

    //this test is modeled after testForceSplit from HBase
    void foo9(byte[] var1, byte[][] var2, int[] var3, int var4, int var5, boolean var6) throws Exception {
        //this assertion is intentionally expected to pass
        assertFalse(foo0(1,1)<1);
        foo10(var1, var2, var4, var5);
    }

    void foo10(byte[] var1, byte[][] var2,int a, int b)
    {
        if(a==1 || b ==1)
        {
            throw new AssertionError("expected");
        }
    }

    //@Test
    //public void testObjectArguments() throws Exception {
    //    SubClass obj = new SubClass(10);
    //    int result = obj.read();
    //    assertTrue(result > 5);
    //}

    @Test
    public void testIncludeAllRelatedOperations() throws Exception {
        SubClass2 obj = new SubClass2();
        obj.prepare();
        int result = obj.read();
        assertTrue(result > 5);
    }

    @Test
    public void testExcludeImpureOperations() throws Exception {
        SubClass2 obj = new SubClass2();
        obj.prepare();
        //create should not be included in the generated assertion, otherwise it would be invoked twice
        obj.create();
        int result = obj.read();
        assertTrue(result > 5);
    }

    public static void main(String[] args) {
        AssertionSample sample = new AssertionSample();
        try {
            if (args[0].equals("testSimple"))
                sample.testSimple();
            if (args[0].equals("testSimpleCopy"))
                sample.testSimpleCopy();
            else if (args[0].equals("testJump"))
                sample.testJump();
            else if (args[0].equals("testBranches"))
                sample.testBranches();
            else if (args[0].equals("testConstant"))
                sample.testConstant();
            else if (args[0].equals("testConstant2"))
                sample.testConstant2();
            else if (args[0].equals("testException"))
                sample.testException();
            else if (args[0].equals("testException2"))
                sample.testException2();
            else if (args[0].equals("testForMultipleAssertions"))
                sample.testForMultipleAssertions();
            else if (args[0].equals("testJumpNotAssert"))
                sample.testJumpNotAssert();
            else if (args[0].equals("testForLoop"))
                sample.testForLoop();
            else if (args[0].equals("testForLoopAdvanced"))
                sample.testForLoopAdvanced();
            else if (args[0].equals("testForLoopAdvanced2"))
                sample.testForLoopAdvanced2();
            else if (args[0].equals("testForLoopAdvanced3"))
                sample.testForLoopAdvanced3();
            else if (args[0].equals("testForLoopAdvanced4"))
                sample.testForLoopAdvanced4();
            else if (args[0].equals("testForNullType"))
                sample.testForNullType();
            else if (args[0].equals("testEnum"))
                sample.testEnum();
            else if (args[0].equals("testMultiEnum"))
                sample.testMultiEnum();
            else if (args[0].equals("testEnumNotInitialized"))
                sample.testEnumNotInitialized();
            else if (args[0].equals("testObjNotInitialized"))
                sample.testObjNotInitialized();
            else if (args[0].equals("testDouble"))
                sample.testDouble();
            else if (args[0].equals("testLong"))
                sample.testLong();
            else if (args[0].equals("testIntArray"))
                sample.testIntArray();
            else if (args[0].equals("testInterFunctionAssert"))
                sample.testInterFunctionAssert();
            else if (args[0].equals("testInterFunctionAssert2"))
                sample.testInterFunctionAssert2();
            else if (args[0].equals("testInterFunctionAssert3"))
                sample.testInterFunctionAssert3();
            else if (args[0].equals("testIncludeAllRelatedOperations"))
                sample.testIncludeAllRelatedOperations();
            else if (args[0].equals("testExcludeImpureOperations"))
                sample.testExcludeImpureOperations();
            else
                throw new RuntimeException("illgeal arguments!");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
