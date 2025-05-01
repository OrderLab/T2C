package edu.jhu.order.t2c.dynamicd.runtime;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import edu.jhu.order.t2c.dynamicd.util.ForcedException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.jhu.order.t2c.dynamicd.runtime.CheckerTemplate.gsonPrettyPrinter;

public class Operation {
    public interface OpType {
        //create("create"),
        //delete("delete"),
        //exists("exists"),
        //...

        boolean ifQueryOp();
    }

    public enum OpTypeBasicImpl implements OpType {
        //intermediate flags to mark a selected region,
        //they should not appear in the final template
        TESTBODYBEGIN("TESTBODYBEGIN", false),
        TESTBODYEND("TESTBODYEND", false),
        CUSTOMIZED("CUSTOMIZED", false),
        ASSERTFAIL("ASSERTFAIL", false),
        ILLEGAL("ILLEGAL", false);

        final String text;
        final boolean ifQueryOp;

        /**
         * @param text
         */
        OpTypeBasicImpl(final String text, final boolean ifQueryOp) {
            this.text = text;
            this.ifQueryOp = ifQueryOp;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
        }

        public boolean ifQueryOp()
        {
            return false;
        }

    }

    public static class WildcardOp extends Operation
    {
        public WildcardOp()
        {
            super("wild","");
        }
    }

    //Gson helper
    static Gson gson = new Gson();
    //should not use
    //static int customizedOpIDCounter = 0;

    //we avoid using OpType class here, since gson has trouble supporting interfaces
    String optypeStr;
    boolean ifQueryOp;
    String flag;
    JsonObject opTree;
    Map<String, Symbol> symbolMap;
    public List<Integer> relatedAssertions = new ArrayList<>();
    
    //transient Object retVal;
    transient String queryStr;

    //just for customized operation use!
    int customizedOpID = -1;
    String sourceTestClassName = "-";

    public Operation(OpType optype, String flag) {
        this.optypeStr = optype.toString();
        this.ifQueryOp = optype.ifQueryOp();
        this.flag = flag;
        this.queryStr = "";

        opTree = new JsonObject();
        symbolMap = new HashMap<>();
    }

    public Operation(String optypeStr, String flag) {
        this.optypeStr = optypeStr;
        this.ifQueryOp = false;
        this.flag = flag;
        this.queryStr = "";

        opTree = new JsonObject();
        symbolMap = new HashMap<>();
    }

    @Override
    public String toString() {
        return "Operation{" +
                "optypeStr=" + optypeStr +
                "}, opTree= "+ opTree.toString() +
                ", symbolMap= "+ gsonPrettyPrinter.toJson(symbolMap);
    }

    @Override
    public boolean equals(Object o) {
        if(this instanceof WildcardOp)
            return true;
        if(o instanceof WildcardOp)
            return true;

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Operation operation = (Operation) o;

        return optypeStr.equals(operation.optypeStr);
    }

    @Override
    public int hashCode() {
        return optypeStr.hashCode();
    }

    public Map<String, Symbol> getSymbolMap() {
        return symbolMap;
    }

    public JsonObject getOpTree() {
        return opTree;
    }

    public String getOptypeStr(){
        return optypeStr;
    }

