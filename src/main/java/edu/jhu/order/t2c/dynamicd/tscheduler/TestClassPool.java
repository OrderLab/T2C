package edu.jhu.order.t2c.dynamicd.tscheduler;

import java.util.*;

import com.google.common.collect.Lists;
import edu.jhu.order.t2c.dynamicd.runtime.ConfigManager;
import edu.jhu.order.t2c.staticd.util.JavaUtils;
import org.reflections8.*;
import org.reflections8.scanners.*;
import org.reflections8.util.*;

public class TestClassPool {

    static private List<String> classLst = new ArrayList<>();

    static public void registerAllClass()
    {
        String prefix = ConfigManager.config.getString(ConfigManager.SYSTEM_PACKAGE_PREFIX_KEY);
        String regex = ConfigManager.config.getString(ConfigManager.TEST_CLASS_NAME_REGEX_KEY);
        String[] specifiedClassList = ConfigManager.config.getStringArray(ConfigManager.SPECIFIED_TEST_CLASS_LIST_KEY);
        if(prefix==null || regex==null)
        {
            throw new RuntimeException("system_package_prefix or test_class_name_regex not set in the config!");
        }

        System.out.println("Start to analyze all test classes with prefix: "+ prefix);
        Reflections reflections = new Reflections(prefix, new SubTypesScanner(false));

        Set<Class<? extends Object>> allClasses =
                reflections.getSubTypesOf(Object.class);
        List<Class<? extends Object>> allClassesLst = (new ArrayList<>(allClasses));
        allClassesLst.sort((a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName()));
        int partitionTotal = 1, partitionIndex = 0;
        if(System.getProperty("t2c.pindex")!=null && !System.getProperty("t2c.pindex").equals(""))
            partitionIndex = Integer.parseInt(System.getProperty("t2c.pindex"));
        if(System.getProperty("t2c.ptotal")!=null && !System.getProperty("t2c.ptotal").equals(""))
            partitionTotal = Integer.parseInt(System.getProperty("t2c.ptotal"));

        System.out.println("Run partition "+(partitionIndex+1)+"/"+partitionTotal);
        for(Class clazz: JavaUtils.chopped(allClassesLst, partitionTotal).get(partitionIndex))
        {
            try{
                //skip subclass
                if(clazz.getName().contains("$"))
                    continue;

		//System.out.println("#### class name: "+clazz.getSimpleName());
                if(!clazz.getSimpleName().matches(regex))
                    continue;

                if(specifiedClassList.length>0)
                    if(!Arrays.asList(specifiedClassList).contains(clazz.getName()))
                        continue;
                
                register(clazz.getName());
                System.out.println("registering for test class "+clazz.getName());
            } catch (Exception ex){
                System.out.println("registering failed for test class "+clazz.getName());
            }

        }
        System.out.println("Finished with "+classLst.size()+" test classes added.");
    }

    static public void register(String clazz)
    {
        classLst.add(clazz);
    }

    public void registerSpecificClasses() throws Exception
    {
        String className = System.getProperty("ok.testname");
        register(className);
    }

    static public Class getClass(String classname) throws Exception
    {
        return Class.forName(classname);
    }

    static public Collection<String> getClasses()
    {
        return classLst;
    }
}
