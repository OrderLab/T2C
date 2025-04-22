package edu.jhu.order.t2c.staticd.tool;

//this class is used to calculate target tests we aim to generate checker, by joining tests w/ op and tests w/ asserts

import edu.jhu.order.t2c.dynamicd.runtime.T2CHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// tests w/ op: run build with not transformed tests
// test w/ asserts: run transformed tests
public class TemplateListJoiner {


    static void doInnerJoin(String wopFile, String wassertFile, String wopsucFile) throws IOException {
        List<String> lst = Files.readAllLines(Paths.get(wopFile));
        Set<String> set = new HashSet<>(lst);
        List<String> lst2 = Files.readAllLines(Paths.get(wassertFile));
        Set<String> set2 = new HashSet<>();
        for(String s: lst2)
        {
            set2.add(s.replaceAll("<.*?>", ""));
        }

        set.retainAll(set2);
        System.out.println("target tests:");
        for(String s: set)
        {
            System.out.println(s);
        }
        int size1= set.size();
        System.out.println("target tests: "+size1);

        List<String> lst3 = Files.readAllLines(Paths.get(wopsucFile));
        Set<String> set3 = new HashSet<>(lst3);

        set.retainAll(set3);

        System.out.println("transformed tests in target:");
        for(String s: set)
        {
            System.out.println(s);
        }
        int size2= set.size();
        System.out.println("transformed tests in target:" + size2);

    }

    public static void main(String[] args) throws IOException {

        doInnerJoin(T2CHelper.testwassertListFileName, T2CHelper.testwopListFileName, T2CHelper.testwassertsucListFileName);
    }
}
