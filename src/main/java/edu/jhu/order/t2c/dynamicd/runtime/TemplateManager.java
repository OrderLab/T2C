package edu.jhu.order.t2c.dynamicd.runtime;

import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.jhu.order.t2c.dynamicd.runtime.T2CHelper.MiniLogger;
import edu.jhu.order.t2c.dynamicd.tscheduler.TestEngine;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static edu.jhu.order.t2c.dynamicd.runtime.CheckerTemplate.gsonPrettyPrinter;

public class TemplateManager {

    static TemplateManager manager = new TemplateManager();

    public static final String templateDir = "./templates_out/";

    List<CheckerTemplate> templates = new ArrayList<>();
    public CheckerTemplate currentTemplate;

    public static TemplateManager getInstance() {
        return manager;
    }

    public void startBuildTemplate(String sourceName) {
        currentTemplate = new CheckerTemplate(sourceName);
    }

    public void finishBuildTemplate(String sourceName) {
        //debugging helper for trunc issues
        //disable now as it often triggers OOM
        //System.out.println("prior to truncate:");
        //System.out.println(currentTemplate.serializeJson());


        currentTemplate.truncate();
        currentTemplate.mergeSymbolMap();

        //add constraints
        currentTemplate.constraints.addAll(Constraint.inferAll(currentTemplate.getSymMap()));

        //filter bad assertions
        currentTemplate.assertions.removeIf(s -> !s.status.equals(Assertion.AssertStatus.NORMAL));
        //index assertions
        if(true){
        for(int i=0;i<currentTemplate.assertions.size();++i)
        {
            int index = -1;
            for(int j=0;j<currentTemplate.operations.size();++j)
            {
                Operation op = currentTemplate.operations.get(j);
                if(op.hashCode()==currentTemplate.assertions.get(i).lastTriggerOpHash)
                {
                    index = j+1;
                    T2CHelper.logInfo("#### put into triggerIndex: "+index+", "+i);
                    if(!currentTemplate.triggerIndex.containsKey(index)){
                    	List<Integer> new_List = new ArrayList<>();
                        new_List.add(i);
                        currentTemplate.triggerIndex.put(index, new_List);
                    }
                    else{
                        currentTemplate.triggerIndex.get(index).add(i);
                    }
                    break;
                 }
             }
            if(index==-1)
                T2CHelper.logInfo("warning: not found triggering operation");
        }
        }
        else
        for(int i=0;i<currentTemplate.operations.size();++i){
            Operation op = currentTemplate.operations.get(i);
            if(!op.relatedAssertions.isEmpty()){
                for(int j=0;j<op.relatedAssertions.size();++j){
                    int assertionNumber = op.relatedAssertions.get(j);
                    for(int k=0;k<currentTemplate.assertions.size();++k){
                        if(currentTemplate.assertions.get(k).assertionLocation == assertionNumber){
                            T2CHelper.logInfo("#### put into triggerIndex: "+(i+1)+", "+k);
                            if(!currentTemplate.triggerIndex.containsKey(i+1)){
                                List<Integer> new_List = new ArrayList<>();
                                new_List.add(k);
                                currentTemplate.triggerIndex.put(i + 1, new_List);
                            }
                            else{
                                currentTemplate.triggerIndex.get(i+1).add(k);
                            }
                        }
                    }
                }
            }
        }

        //clean local symmap in each op
        for(Operation op: currentTemplate.operations)
            op.symbolMap.clear();

        //System.out.println("prior to truncate:");
        //System.out.println(currentTemplate.serializeJson());

        //filter unused symbols
        Set<String> usedSymbols = new HashSet<>();
        for(int i=0;i<currentTemplate.assertions.size();++i)
        {
            for(Map.Entry<String, JsonElement> entry: currentTemplate.assertions.get(i).assertQueryOp.opTree.entrySet())
            {
                usedSymbols.add(entry.getValue().getAsString());
            }
        }
        currentTemplate.symMap.keySet().retainAll(usedSymbols);

        currentTemplate.checkStatus();
        //add here when successfully finish to avoid bad test executions
        templates.add(currentTemplate);
    }

