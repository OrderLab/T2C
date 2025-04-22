package edu.jhu.order.t2c.staticd.algorithm;

import edu.jhu.order.t2c.staticd.transformer.TestAssertionPacker;
import edu.jhu.order.t2c.staticd.util.SootUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.annotation.purity.PurityAnalysis;
import soot.jimple.toolkits.annotation.purity.PurityInterproceduralAnalysis;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.tagkit.GenericAttribute;
import soot.tagkit.StringTag;
import soot.tagkit.Tag;

import java.util.*;

public class MethodPurityAnalysis {
    private static final Logger LOG = LoggerFactory.getLogger(MethodPurityAnalysis.class);

    private static final Set<String> PURE_METHODS = new HashSet<String>(Arrays.asList(
            //new String[] {"org.apache.zookeeper.ZooKeeper#create"}
            new String[] {"org.apache.hadoop.hbase.constraint.Constraints#enabled"}
    ));


    public static void doAnalysis()
    {
        Map newOpts = new HashMap<String, String>();
        newOpts.put("enabled","true");
        newOpts.put("print","true");
        newOpts.put("annotate","true");
        newOpts.put("verbose","true");
        newOpts.put("dump-cg","true");
        newOpts.put("dump-summaries","true");
        //for(Object name: newOpts.keySet())
        //{
        //    System.out.println((String)name+newOpts.get(name));
        //}
        PurityAnalysis.v().transform("PurityAnalysis", newOpts);
    }


    //needs to do PurityTransform first, otherwise always return false
    public static boolean ifMethodPureV1(SootMethod method)
    {
        String fullName = method.getDeclaringClass().getName()+"#"+(method.getName());
        LOG.info("ifMethodPure"+ fullName);
        if(PURE_METHODS.contains(fullName))
            return true;

        for(Tag tag: method.getTags())
        {
            if(tag instanceof GenericAttribute)
            {
                if(tag.getName().equals("Pure"))
                {
                    return true;
                }
            }
        }

        return false;
    }

    //needs to do PurityTransform first, otherwise always return true (try to include in generated assertion)
    //revert the usage logic of purity analysis, now we always assume the function is pure and try to include
    //in the generated assertion methods, unless we found it should be excluded
    public static boolean ifMethodPure(SootMethod method)
    {
        String fullName = method.getDeclaringClass().getName()+"#"+(method.getName());
        LOG.info("ifMethodPure"+ fullName);

        //use heristics to filter
        String methodName = method.getName().toLowerCase();

        if(PURE_METHODS.contains(fullName))
            return true;

        if(methodName.contains("create") || methodName.contains("add") || methodName.contains("disable") || methodName.contains("enable") ||
                methodName.contains("put") || methodName.contains("delete") || methodName.contains("set")
                || methodName.contains("write") || methodName.contains("split") || methodName.contains("clone")
                || methodName.contains("incre") || methodName.contains("append") || methodName.contains("clean")
                || methodName.contains("submit") || methodName.contains("merge") || methodName.contains("truncate")
                || methodName.contains("rename") || methodName.contains("start") || methodName.contains("build")
                || methodName.contains("do")  || methodName.contains("buld") || methodName.contains("compact")
                || methodName.contains("open") || methodName.contains("lock") || methodName.contains("snapshot")
                || methodName.contains("<init>") || methodName.contains("getexceptioncause") || methodName.contains("run")
                || methodName.contains("commit") || methodName.contains("mock") || methodName.contains("install")
                || methodName.contains("exec") || methodName.contains("enforce") || methodName.contains("sync")
                || methodName.contains("place") || methodName.contains("next") || methodName.contains("advance"))
            return false;


        for(Tag tag: method.getTags())
        {
            if(tag instanceof StringTag)
            {
                if(((StringTag) tag).getInfo().equals("purity: impure"))
                {
                    return false;
                }
            }
        }

        return true;
    }
}
