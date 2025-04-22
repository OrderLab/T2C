package edu.jhu.order.t2c.dynamic;

import edu.jhu.order.t2c.dynamicd.runtime.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuntimeDetectionTest {

    public static final String inputDir = "./src/test/java/edu/jhu/order/t2c/dynamic/recipe";
    public static final String valueFilePath = "./src/test/java/edu/jhu/order/t2c/dynamic/recipe/val";

    public static boolean assertTestSimple = false;
    public static boolean assertTestArray = false;
    public static boolean assertTestComplexObjects = false;
    public static boolean assertTestComplexObjects2 = false;
    public static boolean assertTestDefault = false;
    public static boolean assertTestSystemWorkloads = false;

    @BeforeClass
    public static void setup() throws Exception {
        SysWorkloadPool.sysWorkloadTypeSet.add(RuntimeDetectionTest.class.getName());
    }

    @Test
    public void testSimple() throws Exception {
        RuntimeTracer.templateDir = inputDir;
        RuntimeTracer tracer = RuntimeTracer.getInstance();
        LinkedHashMap<String, Object[]> opMap = new LinkedHashMap<String, Object[]>()
        {{
            put("aa", new Object[]{7});
        }};

        for (Map.Entry<String, Object[]> entry : opMap.entrySet()) {
            Operation op = Operation.createOperation(entry.getKey(), entry.getValue());
            tracer.addTrace(op);
        }

        Thread.sleep(1000);
        if(!assertTestSimple)
            Assert.fail("customized function does not successfully invoke");
    }

    @Test
    public void testArray() throws Exception {
        RuntimeTracer.templateDir = inputDir;
        RuntimeTracer tracer = RuntimeTracer.getInstance();
        LinkedHashMap<String, Object[]> opMap = new LinkedHashMap<String, Object[]>()
        {{
            put("bb", new Object[]{"1",2,new short[3]});
        }};

        for (Map.Entry<String, Object[]> entry : opMap.entrySet()) {
            Operation op = Operation.createOperation(entry.getKey(), entry.getValue());
            tracer.addTrace(op);
        }

        Thread.sleep(1000);
        if(!assertTestArray)
            Assert.fail("customized function does not successfully invoke");
    }

    public static class ComplexObject{
        int x;
        String y;
        File file;

        public ComplexObject(int x, String y, File file) {
            this.x = x;
            this.y = y;
            this.file = file;
        }
    }

    @Test
    public void testObjects() throws Exception {
        RuntimeTracer.templateDir = inputDir;
        RuntimeTracer tracer = RuntimeTracer.getInstance();
        LinkedHashMap<String, Object[]> opMap = new LinkedHashMap<String, Object[]>()
        {{
            put("cc", new Object[]{"1",2,new ComplexObject(3,"4",new File(valueFilePath))});
        }};

        //System.out.println(CheckerTemplate.gsonPrettyPrinter.toJson(new ComplexObject(3,"4",new File(valueFilePath))));
        for (Map.Entry<String, Object[]> entry : opMap.entrySet()) {
            Operation op = Operation.createOperation(entry.getKey(), entry.getValue());
            tracer.addTrace(op);
        }

        Thread.sleep(1000);
        if(!assertTestComplexObjects)
            Assert.fail("customized function does not successfully invoke");
    }

    @Test
    public void testObjects2() throws Exception {
        RuntimeTracer.templateDir = inputDir;
        RuntimeTracer tracer = RuntimeTracer.getInstance();
        LinkedHashMap<String, Object[]> opMap = new LinkedHashMap<String, Object[]>()
        {{
            put("dd", new Object[]{"1",3,valueFilePath});
        }};

        for (Map.Entry<String, Object[]> entry : opMap.entrySet()) {
            Operation op = Operation.createOperation(entry.getKey(), entry.getValue());
            tracer.addTrace(op);
        }

        Thread.sleep(1000);
        if(!assertTestComplexObjects2)
            Assert.fail("customized function does not successfully invoke");
    }

    //test if default value in symbol map can work, if not, error will throw like:
    //Cannot find symbol in the map:$849
    @Test
    public void testDefaultValue() throws Exception {
        RuntimeTracer.templateDir = inputDir;
        RuntimeTracer tracer = RuntimeTracer.getInstance();
        LinkedHashMap<String, Object[]> opMap = new LinkedHashMap<String, Object[]>()
        {{
            put("ee", new Object[]{0});
        }};

        for (Map.Entry<String, Object[]> entry : opMap.entrySet()) {
            Operation op = Operation.createOperation(entry.getKey(), entry.getValue());
            tracer.addTrace(op);
        }

        Thread.sleep(1000);
        if(!assertTestDefault)
            Assert.fail("customized function does not successfully invoke");
    }

    void internalUtil()
    {
        RuntimeDetectionTest.assertTestSystemWorkloads = true;
    }

    @Test
    public void testSystemWorkloads() throws Exception {
        RuntimeTracer.templateDir = inputDir;
        RuntimeTracer tracer = RuntimeTracer.getInstance();
        LinkedHashMap<String, Object[]> opMap = new LinkedHashMap<String, Object[]>()
        {{
            put("ff", new Object[]{0});
        }};

        for (Map.Entry<String, Object[]> entry : opMap.entrySet()) {
            Operation op = Operation.createOperation(entry.getKey(), entry.getValue());
            tracer.addTrace(op);
        }

        Thread.sleep(1000);
        if(!assertTestSystemWorkloads)
            Assert.fail("customized function does not successfully invoke");
    }
}
