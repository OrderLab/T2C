package edu.jhu.order.t2c.dynamicd.runtime;

import java.lang.reflect.Method;
import java.util.*;

import javassist.*;
import javassist.bytecode.InstructionPrinter;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class OperationRegister {

    static Map<String, String> primitiveConversionMap = new HashMap<>();

    static
    {
        primitiveConversionMap.put(boolean.class.getName(), Boolean.class.getName());
        primitiveConversionMap.put(byte.class.getName(), Byte.class.getName());
        primitiveConversionMap.put(short.class.getName(), Short.class.getName());
        primitiveConversionMap.put(char.class.getName(), Character.class.getName());
        primitiveConversionMap.put(int.class.getName(), Integer.class.getName());
        primitiveConversionMap.put(long.class.getName(), Long.class.getName());
        primitiveConversionMap.put(float.class.getName(), Float.class.getName());
        primitiveConversionMap.put(double.class.getName(), Double.class.getName());
    }

    //use javassist to register all operation hooks into
    public static void registerAll(Map<String, Set<String>> instMap)
    {
        System.out.println("registering all annotated methods");

        for(String className: instMap.keySet())
        {
            try{
                System.out.println("######## classname: "+className);
                ClassPool pool = ClassPool.getDefault();
                CtClass cc = pool.get(className);
                //if(!instMap.get(className).isEmpty() && instMap.get(className).contains(".")){
                 //   String[] parts = method.split(".");
                   // String subClassName = parts[0];
                  //  String methodName = parts[1]; 
                  //  CtClass[] subclasses = cc.getSubclasses();
                  //  for(CtClass subc : subclasses){
                  //      if(subc.getName().contains(subClassName)){
                  //          for(CtMethod subMethod : subc.getMethods()){
                  //              if(subMethod.getName().equals(methodName)){
                  //                  m = 
                  //              }
                  //          }
                  //      }
                  //  }
               // }
                for(CtMethod m: cc.getMethods())
                {
//                    System.out.println("#### method name is "+m.getName());
                    //Skip this method because it is not specified
                    if (!instMap.get(className).isEmpty() && !instMap.get(className).contains(m.getName())) {
                        continue;
                    }
                    //avoid method such as access$900
                    if(m.getName().contains("access$")||m.getName().contains("hasProperty"))
                        continue;

                    //for hbase, avoid getter style
                    if(m.getName().startsWith("get") || m.getName().startsWith("check") || m.getName().startsWith("is"))
                        continue;

                    //for hbase, avoid noisy operations, should move to config later
                    //the list is from running ± % ack "optypeStr" --no-filename | sort | uniq -c | sort -nr  in templates_in
                    //hbase
                    if(m.getName().equals("setCompleteSequenceId") || m.getName().equals("rowIsInRange")
                            || m.getName().startsWith("rewriteCellTags") || m.getName().startsWith("incMemStoreSize")
                            || m.getName().startsWith("shouldFlush") || m.getName().startsWith("toString")
                            || m.getName().startsWith("hashCode")
                        //hdfs
                            || m.getName().startsWith("shouldRun") || m.getName().startsWith("areIBRDisabledForTests")
                            || m.getName().startsWith("areCacheReportsDisabledForTests") || m.getName().startsWith("areHeartbeatsDisabledForTests")
                            || m.getName().startsWith("sendHeartbeat")
                    )
                        continue;

                    if (Modifier.isNative(m.getModifiers()) || Modifier.isAbstract(m.getModifiers()))
                        continue;

		    //force to register all methods in the interface class
		    //FIXME: this is a temporary solution for HBase, for other systems, it needs to be rolled back!
                    // Uncomment below for ZK-4362 or CS
                    //if(instMap.get(className).contains(m.getName()) || m.hasAnnotation(MarkedOpFunc.class))
                    {
                        List<String> lst = new ArrayList<>();
                        for(int i=0;i<m.getParameterTypes().length;++i)
                        {
                            lst.add(boxPrimitive("$"+(i+1),m.getParameterTypes()[i].getName()));
                        }
                        String arrayStr = String.join(",", lst);
                        String finalStr = "";
                        if(lst.isEmpty())
                        {
                            arrayStr = "null";
                        }
                        //if(m.getName().equals("parseStatement"))
                        //    continue;
                        finalStr += "{Object[] argArray = {"+arrayStr+"};;";
                        finalStr+= "edu.jhu.order.t2c.dynamicd.runtime.Operation.appendOp("
                                + "edu.jhu.order.t2c.dynamicd.runtime.Operation.createOperation("
                                //abandon OpTypeSysImpl class
                                //+ OperationProcessor.OpTypeSysImplName+"."
                                //+"\""+((MarkedOpFunc)m.getAnnotation(MarkedOpFunc.class)).value()+"\""
                                +"\""+m.getName()+"\"";
                        //finalStr += ", $_";
                        finalStr += ",argArray));}";
                        //if(className.contains("cassandra")){
                         //   finalStr = "if(edu.jhu.order.t2c.dynamicd.runtime.GlobalState.ifAsserting);else"+finalStr;
                        //}
                        System.out.println(finalStr);
            		try{
                        	m.insertBefore(finalStr);
                        } catch (Exception ex)
                        {
                            System.err.println(ex);
                            ex.printStackTrace();
                        }
                    }
                    //bytecode debugging printf, not so human friendly
                    //InstructionPrinter.print(m,System.out);
                }
                cc.toClass();
                //System.out.println("#### toClass success!");
            } catch (Exception ex)
            {
                System.err.println(ex);
                ex.printStackTrace();
            }
        }
    }

    //use javassist to register all operation hooks into
    public static void registerSystemConfigConstraints(String className)
    {
        System.out.println("registering system config constraints");

        {
            try{
                ClassPool pool = ClassPool.getDefault();
                CtClass cc = pool.get(className);
                for(CtMethod m: cc.getMethods())
                {
                    if(m.hasAnnotation("org.junit.Before"))
                    {
                        m.instrument(
                                new ExprEditor() {
                                    public void edit(MethodCall mc)
                                            throws CannotCompileException
                                    {
                                        System.out.println(mc.getClassName() + "." + mc.getMethodName() + " " + mc.getSignature());
                                        if (mc.getClassName().equals("java.lang.System")
                                                && mc.getMethodName().equals("setProperty"))
                                            mc.replace("{ "+SysConfConstraint.class.getCanonicalName()+".addSysConfConstraint"+"($$); $_ = $proceed($$); }");
                                    }
                                });
                    }
                    //bytecode debugging printf, not so human friendly
                    //InstructionPrinter.print(m,System.out);
                }
                //don't freeze, we would do in edu.jhu.order.t2c.dynamicd.runtime.OperationRegister.registerAll
                //cc.toClass();
            } catch (Exception ex)
            {
                System.err.println(ex);
                ex.printStackTrace();
            }
        }
    }

    static String tempClassName = "";
    static String tempMethodName = "";
    static String lastClassName = "";
    static String lastMethodName = "";
    //some tests depend on asserting on unreachable points to define expected bahaviors
    public static void registerAssertFail(String className) {
        if(true)
            return;

        System.out.println("registering assertfail");
        String operationClassName = Operation.class.getName();
        {
            try{
                ClassPool pool = ClassPool.getDefault();
                CtClass cc = pool.get(className);
                for(CtMethod m: cc.getMethods())
                {
                    {
                        String finalStr = "edu.jhu.order.t2c.dynamicd.runtime.Operation.appendOp(new " +
                            operationClassName + "(" + operationClassName
                                + ".OpTypeBasicImpl.ASSERTFAIL,\"\"))";
                        m.instrument(
                                new ExprEditor() {
                                    public void edit(MethodCall mc)
                                            throws CannotCompileException
                                    {

                                        if ((mc.getClassName().equals("junit.framework.Assert") ||
                                                mc.getClassName().equals("org.junit.Assert"))
                                                && mc.getMethodName().equals("fail"))
                                        {
                                            lastClassName = tempClassName;
                                            lastMethodName = tempMethodName;
                                            System.out.println("record lastClassName:"+lastClassName+" lastMethodName:"+lastMethodName);
                                        }
                                        tempClassName = mc.getClassName();
                                        tempMethodName = mc.getMethodName();
                                    }
                                });
                        m.instrument(
                                new ExprEditor() {
                                    public void edit(MethodCall mc)
                                            throws CannotCompileException
                                    {
                                        String mcClassName = mc.getClassName();
                                        String mcMethodName = mc.getMethodName();
                                        String mcSignature = mc.getSignature();
                                        if (mcClassName.equals(lastClassName)
                                                && mcMethodName.equals(lastMethodName))
                                        {
                                            mc.replace("{ "+finalStr+"; $_ = $proceed($$); }");
                                            System.out.println("inject at "+mcClassName + "." + mcMethodName + " " + mcSignature);
                                        }
                                    }
                                });;
                    }
                    //bytecode debugging printf, not so human friendly
                    //InstructionPrinter.print(m,System.out);
                }
                //don't freeze, we would do in edu.jhu.order.t2c.dynamicd.runtime.OperationRegister.registerAll
                //cc.toClass();
            } catch (Exception ex)
            {
                System.err.println(ex);
                ex.printStackTrace();
            }
        }
    }

    private static String boxPrimitive(String originName, String typeName)
    {
        if(primitiveConversionMap.containsKey(typeName))
        {
            return "new "+primitiveConversionMap.get(typeName)+"("+originName+")";
        }

        return originName;
    }
}
