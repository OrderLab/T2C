package edu.jhu.order.t2c.dynamic;

import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class AssertionPool {
    public static void externalAssertFunc0(Integer var0) throws Exception {
        if (var0 == 7) {
            RuntimeDetectionTest.assertTestSimple = true;
        } else
            System.err.println("Get wrong input " + var0);
    }

    public static void externalAssertFunc1(String var0, Integer var1, short[] var2) throws Exception {
        if (Integer.parseInt(var0) + var1 + var2.length == 6)
            RuntimeDetectionTest.assertTestArray = true;
        else
            System.err.println("Get wrong input " + var0);
    }

    public static void externalAssertFunc2(String var0, Integer var1, RuntimeDetectionTest.ComplexObject var2) throws Exception {

        int result = Integer.parseInt(var0) + var1 + var2.x + Integer.parseInt(var2.y);
        try (BufferedReader br = new BufferedReader(new FileReader(var2.file))) {
            String line = br.readLine();
            result += Integer.parseInt(line);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //System.err.println("Get input " +result);

        if (result == 15)
            RuntimeDetectionTest.assertTestComplexObjects = true;
        else
            System.err.println("Get wrong input " + result);
    }

    public static void externalAssertFunc3(Integer var0) throws Exception {
        if (var0 == 8) {
            RuntimeDetectionTest.assertTestDefault = true;
        } else
            System.err.println("Get wrong input " + var0);
    }

    public static void externalAssertFunc4(RuntimeDetectionTest test, Integer var0) throws Exception {
        if (var0 == 8) {
            test.internalUtil();
        } else
            System.err.println("Get wrong input " + var0);
    }
    
    public static void externalAssertFunc5(String var0, Integer var1, RuntimeDetectionTest.ComplexObject var2) throws Exception {

        int result = Integer.parseInt(var0) + var1 + var2.x + Integer.parseInt(var2.y);
        try (BufferedReader br = new BufferedReader(new FileReader(var2.file))) {
            String line = br.readLine();
            result += Integer.parseInt(line);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //System.err.println("Get input " +result);

        if (result == 16)
            RuntimeDetectionTest.assertTestComplexObjects2 = true;
        else
            System.err.println("Get wrong input " + result);
    }
}