    public void addOp(Operation op) {
        if (currentTemplate == null) {
            // not make it an exception, just print
            //new RuntimeException("Template not inited!").printStackTrace();
            //System.out.println("Template not inited!");
            //disable appending operations before template inited
            return;
        }

        //skip exists for operation, we did same thing for runtime tracer as well
        if (!op.ifQueryOp){
            //System.out.println("ifQueryOp is false, addOp");
            //T2CHelper.logInfo("ifQueryOp is false, addOp");
            currentTemplate.addOp(op);
        }   
        else{
            //System.out.println("ifQueryOp is true, addAssertQueryOp");
            //T2CHelper.logInfo("ifQueryOp is true, addAssertQueryOp");
            currentTemplate.addAssertQueryOp(op);
        }        
    }

    public void addAssert(Assertion assertion) {
        if (currentTemplate == null) {
            //throw new RuntimeException("Template not inited!");
            //this could happen at init phase of junit
            //we just ignore and leave
            return;
        }

        if (GlobalState.mode.equals(GlobalState.T2CMode.VALIDATION)) {
            return;
        }

        currentTemplate.addAssertion(assertion);
    }

    public void flushAllTemplates() {
        //init output templates
        new File(templateDir).mkdirs();

        //T2CHelper.globalLogInfo("#############################");

        String[] excludedTestMethodList = ConfigManager.config.getStringArray(ConfigManager.EXCLUDED_TEST_METHOD_LIST_KEY);
        for (CheckerTemplate template : templates) {
            if(Arrays.asList(excludedTestMethodList).contains(template.templateSource))
                continue;

            //mutate!!
            List<CheckerTemplate> mutated = new ArrayList<>();
            if(ConfigManager.config.getBoolean(ConfigManager.IF_ENABLE_MUTATE_KEY))
            {
                mutated = TemplateMutator.mutate(template,1);
            }
            else {
                mutated.add(template);
            }
            for(CheckerTemplate mutant: mutated)
            {
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(templateDir + mutant.templateSource)))) {
                    //add check to avoid dumping super large template
//                    if(mutant.getOperations().size()+mutant.constraints.size()>10000)
//                    {
//                        System.out.println("WARN: skip large template " +
//                                mutant.templateSource + " with " + mutant.operations.size() +
//                                " operations and "+mutant.constraints.size()+" constraints!");
//                        continue;
//                    }
                    //writer.write(mutant.serializeJson());
                    mutant.serializeJson(new JsonWriter(writer));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static void loadTemplates(String templateDir, List<CheckerTemplate> templates, MiniLogger logger) {
        File[] files = (new File(templateDir)).listFiles();
        if (files == null) {
            System.err.println("Templates input dir not exist!" + new File(templateDir).getAbsolutePath());
            logger.logInfo("Templates input dir not exist!" + new File(templateDir).getAbsolutePath());
            return;
        }

        for (final File fileEntry : files) {
            //TODO: better rule
            if (!fileEntry.getName().contains("#"))
                continue;

            try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(fileEntry)))) {
                logger.logInfo("Loading template from " + fileEntry.getAbsolutePath());
                CheckerTemplate template = CheckerTemplate.deserializeJson(reader);
                //sometimes the template is problematic despite it has been loaded, we check before adding to templates
                if(template == null)
                {
                    logger.logInfo("Failed to load template:" + fileEntry.getAbsolutePath());
                    continue;
                }
                if(template.triggerIndex.isEmpty())
                    continue;
                //logger.logInfo("Successfully load template:" + template.templateSource);
                if(template.getOperations().size()<=2100){
                    templates.add(template);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Total load " + templates.size() + " healthy templates");
        logger.logInfo("Total load " + templates.size() + " healthy templates");
    }

    public void markTestFailed(String testName)
    {
        File dir = new File(FileLayoutManager.getPathForVerifiedInvOutputDir());
        if (!dir.exists()) dir.mkdirs();

        String outputPath = dir.getAbsolutePath() +
                "/" + FileLayoutManager.EXCHANGE_RESULT_FILE_NAME;
        boolean ifAppend = true;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath, ifAppend))) {
            bw.write("[TEST] "+testName+"\n");
            //we write magical number to mark the test failed
            bw.write("FAILED");
            bw.newLine();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void recordTemplatesResult(String testName)
    {
        File dir = new File(FileLayoutManager.getPathForVerifiedInvOutputDir());
        if (!dir.exists()) dir.mkdirs();

        String outputPath = dir.getAbsolutePath() +
                "/" + FileLayoutManager.EXCHANGE_RESULT_FILE_NAME;
        boolean ifAppend = true;

        List<Map<Integer, Integer>> passInvs = new ArrayList<>();
        List<Map<Integer, Integer>> inactiveInvs = new ArrayList<>();
        List<Map<Integer, Integer>> failedInvs = new ArrayList<>();
        List<Map<Integer, Integer>> skipInvs = new ArrayList<>();

        int count = 0;
        for(CheckerTemplate template: RuntimeTracer.getInstance().builtinTemplates)
        {
            if(template == null)
            {
                continue;
            }

            for (Assertion assertion : template.assertions) {
                final Integer tempCount = new Integer(count);

                // loaded templates if never got checked, the state might be null
                if (assertion.state == null || assertion.state.equals(Assertion.AssertState.INACTIVE)) {
                    inactiveInvs.add(new HashMap<Integer, Integer>() {
                        {
                            put(tempCount, assertion.assertionLocation);
                        }
                    });
                } else if (assertion.state.equals(Assertion.AssertState.SKIP)) {
                    skipInvs.add(new HashMap<Integer, Integer>() {
                        {
                            put(tempCount, assertion.assertionLocation);
                        }
                    });
                } else if (assertion.state.equals(Assertion.AssertState.PASS)) {
                    passInvs.add(new HashMap<Integer, Integer>() {
                        {
                            put(tempCount, assertion.assertionLocation);
                        }
                    });
                } else if (assertion.state.equals(Assertion.AssertState.FAIL)) {
                    failedInvs.add(new HashMap<Integer, Integer>() {
                        {
                            put(tempCount, assertion.assertionLocation);
                        }
                    });
                } else {
                    System.out
                            .println("WARN ILLEGAL STATE FOR " + template.templateSource + " " + assertion.state.toString());
                    // throw new RuntimeException("ILLEGAL!");
                }
            }

            count++;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath, ifAppend))) {
            bw.write("[TEST] " + testName + "\n");
            bw.write("pass ");
            for (Map<Integer, Integer> item : passInvs) {
                for (Map.Entry<Integer, Integer> mapItem : item.entrySet()) {
                    bw.write(mapItem.getKey() + "." + mapItem.getValue() + " ");
                }
            }
            bw.newLine();
            bw.write("inac ");
            for (Map<Integer, Integer> item : inactiveInvs) {
                for (Map.Entry<Integer, Integer> mapItem : item.entrySet()) {
                    bw.write(mapItem.getKey() + "." + mapItem.getValue() + " ");
                }
            }
            bw.newLine();
            bw.write("fail ");
            for (Map<Integer, Integer> item : failedInvs) {
                for (Map.Entry<Integer, Integer> mapItem : item.entrySet()) {
                    bw.write(mapItem.getKey() + "." + mapItem.getValue() + " ");
                }
            }
            bw.newLine();
            bw.write("skip ");
            for (Map<Integer, Integer> item : skipInvs) {
                for (Map.Entry<Integer, Integer> mapItem : item.entrySet()) {
                    bw.write(mapItem.getKey() + "." + mapItem.getValue() + " ");
                }
            }
            bw.newLine();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
