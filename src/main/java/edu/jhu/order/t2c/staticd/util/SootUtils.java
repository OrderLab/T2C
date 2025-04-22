/*
 *  @author Ryan Huang <huang@cs.jhu.edu>
 *
 *  The AutoWatchdog Project  
 *
 *  Copyright (c) 2018, Johns Hopkins University - Order Lab.
 *      All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.jhu.order.t2c.staticd.util;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.baf.BafASMBackend;
import soot.jimple.*;
import soot.options.Options;
import soot.tagkit.*;
import soot.util.Chain;
import soot.util.JasminOutputStream;
import soot.validation.ValidationException;

/**
 * A collection of helper functions for using Soot
 */
public class SootUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SootUtils.class);

    /**
     * Get line number of a Soot unit
     */
    public static int getLine(Unit unit) {
        int line = -1;
        LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
        if (tag != null) {
            line = tag.getLineNumber();
        }
        return line;
    }

    /**
     * Add a new phase into a phase pack in Soot
     *
     * @return the new phase added
     */
    public static Transform addNewTransform(String phasePackName, String phaseName,
            Transformer transformer) {
        Transform phase = new Transform(phaseName, transformer);
        phase.setDeclaredOptions("enabled");
        phase.setDefaultOptions("enabled:false");
        PackManager.v().getPack(phasePackName).add(phase);
        return phase;
    }

    /**
     * check if a type is primtype (int, bool..) or string
     */
    public static boolean ifPrimJavaType(Type type) {
        return type.equals(ShortType.v()) || type.equals(ByteType.v()) ||
                type.equals(BooleanType.v()) || type.equals(CharType.v()) ||
                type.equals(IntType.v()) || type.equals(LongType.v()) ||
                type.equals(FloatType.v()) || type.equals(DoubleType.v()) ||
                type.equals(NullType.v());
    }

    public static boolean ifCollectionJavaType(Type type) {

        if (
                SootUtils.ifTypeImplementInterface(type, "java.util.Set")
                        ||
                        SootUtils.ifTypeImplementInterface(type, "java.util.List")
                        ||
                        SootUtils.ifTypeImplementInterface(type, "java.util.Map")) {
            return true;
        }

        return false;
    }

    public static boolean ifArrayJavaType(Type type) {
        if (type.getArrayType() != null) {
            return true;
        }

        return false;
    }

    /**
     * check if type implements a given interface
     */
    public static boolean ifTypeImplementInterface(Type type, String interfaceName) {
        SootClass c = Scene.v().loadClassAndSupport(type.toString());
        if (type.toString().equals(interfaceName)
                || c.implementsInterface(interfaceName)) {
            return true;
        }

        if (c.isInterface()) {
            for (SootClass z : Scene.v().getActiveHierarchy().getSuperinterfacesOf(c)) {
                if (z.getName().equals(interfaceName)) {
                    return true;
                }
            }
        } else {
            SootClass it = c;
            while (it.hasSuperclass()) {
                it = it.getSuperclass();
                if (it.implementsInterface(interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * get the base from invoker expr, e.g. base.doIO() -> base
     *
     * @param stmt invokerstmt
     * @return base
     */
    public static Value getBaseFromInvokerExpr(Stmt stmt) {
        Value base = null;
        if (stmt.getInvokeExpr() instanceof VirtualInvokeExpr) {
            base = ((VirtualInvokeExpr) stmt.getInvokeExpr()).getBase();
        } else if (stmt.getInvokeExpr() instanceof SpecialInvokeExpr) {
            base = ((SpecialInvokeExpr) stmt.getInvokeExpr()).getBase();
        } else if (stmt.getInvokeExpr() instanceof InterfaceInvokeExpr) {
            base = ((InterfaceInvokeExpr) stmt.getInvokeExpr()).getBase();
        } else {
            // happens for staticinvoke

            //throw new RuntimeException(
            //        "InvokeExpr type not supported\n stmt:" + stmt);
        }
        return base;
    }

    /**
     * a helper function to generate printf stmt for debugging purpose
     *
     * @param units target method body
     * @param message string to dump
     * @return stmt to insert
     */
    public static Stmt getPrintfStmt(Chain units, String message) {
        SootClass driverClass = Scene.v().loadClassAndSupport(Constants.DRIVER_NAME);
        SootMethod printMethod = driverClass
                .getMethod("logInfo", new ArrayList<Type>() {{
                    add(RefType.v("java.lang.String"));
                }});

        Value dummyMessage = StringConstant.v(message);
        return Jimple.v().newInvokeStmt(
                Jimple.v().newStaticInvokeExpr(printMethod.makeRef(), dummyMessage));
    }

    /**
     * get method signature (class name + method name)
     */
    public static String getMethodSignature(SootMethod method) {
        return method.getDeclaringClass().getName() + "@" + method.getNumberedSubSignature()
                .toString();
    }

    /**
     * get sub classes of a class
     *
     * @return list of sub class
     */
    public static List<SootClass> getSubClass(SootClass superClass) {
        List<SootClass> lst = new ArrayList<>();
        for (SootClass c : Scene.v().getApplicationClasses()) {
            SootClass it = c;
            if (it.hasSuperclass()) {
                if (it.getSuperclass().getName().equals(superClass.getName())) {
                    lst.add(c);
                }
            }
        }

        return lst;
    }

    public static List<SootClass> getImpls(SootClass interfaceClass) {
        List<SootClass> lst = new ArrayList<>();
        for (SootClass c : Scene.v().getApplicationClasses()) {
            SootClass it = c;
            if (it.implementsInterface(interfaceClass.getName())) {
                lst.add(c);
            }
        }

        return lst;
    }

    private static void outputSootClassJimple(SootClass sootClass, PrintStream out) {

        out.println("public class " + sootClass.toString());
        out.println("{");
        for (SootField f : sootClass.getFields()) {
            out.println("    " + f.getDeclaration().toString());
        }
        out.println("");
        for (SootMethod m : sootClass.getMethods()) {
            if (m.hasActiveBody()) {
                out.println("    " + m.getActiveBody().toString());
            } else {
                out.println("    " + m.toString() + " [no active body found]");
            }
        }
        out.println("}");
    }

    public static void printBodyJimple(Body body) {
        System.out.println(body.toString());
    }


    public static void printSootClassJimple(SootClass sootClass) {
        outputSootClassJimple(sootClass, System.out);
    }

    public static void dumpSootClassJimple(SootClass sootClass, String... fileNameSuffix) {
        PrintStream out;
        String fileName = SourceLocator.v().getFileNameFor(sootClass, Options.output_format_jimple);
        if(fileNameSuffix.length>0)
            fileName += "."+fileNameSuffix[0];
        File file = new File(fileName);
        file.getParentFile().mkdirs();
        try {
            LOG.info("Writing class " + sootClass.getName() + " to " + fileName);
            out = new PrintStream(file);
            outputSootClassJimple(sootClass, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void dumpBodyJimple(Body body, String... fileNameSuffix) {
        String fileName =
                body.getMethod().getDeclaringClass().getName() + "_" + body.getMethod().getName();
        if(fileNameSuffix.length>0)
            fileName += "."+fileNameSuffix[0];
        fileName += ".jimple";
        LOG.info(
                "Writing method " + body.getMethod().getName() + " to " + SourceLocator.v()
                        .getOutputDir() + "/" + fileName);
        File file = new File(SourceLocator.v().getOutputDir(), fileName);
        file.getParentFile().mkdirs();
        try {
            PrintStream out = new PrintStream(file);
            out.println(body.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static boolean dumpSootClass(SootClass sClass) {
        // Since it is .class we are generating, we must validate its integrity before dumping.
        sClass.validate();
        String fileName = SourceLocator.v().getFileNameFor(sClass, Options.output_format_class);
        LOG.info("Writing class " + sClass.getName() + " to " + fileName);
        File file = new File(fileName);
        file.getParentFile().mkdirs();
        try {
            OutputStream streamOut = new FileOutputStream(file);
            if (Options.v().jasmin_backend()) {
                streamOut = new JasminOutputStream(streamOut);
            }
            PrintWriter writerOut = new PrintWriter(
                    new OutputStreamWriter(streamOut));
            if (!Options.v().jasmin_backend()) {
                new BafASMBackend(sClass, Options.v().java_version()).generateClassFile(streamOut);
            } else {
                JasminClass jasminClass = new soot.jimple.JasminClass(sClass);
                jasminClass.print(writerOut);
            }
            writerOut.flush();
            streamOut.close();
            writerOut.close();

            return true;
        } catch (Exception e) {
            LOG.error("Exception when writing class "+ sClass.getName());
            e.printStackTrace();

            //due to Soot's some internal bad usages of exception msg recording,
            //sometimes we need to print more here
            if(e instanceof ValidationException)
                LOG.info("AdditionalExceptionMsg: "+((ValidationException)e).getConcerned()+" "+((ValidationException)e).getRawMessage());

            try {
                boolean result = Files.deleteIfExists(file.toPath());
            }catch (IOException ex){
                //try your best to clean, if not an empty class will replace target class
                LOG.error("Exception when cleaning class "+ sClass.getName());
            }

            return false;
        }
    }

    public static void tryDumpSootClass(SootClass sClass) {
        try {
            SootUtils.dumpSootClass(sClass);
        } catch (Exception e) {
            e.printStackTrace();
            SootUtils.printSootClassJimple(sClass);
            System.exit(-1);
        }
    }

    public static void SecureExportSootClass(SootClass sClass) {
        //SootUtils.printSootClassJimple(contextClass);
        SootUtils.dumpSootClassJimple(sClass);
        sClass.validate();
        SootUtils.tryDumpSootClass(sClass);
    }

    public static boolean hasTestAnnotation(SootMethod method) {
        boolean hasTestAnnotation = false;
        VisibilityAnnotationTag tag = (VisibilityAnnotationTag) method.getTag("VisibilityAnnotationTag");
        if (tag != null) {
            for (AnnotationTag annotation : tag.getAnnotations()) {
                System.out.println(annotation.getType());
                if (annotation.getType().equals("Lorg/junit/Test;")) {
                    hasTestAnnotation = true;
                    break;
                }
            }
        }
        return hasTestAnnotation;
    }

    public static Stmt findFirstNonIdentityStmt(Body body) {
        for(Unit unit:body.getUnits())
        {
            if(!(unit instanceof IdentityStmt))
            {
                LOG.debug("findFirstNonIdentityStmt "+unit.toString());
                return (Stmt) unit;
            }
        }
        throw new RuntimeException("cannot find non-IdentityStmt");
    }

    public static List<IdentityStmt> findAllIdentityStmts(Body body) {
        List<IdentityStmt> ret = new ArrayList<>();
        for(Unit unit:body.getUnits())
        {
            if((unit instanceof IdentityStmt))
            {
                ret.add((IdentityStmt)unit);
            }
        }
        return ret;
    }

    public static Type boxBasicJavaType(Type type) {
        if (type.equals(BooleanType.v())) {
            return RefType.v("java.lang.Boolean");
        } else if (type.equals(ByteType.v())) {
            return RefType.v("java.lang.Byte");
        } else if (type.equals(CharType.v())) {
            return RefType.v("java.lang.Character");
        } else if (type.equals(FloatType.v())) {
            return RefType.v("java.lang.Float");
        } else if (type.equals(IntType.v())) {
            return RefType.v("java.lang.Integer");
        } else if (type.equals(LongType.v())) {
            return RefType.v("java.lang.Long");
        } else if (type.equals(ShortType.v())) {
            return RefType.v("java.lang.Short");
        } else if (type.equals(DoubleType.v())) {
            return RefType.v("java.lang.Double");
        } else if (type.equals(NullType.v())) {
            return RefType.v("java.lang.Object");
        } else {
            //throw new RuntimeException("New type not supported:" + type.toString());
            return type;
        }
    }

    private static String unboxJavaType(RefType boxedType)
    {
        String primTypeName = null;
        if (boxedType.getClassName().equals("java.lang.Boolean")) {
            primTypeName = "boolean";
        } else if (boxedType.getClassName().equals("java.lang.Byte")) {
            primTypeName = "byte";
        } else if (boxedType.getClassName().equals("java.lang.Character")) {
            primTypeName = "char";
        } else if (boxedType.getClassName().equals("java.lang.Float")) {
            primTypeName = "float";
        } else if (boxedType.getClassName().equals("java.lang.Integer")) {
            primTypeName = "int";
        } else if (boxedType.getClassName().equals("java.lang.Long")) {
            primTypeName = "long";
        } else if (boxedType.getClassName().equals("java.lang.Short")) {
            primTypeName = "short";
        } else if (boxedType.getClassName().equals("java.lang.Double")) {
            primTypeName = "double";
        }

        return primTypeName;
    }

    //to get the prim type out of boxed type: e.g. int a = (new Integer(5)).intValue();
    public static SootMethod getBackPrimMethodName(RefType boxedType) {
        String methodName = null;
        methodName = unboxJavaType(boxedType)+"Value";

        return boxedType.getSootClass().getMethodByName(methodName);
    }

    //to get the init method out of boxed type
    public static String getInitPrimMethodSignature(RefType boxedType) {
        String methodSig = null;
        methodSig = "void <init>("+unboxJavaType(boxedType)+")";

        return methodSig;
    }

    public static Constant getConstantForPrim(Type t)
    {
        if(t.equals(DoubleType.v()) || t.equals(RefType.v("java.lang.Double")))
            return DoubleConstant.v(0);
        else if(t.equals(FloatType.v()) || t.equals(RefType.v("java.lang.Float")))
            return FloatConstant.v(0);
        else if(t.equals(LongType.v())|| t.equals(RefType.v("java.lang.Long")))
            return LongConstant.v(0);
        else
            return IntConstant.v(0);
    }
}
