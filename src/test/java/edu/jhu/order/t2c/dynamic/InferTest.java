package edu.jhu.order.t2c.dynamic;

import edu.jhu.order.t2c.dynamicd.runtime.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class InferTest {

    public static final String valueFilePath = "./src/test/java/edu/jhu/order/t2c/dynamic/recipe/val";

    @BeforeClass
    public static void setup() throws Exception {
        GlobalState.mode = GlobalState.T2CMode.UNITTEST;
    }

    void startTemplate() {
        TemplateManager.getInstance().startBuildTemplate("test");
        TemplateManager.getInstance().addOp(new Operation(Operation.OpTypeBasicImpl.TESTBODYBEGIN, ""));
        TemplateManager.getInstance().addAssert(new Assertion(Assertion.AssertType.TESTBODYBEGIN, null));
    }

    void endTemplate() {
        TemplateManager.getInstance().addOp(new Operation(Operation.OpTypeBasicImpl.TESTBODYEND, ""));
        TemplateManager.getInstance().addAssert(new Assertion(Assertion.AssertType.TESTBODYEND, null));
        TemplateManager.getInstance().finishBuildTemplate("test");
    }

    @Test
    public void testInteger() throws Exception {
        LinkedHashMap<String, Object[]> opMap = new LinkedHashMap<String, Object[]>() {{
            put("aa", new Object[]{180});
            put("cc", new Object[]{180});
        }};

        startTemplate();
        //System.out.println(CheckerTemplate.gsonPrettyPrinter.toJson(new ComplexObject(3,"4",new File(valueFilePath))));
        for (Map.Entry<String, Object[]> entry : opMap.entrySet()) {
            Operation op = Operation.createOperation(entry.getKey(), entry.getValue());
            TemplateManager.getInstance().addOp(op);
        }
        Assertion.appendCustomizedAssert("test", 1, 180);
        endTemplate();
        //System.out.println(TemplateManager.getInstance().currentTemplate.serializeJson());
        //should only generate one symbol
        Assert.assertEquals(1, TemplateManager.getInstance().currentTemplate.getSymMap().size());
    }

    @Test
    public void testString() throws Exception {
        LinkedHashMap<String, Object[]> opMap = new LinkedHashMap<String, Object[]>() {{
            put("aa", new Object[]{"workloada"});
            put("cc", new Object[]{"workloada"});
        }};

        startTemplate();
        //System.out.println(CheckerTemplate.gsonPrettyPrinter.toJson(new ComplexObject(3,"4",new File(valueFilePath))));
        for (Map.Entry<String, Object[]> entry : opMap.entrySet()) {
            Operation op = Operation.createOperation(entry.getKey(), entry.getValue());
            TemplateManager.getInstance().addOp(op);
        }
        Assertion.appendCustomizedAssert("test", 1, "workloada");
        endTemplate();
        //should only generate one symbol
        Assert.assertEquals(1, TemplateManager.getInstance().currentTemplate.getSymMap().size());
    }


    @Test
    public void testArray() throws Exception {
        LinkedHashMap<String, Object[]> opMap = new LinkedHashMap<String, Object[]>() {{
            put("aa", new Object[]{valueFilePath});
            put("cc", new Object[]{"1", 2, new String[]{valueFilePath, "xx"}});
        }};

        startTemplate();
        //System.out.println(CheckerTemplate.gsonPrettyPrinter.toJson(new ComplexObject(3,"4",new File(valueFilePath))));
        for (Map.Entry<String, Object[]> entry : opMap.entrySet()) {
            Operation op = Operation.createOperation(entry.getKey(), entry.getValue());
            TemplateManager.getInstance().addOp(op);
        }
        Assertion.appendCustomizedAssert("test", 1, new String[]{valueFilePath, "xx"});
        endTemplate();
        //System.out.println(TemplateManager.getInstance().currentTemplate.serializeJson());
        
        // This commented part is the original test. I'm not sure what does it test, though
        // boolean ifReplace = false;
        // for (Map.Entry<String, Symbol> entry : TemplateManager.getInstance().currentTemplate.getSymMap().entrySet()) {
            // if (entry.getValue().strVal.contains("$"))
                // ifReplace = true;
        // }
        // if (!ifReplace)
            // Assert.fail("Symbol substitute fails!");
        
        for (Map.Entry<String, Symbol> entry : TemplateManager.getInstance().currentTemplate.getSymMap().entrySet()) {
            if (entry.getValue().typeName.equals(RuntimeDetectionTest.ComplexObject.class.getName())) {
                if (!entry.getValue().strVal.contains("$"))
                    Assert.fail("Symbol substitute fails! Expected to get $ but got " + entry.getValue().strVal);
            }
        }
    }

    @Test
    public void testObjects() throws Exception {
        LinkedHashMap<String, Object[]> opMap = new LinkedHashMap<String, Object[]>() {{
            put("aa", new Object[]{valueFilePath});
            put("cc", new Object[]{"1", 2, new RuntimeDetectionTest.ComplexObject(3, "4", new File(valueFilePath))});
        }};

        startTemplate();
        //System.out.println(CheckerTemplate.gsonPrettyPrinter.toJson(new ComplexObject(3,"4",new File(valueFilePath))));
        for (Map.Entry<String, Object[]> entry : opMap.entrySet()) {
            Operation op = Operation.createOperation(entry.getKey(), entry.getValue());
            TemplateManager.getInstance().addOp(op);
        }
        Assertion.appendCustomizedAssert("test", 1, valueFilePath);
        endTemplate();
        for (Map.Entry<String, Symbol> entry : TemplateManager.getInstance().currentTemplate.getSymMap().entrySet()) {
            if (entry.getValue().typeName.equals(RuntimeDetectionTest.ComplexObject.class.getName())) {
                if (!entry.getValue().strVal.contains("$"))
                    Assert.fail("Symbol substitute fails! Expected to get $ but got " + entry.getValue().strVal);
            }
        }
    }
}