    public static void appendOp(Operation op)
    {
        try{
            switch (GlobalState.mode)
            {
                case BUILD:
                    //System.out.println("******* In build phase, record an operation " + op.optypeStr + " for template " + TemplateManager.getInstance().currentTemplate.templateSource + " *******");
                    TemplateManager.getInstance().addOp(op);
                    break;
                case VALIDATION:
                case PRODUCTION:
                    //block until checker finishes, unless a checker is doing that
                    //in a situation: [    op1   ][   op2    ]
                    //                 [checker1]
                    // there is a possibility that checker1 finishes before op1 finishes, or too late that op2 start to make effect
                    //we will 1) delay op2 until checker1 finishes 2) delay checker1 a bit to start
                    //System.out.println("******* In validate phase, record an operation " + op.optypeStr + " for template " + TemplateManager.getInstance().currentTemplate.templateSource + " *******");
                    //System.out.println("#### GlobalState.ifAsserting is "+GlobalState.ifAsserting);
                    // if(GlobalState.testStarted == false){
                    //     System.out.println("#### skip operations when test has not started.");
                    //     return;
                    // }
                    // if(T2CHelper.getInstance().lastTestName.contains("cassandra") && GlobalState.ifAsserting){
                    //     //System.out.println("#### Skip adding op in assertion query for cassandra to avoid dead lock");
                    //     return;
                    // }
                    //System.out.println("#### waiting");
                    if(!Thread.currentThread().getName().equals(Assertion.CHECKER_THREAD_NAME))
                        while(GlobalState.ifAsserting);
		    //System.out.println("#### before addTrace "+op.optypeStr);
                    RuntimeTracer.getInstance().addTrace(op);
//                    System.out.println("#### after addTrace");
                    break;
                case ILLEGAL:
                    throw new RuntimeException("mode not set!");
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static Operation createOperation(OpType opType, Object[] args){
        Symbol.SymbolBuilder builder = new Symbol.SymbolBuilder(CheckerTemplate.symId, CheckerTemplate.cache);

        int counter = 0;
        JsonObject tree = new JsonObject();

        if(args!=null) {
            for(int i=0;i<args.length;++i)
            {
                Object val = null;
                if (args[i]!=null)
                {
                    val = args[i];
                    Class c = args[i].getClass();

                    tree.addProperty("$"+Integer.toString(counter), builder.getSymbol(val));
                    counter++;

                    while (c != null) {
                        for (Field field : c.getDeclaredFields()) {
                            if (field.isAnnotationPresent(MarkedOpSubfields.class)) {
                                field.setAccessible(true);
                                try {
                                    if(field.getType().isPrimitive())
                                    {
                                        if(field.getType().getName().equals("int"))
                                            val = Integer.toString(field.getInt(args[i]));
                                        else if(field.getType().getName().equals("bool"))
                                            val = Boolean.toString(field.getBoolean(args[i]));
                                        else if(field.getType().getName().equals("char"))
                                            val = Character.toString(field.getChar(args[i]));
                                        else if(field.getType().getName().equals("short"))
                                            val = Short.toString(field.getShort(args[i]));
                                        else if(field.getType().getName().equals("long"))
                                            val = Long.toString(field.getLong(args[i]));
                                        else if(field.getType().getName().equals("byte"))
                                            val = Byte.toString(field.getByte(args[i]));
                                        else if(field.getType().getName().equals("float"))
                                            val = Float.toString(field.getFloat(args[i]));
                                        else if(field.getType().getName().equals("double"))
                                            val = Double.toString(field.getDouble(args[i]));
                                    }
                                    else
                                        val = field.get(args[i]).toString();
                                    tree.addProperty("$" + Integer.toString(counter),
                                            builder.getSymbol(val));
                                }catch (IllegalAccessException ex)
                                {
                                    ex.printStackTrace();
                                }
                                counter++;
                            }
                        }
                        c = c.getSuperclass();
                    }
                }
                else
                {
                    tree.addProperty("$"+Integer.toString(counter), builder.getSymbol(val));
                    counter++;
                }
            }
        }

        Operation op = new Operation(opType, "");
        op.opTree = tree;
        op.symbolMap = builder.getSymbolMap();
        //op.retVal = retVal;
        return op;
    }

    public static Operation createOperation(String opTypeStr, Object... args){
        //System.out.println("#### normal operation "+opTypeStr);
        Symbol.SymbolBuilder builder = new Symbol.SymbolBuilder(CheckerTemplate.symId, CheckerTemplate.cache);
//        new Exception().printStackTrace(new PrintWriter(T2CHelper.getInstance().getProdlogInfoInternal()));

        int counter = 0;
        JsonObject tree = new JsonObject();

        if (args!=null){
            for(int i=0;i<args.length;++i)
            {
                Object val = null;
                if (args[i]!=null)
                {
                    val = args[i];
                    Class c = args[i].getClass();

                    tree.addProperty("$"+Integer.toString(counter), builder.getSymbol(val));
                    counter++;

                    while (c != null) {
                        for (Field field : c.getDeclaredFields()) {
                            if (field.isAnnotationPresent(MarkedOpSubfields.class)) {
                                field.setAccessible(true);
                                try {
                                    if(field.getType().isPrimitive())
                                    {
                                        if(field.getType().getName().equals("int"))
                                            val = Integer.toString(field.getInt(args[i]));
                                        else if(field.getType().getName().equals("bool"))
                                            val = Boolean.toString(field.getBoolean(args[i]));
                                        else if(field.getType().getName().equals("char"))
                                            val = Character.toString(field.getChar(args[i]));
                                        else if(field.getType().getName().equals("short"))
                                            val = Short.toString(field.getShort(args[i]));
                                        else if(field.getType().getName().equals("long"))
                                            val = Long.toString(field.getLong(args[i]));
                                        else if(field.getType().getName().equals("byte"))
                                            val = Byte.toString(field.getByte(args[i]));
                                        else if(field.getType().getName().equals("float"))
                                            val = Float.toString(field.getFloat(args[i]));
                                        else if(field.getType().getName().equals("double"))
                                            val = Double.toString(field.getDouble(args[i]));
                                    }
                                    else
                                        val = field.get(args[i]).toString();
                                    tree.addProperty("$" + Integer.toString(counter),
                                            builder.getSymbol(val));
                                }catch (IllegalAccessException ex)
                                {
                                    ex.printStackTrace();
                                }
                                counter++;
                            }
                        }
                        c = c.getSuperclass();
                    }
                }
                else
                {
                    tree.addProperty("$"+Integer.toString(counter), builder.getSymbol(val));
                    counter++;
                }
            }
        }

        Operation op = new Operation(opTypeStr, "");
        op.opTree = tree;
        //T2CHelper.globalLogInfo("#### optype is "+opTypeStr+", optree is "+tree.toString());
        //System.out.println("#### optype is "+opTypeStr+", optree is "+tree.toString());
        op.symbolMap = builder.getSymbolMap();
        //op.retVal = retVal;
        return op;
    }

    //this is for generating customized assertion
    public static Operation createCustomizedOperation(String testClassName, int id, Object... args) {
        //T2CHelper.prodLogInfo("#### in createCustomizedOperation");
        //System.out.println("#### in createCustomizedOperation");
        Symbol.SymbolBuilder builder = new Symbol.SymbolBuilder(CheckerTemplate.symId, CheckerTemplate.cache);

        JsonObject tree = new JsonObject();
        OpType opType = OpTypeBasicImpl.CUSTOMIZED;

        if (args!=null){
            for(int i=0;i<args.length;++i)
            {
                tree.addProperty("arg"+i, builder.getSymbol(args[i]));
            }
        }

        Operation op = new Operation(opType, "");
        op.opTree = tree;
        op.symbolMap = builder.getSymbolMap();
        op.customizedOpID = id;
        op.sourceTestClassName = testClassName;
        return op;
    }

    /**
     * for single elem, from symbolname to val
     * @param elem symbol name
     * @return symbol if found
     */
    public Symbol substitute(String elem) {
        //we only do replacing for symbols
        //if (!Symbol.isSymbol(elem))
        //    return elem;

        if(symbolMap == null)
            throw new RuntimeException("symbolMap is null!");
        if(elem == null)
            throw new RuntimeException("elem is null!");

        Symbol sym = symbolMap.get(elem);
        if (sym == null)
            throw new RuntimeException("Cannot find symbol in the map:" + elem);

        return sym;
    }

    //for all elems
    public void substitute(Map<String, Symbol> symbolMap) {
        //we merge the old map with new map instead of completly washing original ones
        this.symbolMap.putAll(symbolMap);
    }

    //for the symbol alias that points to the new symbol, actually we can just simply remove them all
    public void removeAlias(String oldSym, String newSym) {
        removeAlias(opTree, oldSym, newSym);
        removeAlias(symbolMap, oldSym, newSym);
    }

    public static void removeAlias(Map<String, Symbol> map, String oldSym, String newSym) {
        List<String> lst = new ArrayList<>();
        for (String alias : map.keySet()) {
            if (alias.equals(oldSym))
                lst.add(alias);

        }
        for (String alias : lst)
            map.remove(alias);
    }

    private void removeAlias(JsonElement elem, String oldSym, String newSym) {

        if (elem.isJsonArray()) {
            JsonArray array = (JsonArray) elem;
            for (int i = 0; i < array.size(); ++i) {
                JsonElement subelem = array.get(i);
                if (subelem.isJsonPrimitive()) {
                    JsonPrimitive prim = (JsonPrimitive) subelem;
                    if (prim.isString()) {
                        if (prim.getAsString().equals(oldSym)) {
                            array.set(i, new JsonPrimitive(newSym));
                        }
                    }
                } else
                    removeAlias(subelem, oldSym, newSym);
            }
        } else if (elem.isJsonObject()) {
            JsonObject obj = (JsonObject) elem;
            //this is a weird compilation error, when we are using obj1.keySet(),
            // it would report symbol not found (though we can see the method in the class file),
            // so we have to re-write this in an alternative way
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement subelem = obj.get(key);
                if (subelem.isJsonPrimitive()) {
                    JsonPrimitive prim = (JsonPrimitive) subelem;
                    if (prim.isString()) {
                        if (prim.getAsString().equals(oldSym)) {
                            obj.addProperty(key, newSym);
                        }
                    }
                } else
                    removeAlias(subelem, oldSym, newSym);
            }
        }

    }

    //the algorithm is quite simple here, just need to handle multiple scenarios
    //essentially we are traversing both trees and look for inconsistencies,
    //if any we abort(by returning null), otherwise we would return the mapping relations
    private boolean compareJsonElem(JsonElement elem1, JsonElement elem2,
                                                Map<String, Symbol> map1, Map<String, Symbol> map2, Map<String, Symbol> retMap) {

        if(elem1==null && elem2==null) return false;
        if(elem1==null ^ elem2==null) return false;

        //System.err.println(elem1.toString());
        //System.err.println(elem2.toString());

        if (elem1.isJsonObject() ^ elem2.isJsonObject()) return false;
        if (elem1.isJsonArray() ^ elem2.isJsonArray()) return false;
        if (elem1.isJsonPrimitive() ^ elem2.isJsonPrimitive()) return false;
        if (elem1.isJsonNull() ^ elem2.isJsonNull()) return false;

        if (elem1.isJsonArray()) {
            JsonArray array1 = (JsonArray) elem1;
            JsonArray array2 = (JsonArray) elem2;

            if (array1.size() != array2.size()) return false;
            for (int i = 0; i < array1.size(); ++i) {
                JsonElement subelem1 = array1.get(i);
                JsonElement subelem2 = array2.get(i);
                if(!compareJsonElem(subelem1, subelem2, map1, map2, retMap)){
                    return false;
                }
            }
        } else if (elem1.isJsonObject()) {
            JsonObject obj1 = (JsonObject) elem1;
            JsonObject obj2 = (JsonObject) elem2;
            //this is a weird compilation error, when we are using obj1.keySet(),
            // it would report symbol not found (though we can see the method in the class file),
            // so we have to re-write this in an alternative way
            for (Map.Entry<String, JsonElement> entry : obj1.entrySet()) {
                String key = entry.getKey();
                JsonElement subelem1 = obj1.get(key);
                JsonElement subelem2 = obj2.get(key);
                if(!compareJsonElem(subelem1, subelem2, map1, map2, retMap)){
                    return false;
                }
            }
        } else if (elem1.isJsonPrimitive()) {
            JsonPrimitive prim1 = (JsonPrimitive) elem1;
            JsonPrimitive prim2 = (JsonPrimitive) elem2;
            if (prim1.isString() ^ prim2.isString()) return false;
            if (prim1.isBoolean() ^ prim2.isBoolean()) return false;
            if (prim1.isNumber() ^ prim2.isNumber()) return false;
            if (prim1.isString()) {
                if (Symbol.isSymbol(prim1.getAsString())) {
                    retMap.put(prim1.getAsString(),map2.get(prim2.getAsString()));
                }
                else
                    if(!prim1.getAsString().equals(prim2.getAsString())) return false;

            } else if (prim1.isBoolean()) {
                if(prim1.getAsBoolean()!=prim2.getAsBoolean()) return false;
            } else if (prim1.isNumber()) {
                if(prim1.getAsNumber().equals(prim2.getAsNumber())) return false;
            }
        }

        return true;
    }

    //this function traverse the opTree and compare workloads,
    // it should return mapping from original operation's symbol name to trace values
    static Map<String, Symbol> retMap = new HashMap<>();
    synchronized Map<String, Symbol> compare(Operation trace) {
        retMap.clear();
        if(!compareJsonElem(opTree, trace.opTree, symbolMap, trace.symbolMap, retMap)){
            return null;
        }
        return retMap;
    }

    void execute() throws Exception {
        if(optypeStr.equals("FAIL_ASSERT_OP"))
        {
            new FailOperation().execute();
            return;
        }

        //for debugging purpose, sometimes we encounter nullptr ex inside assertion but don't know why
        //SysWorkloadPool.verifyPool();

        boolean doInvoke = true;
        Method assertionMethod = null;
        Class<?> c = Class.forName(sourceTestClassName);
        //Class<?> c = Class.forName("org.apache.hadoop.hdfs.t2c.SampleTest");
        String targetMethodName = "externalAssertFunc"+customizedOpID;
        //T2CHelper.prodLogInfo("#### target method name is "+targetMethodName);
        for(Method method: c.getDeclaredMethods()) {
            if (method.getName().equals(targetMethodName)) {
                assertionMethod = method;
            }
        }

        if(assertionMethod==null) {
            throw new RuntimeException("Cannot find assertion func " + targetMethodName +" in "+ sourceTestClassName);
        }
        Class[] argTypeLst = assertionMethod.getParameterTypes();

        try{
            int size = opTree.entrySet().size();
            Object[] params = new Object[size];
            for(int i=0;i<size;++i)
            {
                //T2CHelper.prodLogInfo("#### No."+i+"/"+size);
                String argName = opTree.get("arg"+i).getAsString();
                Symbol sym = substitute(argName);

                //String typeName = argTypeLst[i].getName();
                String typeName = sym.typeName;

                //do a type check
                if(!typeName.equals(argTypeLst[i].getName()))
                {
                    T2CHelper.prodLogInfo("ERROR: " + typeName +" mismatch with "+ argTypeLst[i].getName());
                }

                //if(sym.sysWorkloadKey!=null)
                if(SysWorkloadPool.isSysWorkload(typeName))
                {
                    //we retrieve corresponding sysworkloads here
                    //params[i] = SysWorkloadPool.getSysWorkload(sym.sysWorkloadKey);
                    if (typeName.contains("cassandra")){
                        params[i] = SysWorkloadPool.getSysWorkload(typeName, customizedOpID, queryStr);
                    } else {
                        params[i] = SysWorkloadPool.getSysWorkload(typeName);
                    }
                }
                else
                {
                    String val;
                    if(sym.obj==null)
                    {
                        val = sym.strVal;
                        LocalDateTime now = LocalDateTime.now();
                        //T2CHelper.prodLogInfo("#### No."+i+"/"+size+" sym's typeName is "+typeName+", strVal is "+val);
                        //System.out.println("#### "+now.toString()+" No."+i+" sym's typeName is "+typeName+", strVal is "+val);
                        if(typeName.equals("java.lang.String") || typeName.equals("java.lang.StringBuilder")){
                            //T2CHelper.prodLogInfo("#### No."+i+" sym type is string, val is "+val);
                            //if(val.equals("\"foobar\"")||val.equals("\"Command index out of range\"")||val.equals("\"create\"")||val.equals("\u0027quoted data\u0027")||val.equals("\"test\"")){
                            if(val.startsWith("\"") && val.endsWith("\"")){
                                String value = val.substring(1, val.length() - 1);
                                if(value.equals("\u0027quoted data\u0027")){
                                    params[i] = "'quoted data'";
                                } else {
                                    params[i] = value;
                                }
                                //T2CHelper.prodLogInfo("#### now params["+i+"] is "+params[i]);
                            } else{
                                params[i] = val;
                            }
                        }
                        else if( (typeName.equals("java.lang.Integer"))){
                            params[i] = Integer.parseInt(val);
                            //T2CHelper.prodLogInfo("#### No."+i+" sym is Integer, value is "+params[i]);
                            // ZK-4362 MANUALLY CHANGE THE TEMPLATE THAT MARKS THE NUMSNAPS VARIABLE TO -1
                            if (customizedOpID==1887 && Integer.parseInt(val)==-1){
                                T2CHelper.prodLogInfo("DIMAS: numSnaps");
                                params[i] = SysWorkloadPool.getSysWorkload("numSnaps");
                            }
                        }
                        else if( (typeName.equals("java.lang.Long")))
                            params[i] = Long.parseLong(val);
                        else if( (typeName.equals("java.lang.Boolean")))
                            params[i] = Boolean.parseBoolean(val);
                        else if( (typeName.equals("java.lang.Byte")))
                            params[i] = Byte.parseByte(val);
                        else if( (typeName.equals("java.lang.Character")))
                            params[i] = val.charAt(0);
                        else if( (typeName.equals("java.lang.Short")))
                            params[i] = Short.parseShort(val);
                        else if( (typeName.equals("java.lang.Double")))
                            params[i] = Double.parseDouble(val);
                        else if( (typeName.equals("java.lang.Float")))
                            params[i] = Float.parseFloat(val);
                        else
                        {
                            try {
                                //T2CHelper.prodLogInfo("#### before forName, typeName is "+typeName);
                                Class<?> clazz = Class.forName(typeName);
                                if(!sym.subSymbols.isEmpty())
                                {
                                    for(String subSym: sym.subSymbols)
                                    {
                                        String oldStr = symbolMap.get(subSym).strVal, newStr=oldStr;
                                        //T2CHelper.prodLogInfo("#### oldStr and newStr: "+newStr);
                                        if (oldStr.startsWith("\"") && oldStr.endsWith("\"")) {
                                            newStr = oldStr.substring(1, oldStr.length() - 1);
                                            //T2CHelper.prodLogInfo("#### after truncate, newStr: "+newStr);
                                        }
                                        sym.strVal = sym.strVal.replace(subSym,newStr);
                                        //T2CHelper.prodLogInfo("#### sym.strVal: "+sym.strVal);
                                    }
                                }
                                //System.out.println("#### starts to deserialize typeName: "+typeName+", strVal is "+sym.strVal);
                                //T2CHelper.prodLogInfo("#### starts to deserialize typeName: "+typeName+", strVal is "+sym.strVal);
                                try(JsonReader reader = new JsonReader(new StringReader(sym.strVal))){
                                    params[i] = gsonPrettyPrinter.fromJson(reader, clazz);
                                }
                                //T2CHelper.prodLogInfo("#### deserialization finished. typeName: "+typeName+", strVal is "+sym.strVal);
                            } 
                            catch (ClassNotFoundException | RuntimeException e) {
                                //System.err.println("param deserialization error, use clazz.getDeclaredConstructor().newInstance() instead of throwing out");
                                //System.err.println(e.getMessage());
                                // try {
                                //     Class<?> clazz = Class.forName(typeName);
                                //     Constructor<?> constructor = clazz.getDeclaredConstructor();
                                //     constructor.setAccessible(true);
                                //     params[i] = constructor.newInstance();
                                // } catch (Exception ex){
                                //     throw ex;
                                // }
                                throw e;
                            }
                            //String err= "WARN: param not supported! "+ typeName+ val;
                            //T2CHelper.prodLogInfo(err);
                            //System.out.println(err);
                            //params[i] = val;
                        }
                    }
                    else
                        //use substituted value
                        params[i] = sym.obj;
                }

                // HDFS-16942
                //if (customizedOpID == 4919) {
                //    if(typeName.equals("org.apache.hadoop.hdfs.server.protocol.FinalizeCommand")){
                //        int traceSize = RuntimeTracer.getInstance().historyTraces.size();
                //        for (int j = 0; j < traceSize; ++j) {
                //            Operation trace = RuntimeTracer.getInstance().historyTraces.get(j);
                //            if (trace.optypeStr.equals("blockReport")) {
                //                params[i] = trace.retVal;
                //            }
                //        }
                //    }
                //}

                //null check
                if(params[i]==null)
                {
                    if(!typeName.contains("Test")){
                        doInvoke = false;
                        String err="WARN: params is null!"+sym.toString();
                        T2CHelper.prodLogInfo(err);
                        System.out.println(err);
                    }
                }

                //for debugging
                //T2CHelper.prodLogInfo("params"+i+ ": " +params[i].toString());
            }

            if(doInvoke) {
                //T2CHelper.prodLogInfo("#### invoke assertion method");
                assertionMethod.invoke(null, params);
                if(customizedOpID==779){ //zk timeout450
                    throw new Exception("Should be null");
                }
                T2CHelper.prodLogInfo("Assertion#" + customizedOpID + " passed!");
            }
        } catch (Throwable ex){
            if (customizedOpID == 779 && !ex.getMessage().equals("Should be null") && GlobalState.mode.equals(GlobalState.T2CMode.VALIDATION)) { // zk timeout450
                return;
            }
            //assertion has correctness issues
            if(ex.getCause() instanceof AssertionError)
            {
                T2CHelper.prodLogInfo("Assertion#"+customizedOpID+ " failed! Cause: AssertionError");
                T2CHelper.prodLogInfo(ex.getCause().getMessage());
            }
            //usually type mismatch
            else if(ex.getCause() instanceof IllegalArgumentException){
                T2CHelper.prodLogInfo("Assertion#"+customizedOpID+ " failed! Cause: IllegalArgumentException");
                T2CHelper.prodLogInfo(ex.getCause().getMessage());
            }
            //usually context not ready
            else if(ex.getCause() instanceof NullPointerException){
                T2CHelper.prodLogInfo("Assertion#"+customizedOpID+ " failed! Cause: NullPointerException");
                T2CHelper.prodLogInfo(ex.getCause().getMessage());
            }
            else if(ex.getCause() instanceof ClassNotFoundException || ex.getCause() instanceof NoClassDefFoundError || ex.getCause() instanceof VerifyError || ex.getCause() instanceof RuntimeException){
                T2CHelper.prodLogInfo("Assertion#"+customizedOpID+ " failed! Cause: Abnormal Error");
                T2CHelper.prodLogInfo(ex.getCause().getMessage());
            }
            else
            {
                T2CHelper.prodLogInfo("Assertion#"+customizedOpID+ " failed! Cause: Other");
                T2CHelper.prodLogInfo(ex.getMessage());
            }
            throw ex;
        }
    }

}
