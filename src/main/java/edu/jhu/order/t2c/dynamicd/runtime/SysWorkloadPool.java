package edu.jhu.order.t2c.dynamicd.runtime;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.jhu.order.t2c.dynamicd.tscheduler.TestClassPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SysWorkloadPool {

    private static final Logger LOG = LoggerFactory.getLogger(SysWorkloadPool.class);

    static public Set<String> sysWorkloadTypeSet = new HashSet<>();
    static public Set<String> sysWorkloadSpecialSet = new HashSet<>();
    
    private static final Cache<String, Object> objCache = Caffeine.newBuilder()
      .expireAfterWrite(1, TimeUnit.MINUTES)
      .maximumSize(8)
      .build();

    public static Object getSysWorkloadSpecial(String cName, Object... args){
        if (cName.equals("org.apache.cassandra.cql3.UntypedResultSet")){
            // args[0] = customizedOpId
            // args[1] = queryStr
            try{
                // qp = QueryProcessor
                Class<?> retClass = Class.forName(cName);
                Class<?> qpClass = Class.forName("org.apache.cassandra.cql3.QueryProcessor");
                Method execInternal = qpClass.getMethod("executeInternal", String.class, Object[].class);
                // CS-14092
                if ((int) args[0] == 4214){
                    return retClass.cast(execInternal.invoke(null, "select * from test.ttlTable"));
                } else {
                    return retClass.cast(execInternal.invoke(null, String.valueOf(args[1])));
                }
            } catch (Throwable ex) {
                //T2CHelper.prodLogInfo(ex.getMessage());
                //System.err.println(ex);
                //LOG.error(ex.getMessage());
            }
        } else if (cName.equals("java.io.File")) {
            try{
                Class<?> retClass = Class.forName("java.io.File");
                Constructor<?> retCons = retClass.getConstructor(String.class);
                return retCons.newInstance("./data");
            } catch (Throwable ex) {
                //T2CHelper.prodLogInfo(ex.getMessage());
                //System.err.println(ex);
                //LOG.error(ex.getMessage());
            }
        } else if(cName.equals("numSnaps")){
            try {
                // ZK-4362 CHANGE THE SNAPSHOT PATH
                File snapLogDir = new File("/home/dparikesit/Projects/garudaAce/zookeeper/version-2");
                Class<?> snapLogClass = Class.forName("org.apache.zookeeper.server.persistence.FileSnap");
                Constructor<?> snapLogCons = snapLogClass.getConstructor(snapLogDir.getClass());
                Object snapLogObj = snapLogCons.newInstance(snapLogDir);
                Method findNRecentSnapshots = snapLogClass.getMethod("findNRecentSnapshots", int.class);
                List<?> listOfFiles = (List<?>) findNRecentSnapshots.invoke(snapLogObj,10);
                return listOfFiles.size();
            } catch (Throwable ex) {
                //T2CHelper.prodLogInfo(ex.getMessage());
                //System.err.println(ex);
                //LOG.error(ex.getMessage());
            }
        }
        return null;
    }
    
    public static Object createObj(String cName) throws ClassNotFoundException, InstantiationException, InvocationTargetException, IllegalAccessException {
        Class<?> c = Class.forName(cName);
        Object workload = c.getConstructors()[0].newInstance();
        try{
            Method method = c.getDeclaredMethod("setUpClass");
            method.invoke(workload);
            if(!cName.contains("cassandra")){
                //T2CHelper.prodLogInfo("#### try invoke beforeTest for"+cName);
                Method method2 = c.getSuperclass().getDeclaredMethod("beforeTest");
                method2.invoke(workload);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored){
            // Do nothing
        }
        
        return workload;
    }

    public static Object getSysWorkload(String cName, Object... args) {
        if (sysWorkloadSpecialSet.contains(cName)){
            return getSysWorkloadSpecial(cName, args);
        }

        return objCache.get(cName, k -> {
            try {
                return createObj(cName);
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                return null;
            }
        });
    }

    public static boolean isSysWorkload(String name) {
        return sysWorkloadSpecialSet.contains(name)||sysWorkloadTypeSet.contains(name);
    }

    public static void registerTypes() {
        if(!GlobalState.mode.equals(GlobalState.T2CMode.UNITTEST))
            TestClassPool.registerAllClass();
        LOG.info("start to registerTypes");
        System.out.println("start to registerTypes");

        sysWorkloadTypeSet.addAll(TestClassPool.getClasses());
        sysWorkloadSpecialSet.add("org.apache.cassandra.cql3.UntypedResultSet");
        sysWorkloadSpecialSet.add("java.io.File");
        sysWorkloadSpecialSet.add("numSnaps");
    }
}