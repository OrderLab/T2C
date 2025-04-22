package edu.jhu.order.t2c.staticd.cases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class MutationSample {
    private static final AtomicReferenceFieldUpdater<MutationSample, Integer> refUpdater = AtomicReferenceFieldUpdater.newUpdater(MutationSample.class, Integer.class, "atomicNum");


    Set<String> set = new HashSet<>();
    List<String> lst = new ArrayList<>();
    Map<Integer, String> map = new HashMap<>();
    int num = 10;
    int num2 = 10;

    Integer atomicNum = 10;

    Map<Integer,Map<Integer,String>> nestedMap = new HashMap<>();
    Map<Integer,Map<Integer,Map<Integer,String>>> nestedNestedMap = new HashMap<>();

    int arr[] = {1,2,3};

    MutationSample field;

    void testSimple()
    {
        //some mutations, should be tracked
        set.add("11");
        lst.add("22");
        lst.add("33");
        lst.remove(0);
        map.put(1,"44");

        //some gets, should not be tracked
        int size = set.size();
        String str = lst.get(0);
        if(map.containsKey(1))
        {
            System.out.println(map.get(1));
        }
    }

    void testPrimtype()
    {
        //tracked
        num = 10;
        num++;

        //not tracked
        int n = num;
        n++;

        //tracked
        num2 = n;
    }

    void testRecursive()
    {
        //should be tracked

        Set<String> newSet = set;
        newSet.add("foo");
        //jimple would automatically merge the same reference, however we need to deal with cast
        //as well
        HashSet<String> newSet2 = (HashSet<String>) newSet;
        newSet2.add("foo");

        //should not be tracked
        int num2 = num;
        num2++;
    }

    void testNestedCollection()
    {
        //such usage should still be tracked as mutation
        nestedMap.get(0).put(1,"foo");
        nestedNestedMap.get(0).get(0).put(1,"foo");

    }

    void testInterProcedureCall()
    {
        testInterProcedureCallHelper(set);
    }

    void testInterProcedureCallHelper(Set<String> set)
    {
        set.add("foo");

    }

    void testAtomicReferenceFieldUpdater()
    {
        refUpdater.compareAndSet(this, atomicNum, new Integer(10));

    }

    void testArrayWrite()
    {
        int index = 0;

        //tracked
        arr[index] = 1;

        //not tracked
        int b = arr[index];
        System.out.println(b);
    }

    void foo()
    {
        MutationSample s= new MutationSample();
        s.num = 10;


        field = s;
    }

}
