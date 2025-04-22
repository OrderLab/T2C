package edu.jhu.order.t2c.staticd.tool;

import com.google.gson.stream.JsonWriter;
import edu.jhu.order.t2c.dynamicd.runtime.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.jhu.order.t2c.staticd.util.JavaUtils.sortByValue;

/*
    Tool class to merge, which is part of verifying
    note this is not an isolated tool class
 */
public class TemplateValidationMerger {

    public String output_dir = "invalid";

    private void dumpTemplates(List<CheckerTemplate> templates, String fileName)
    {
        try {
            File logFile = new File(output_dir+"/"+fileName);
            //cleanup old one
            Files.deleteIfExists(logFile.toPath());

            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile)));

            for(CheckerTemplate template:templates)
                //writer.write(template.serializeJson());
                template.serializeJson(new JsonWriter(writer));

            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void dumpTemplatesToDir(List<CheckerTemplate> templates, String dirName) {

        File directory = new File(output_dir + "/" + dirName);
        // Try to create the directory if it doesn't exist
        if (!directory.exists()) {
            boolean isCreated = directory.mkdir();
            if (isCreated) {
                System.out.println("Directory created successfully");
                // Now let's create files in this directory
                for (CheckerTemplate template : templates) {
                    try {
                        File logFile = new File(output_dir + "/" + dirName + "/" + template.templateSource);
                        Writer writer = new BufferedWriter(new OutputStreamWriter(
                                new FileOutputStream(logFile)));
                        //writer.write(template.serializeJson());
                        template.serializeJson(new JsonWriter(writer));
                        writer.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                System.err.println("Failed to create the directory");
            }
        } else {
            System.out.println("Directory already exists");
        }

    }

    private void dumpTemplatesOpNum(List<CheckerTemplate> templates, String fileName)
    {
        try {
            File logFile = new File(output_dir+"/"+fileName);
            //cleanup old one
            Files.deleteIfExists(logFile.toPath());

            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile)));

            int count  = 0;
            for(CheckerTemplate template:templates)
            {
                writer.write(count+" "+template.getOperations().size()+"\n");
                count++;
            }

            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //dump the distribution of passed test to inspect
    private void dumpDist(Map<Integer, Integer> occuranceMap, String fileName)
    {
        //phase 1
        //transform occuranceMap to distMap
        //key: how many test passed
        //val: list of templates
        TreeMap<Integer, List<Integer>> distMap = new TreeMap<>();

        for(Map.Entry<Integer, Integer> entry: occuranceMap.entrySet())
        {
            //id of inv
            Integer key = entry.getKey();
            //how many test passed
            Integer val = entry.getValue();
            if(distMap.containsKey(val))
            {
                distMap.get(val).add(key);
            }
            else {
                List<Integer> lst = new ArrayList<>();
                lst.add(key);
                distMap.put(val, lst);
            }
        }

        //phase 2
        //dump the distMap
        try {
            File logFile = new File(output_dir+"/"+fileName);
            //cleanup old one
            Files.deleteIfExists(logFile.toPath());

            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile)));

            for(Map.Entry<Integer, List<Integer>> entry:distMap.entrySet())
            {
                writer.write(entry.getKey()+","+entry.getValue().size()+","+entry.getValue());
                writer.write("\n");
            }
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void summarizeVerifiedInvs(Map<Integer, Map<Integer, Integer>> passInvMap,
                                       Map<Integer, Map<Integer, Integer>> inactiveInvMap,
                                       Map<Integer, Map<Integer, Integer>> failInvMap, Map<Integer, Map<Integer, Integer>> skipInvMap, List<CheckerTemplate> totalTemplates,
                                       List<CheckerTemplate> outputVerifiedInvs) {
                           
       Map<String, Set<Integer>> savedAssertionAll = new HashMap<>();
       int assertCount = 0;
       
       for(CheckerTemplate template:totalTemplates){
           assertCount += template.assertions.size();
       }
       // <template_id, <assertion_id, count>>
       for (Map.Entry<Integer, Map<Integer, Integer>> item1 : passInvMap.entrySet()) {
           CheckerTemplate inv = totalTemplates.get(item1.getKey());
    
           String baseSource;
           if(inv.templateSource.contains("_mutated_")){
               baseSource = inv.templateSource.split("_mutated_")[0];
           } else {
               baseSource = inv.templateSource;
           }
    
           if(!savedAssertionAll.containsKey(baseSource)){
               savedAssertionAll.put(baseSource, new HashSet<>());
           }
    
           for (Map.Entry<Integer, Integer> item2 : item1.getValue().entrySet()) {
               Integer passNum = 0;
               Integer inactiveNum = 0;
               Integer failNum = 0;

               passNum = item2.getValue();
               if (skipInvMap.get(item1.getKey()) != null
                   && skipInvMap.get(item1.getKey()).get(item2.getKey()) != null) {
                   continue;
               }
               if (inactiveInvMap.get(item1.getKey()) != null
                   && inactiveInvMap.get(item1.getKey()).get(item2.getKey()) != null) {
                   inactiveNum = inactiveInvMap.get(item1.getKey()).get(item2.getKey());
               }
               if (failInvMap.get(item1.getKey()) != null
                   && failInvMap.get(item1.getKey()).get(item2.getKey()) != null) {
                   failNum = failInvMap.get(item1.getKey()).get(item2.getKey());
               }

               if (passNum > 0) { // !(failNum > 5 || passNum == 0)
                    if(inv.templateSource.contains("cassandra") && failNum <= 5){
                        savedAssertionAll.get(baseSource).add(item2.getKey());
                    }
                    if(!inv.templateSource.contains("cassandra")){
                        savedAssertionAll.get(baseSource).add(item2.getKey());
                    }
               }
           }
       }

        for (CheckerTemplate inv: totalTemplates){
            String baseSource;
            if(inv.templateSource.contains("_mutated_")){
                baseSource = inv.templateSource.split("_mutated_")[0];
            } else {
                baseSource = inv.templateSource;
            }

            inv.triggerIndex.clear();
            for(Operation op: inv.operations){
                op.relatedAssertions.clear();
            }

            inv.assertions.removeIf(assertion -> !savedAssertionAll.get(baseSource).contains(assertion.getAssertionLocation()));

            List<Integer> opList = new ArrayList<>();
            int opIdx = 0;
            for (int i=0; i<inv.assertions.size(); i++){
                opList.clear();
                for(int j=0; j<inv.operations.size(); j++){
                    if(inv.operations.get(j).hashCode()==inv.assertions.get(i).lastTriggerOpHash){
                        opList.add(j);
                    }
                }
                if(!opList.isEmpty()) {
                    opIdx = opList.get(0);
                }
                inv.operations.get(opIdx).relatedAssertions.add(i);
                int index = opIdx+1;
                T2CHelper.logInfo("#### put into triggerIndex: " + index + ", " + i);
                if (!inv.triggerIndex.containsKey(index)) {
                    List<Integer> new_List = new ArrayList<>();
                    new_List.add(i);
                    inv.triggerIndex.put(index, new_List);
                } else {
                    inv.triggerIndex.get(index).add(i);
                }
            }
    
           if(inv.assertions.isEmpty()){
               inv.warnings.add(CheckerTemplate.TemplateWarning.NO_VALID_ASSERTION);
           }
    
           if(inv.warnings.isEmpty()){
               outputVerifiedInvs.add(inv);
           }
       }
    }

    private void dumpRank(Map<Integer, CheckerTemplate.VerifyStats> statsMap, String fileName)
    {

        try {
            File logFile = new File(output_dir+"/"+fileName);
            //cleanup old one
            Files.deleteIfExists(logFile.toPath());

            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile)));

            //traverse twice

            //first
            for(Map.Entry<Integer, CheckerTemplate.VerifyStats> entry: statsMap.entrySet())
            {
                if(entry.getValue().failNum==0)
                {
                    writer.write( entry.getKey()+" "+entry.getValue().passNum +
                            " " + entry.getValue().inactiveNum + " " + entry.getValue().failNum);
                    writer.write("\n");
                }
            }

            //second
            for(Map.Entry<Integer, CheckerTemplate.VerifyStats> entry: statsMap.entrySet())
            {
                if(entry.getValue().failNum>0)
                {
                    writer.write( entry.getKey()+" "+entry.getValue().passNum +
                            " " + entry.getValue().inactiveNum + " " + entry.getValue().failNum);
                    writer.write("\n");
                }
            }
            //leave a EOF in case we thought nothing useful get printed
            writer.write("EOF.\n");
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void aggregate()
    {
        long startTime = System.currentTimeMillis();

        RuntimeTracer runtimeTracer = null;
        try{
            runtimeTracer = new RuntimeTracer();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(-1);
        }
        //runtimeTracer.loadTemplates();
        List<CheckerTemplate> totalTemplates = runtimeTracer.builtinTemplates;

        // <invid, <assertion_id, count>>
        Map<Integer, Map<Integer, Integer>> passInvMap = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> inactiveInvMap = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> passOrInactiveInvMap = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> failInvMap = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> skipInvMap = new HashMap<>();
        List<Assertion> selfPassInvs = new ArrayList<>();
        List<Assertion> selfPassUnhealthyInvs = new ArrayList<>();
        List<CheckerTemplate> verifiedInvs = new ArrayList<>();
        Map<Integer, CheckerTemplate.VerifyStats> statsMap = new LinkedHashMap<>();

        // init passInvMap
        for (int i = 0; i < totalTemplates.size(); ++i) {
            for (Assertion assertion : totalTemplates.get(i).getAssertions()) {
                passInvMap.put(i, new HashMap<Integer, Integer>() {
                    {
                        put(assertion.getAssertionLocation(), 0);
                    }
                });
            }
        }

        //init folder if not exists
        File dir = new File(output_dir);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(output_dir+"/"+ FileLayoutManager.EXCHANGE_RESULT_FILE_NAME);
        BufferedReader reader = null;

        int headerCount = 0;
        int failedCount = 0;

        List<String> inactiveHealthyTemplates = new ArrayList<>();
        List<String> inactiveUnhealthyTemplates = new ArrayList<>();
        List<String> failHealthyTemplates = new ArrayList<>();
        List<String> failUnhealthyTemplates = new ArrayList<>();

        //the format of exchange file looks like (suppose you have 10 templates):
        //[TEST] test1
        //FAILED
        //[TEST] test2
        //pass 1 2 3 4 5                                  (passed templates)
        //inac 6 7                                        (inactive templates in this test)
        //fail 8 9 10                                     (failed templates)
        try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;
            String lastTestName = null;

            while ((text = reader.readLine()) != null) {
                if (text.startsWith("[TEST]"))
                {
                    //save lastTestName for checking activation
                    lastTestName = text.substring(7);
                    lastTestName = lastTestName.replace("\n", "").replace("\r", "");

                    //just start of a new block
                    headerCount++;
                }
                else if (text.startsWith("FAILED"))
                    failedCount++;
                else
                {
                    Map<Integer, Map<Integer, Integer>> invMapRef = null;
                    if(text.startsWith("pass"))
                        invMapRef = passInvMap;
                    else if(text.startsWith("inac"))
                        invMapRef = inactiveInvMap;
                    else if(text.startsWith("fail"))
                        invMapRef = failInvMap;
                    else if (text.startsWith("skip"))
                        invMapRef = skipInvMap;
                    else
                    {
                        throw new RuntimeException("[ERROR] Incorrect format detected with content: "+text);
                    }

                    for(String intStr: text.substring(4).split("\\s+"))
                    {
                        try {
                            int template_id = Integer.parseInt(intStr.split("\\.")[0]);
                            int assertion_id = Integer.parseInt(intStr.split("\\.")[1]);
                            
                            if (!invMapRef.containsKey(template_id)) {
                                invMapRef.put(template_id, new HashMap<Integer, Integer>());
                            }
                            
                            if (invMapRef.containsKey(template_id)
                                && !invMapRef.get(template_id).containsKey(assertion_id)) {
                                invMapRef.get(template_id).put(assertion_id, 0);
                            }
                            invMapRef.get(template_id).put(assertion_id,invMapRef.get(template_id).get(assertion_id) + 1);
                            // invMapRef.putIfAbsent(template_id, 0);
                            // invMapRef.put(id, invMapRef.get(id) + 1);

                            /// templates should be activated in their own test (which creates them)
                            if (text.startsWith("pass")) {
                                String source = totalTemplates.get(template_id).templateSource;
                                if (source.equals(lastTestName)) {
                                    Assertion assertion = totalTemplates.get(template_id).getAssertions().stream()
                                            .filter(item -> assertion_id == item.getAssertionLocation())
                                            .findFirst().orElse(null);
                                    if (assertion != null) {
                                        if (totalTemplates.get(template_id).warnings.size() == 0) {
                                            selfPassInvs.add(assertion);
                                        } else {
                                            selfPassUnhealthyInvs.add(assertion);
                                        }
                                    }
                                }
                            } else if (text.startsWith("inac")) { // templates should be activated in their own test (which creates them)
                                String source = totalTemplates.get(template_id).templateSource;
                                if (source.equals(lastTestName)) {
                                    // System.out.println("WARN: "+id+" template "+source+ " fails to be activated
                                    // in its own test!");
                                    try {
                                        if (totalTemplates.get(template_id).warnings.size() == 0)
                                            inactiveHealthyTemplates.add(source + "-" + totalTemplates.get(template_id)
                                                    .getAssertions().get(assertion_id).getAssertionLocation());
                                        else
                                            inactiveUnhealthyTemplates
                                                    .add(source + "-" + totalTemplates.get(template_id)
                                                            .getAssertions().get(assertion_id).getAssertionLocation());
                                    } catch (Exception e) {
                                        // TODO: handle exception
                                    }
                                    // System.out.println("\titis a healthy template!");
                                }
                            } else if (text.startsWith("fail")) {
                                String source = totalTemplates.get(template_id).templateSource;
                                if (source.equals(lastTestName)) {
                                    // System.out.println("WARN: "+id+" template "+source+ " fails to be activated
                                    // in its own test!");
                                    try {
                                        if (totalTemplates.get(template_id).warnings.size() == 0)
                                            failHealthyTemplates.add(source + "-" + totalTemplates.get(template_id)
                                                    .getAssertions().get(assertion_id).getAssertionLocation());
                                        else
                                            failUnhealthyTemplates.add(source + "-" + totalTemplates.get(template_id)
                                                    .getAssertions().get(assertion_id).getAssertionLocation());
                                    } catch (Exception e) {
                                        // TODO: handle exception
                                    }
                                    // System.out.println("\titis a healthy template!");
                                }
                            }

                        } catch (NumberFormatException ex)
                        {
                            //just continue
                            //this is empty line
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println(inactiveHealthyTemplates.size()+" checkers did not get activated in their own tests and are healthy, they are:");
        for(String src: inactiveHealthyTemplates)
        {
            System.out.println("\t"+src);
        }
        System.out.println(inactiveUnhealthyTemplates.size()+" checkers did not get activated in their own tests but are unhealthy anyway, they are:");
        for(String src: inactiveUnhealthyTemplates)
        {
            System.out.println("\t"+src);
        }

        System.out.println(failHealthyTemplates.size()+" checkers did not pass in their own tests and are healthy, they are:");
        for(String src: failHealthyTemplates)
        {
            System.out.println("\t"+src);
        }
        System.out.println(failUnhealthyTemplates.size()+" checkers did not pass in their own tests but are unhealthy anyway, they are:");
        for(String src: failUnhealthyTemplates)
        {
            System.out.println("\t"+src);
        }


        int healthyTemplatesTotal = 0;
        for (CheckerTemplate totalTemplate : totalTemplates)
            if (totalTemplate.warnings.size() == 0)
                healthyTemplatesTotal++;

        // passOrInactiveInvMap = sumMap(passInvMap,inactiveInvMap);

        //System.out.println("Result: ");

        // int maxPass = passInvMap.isEmpty()?0:Collections.max(passInvMap.values());
        // int maxSucc = passOrInactiveInvMap.isEmpty()?0:Collections.max(passOrInactiveInvMap.values());
//        for(Map.Entry<Integer, Integer> entry:passOrInactiveInvMap.entrySet())
//        {
//            if(entry.getValue()>=maxSucc)
//            {
//                //System.out.print(entry.getKey()+":"+entry.getValue()+" ");
//                verifiedInvs.add(totalTemplates.get(entry.getKey()));
//            }
//        }

        summarizeVerifiedInvs(passInvMap, inactiveInvMap, failInvMap, skipInvMap, totalTemplates, verifiedInvs);

        System.out.println("[[SUMMARY]]");
        System.out.println("Total finished tests:"+(headerCount-failedCount));
        System.out.println("Total aborted tests:"+failedCount);
        //System.out.println("Most successful templates pass or inactive in "+maxSucc+" tests");
        //System.out.println("Most valuable templates pass in "+maxPass+" tests");
        System.out.println("Dumping output now...");
        System.out.println("Total "+totalTemplates.size()+" templates loaded");
        System.out.println("Among which, "+healthyTemplatesTotal+" templates are healthy");
        dumpTemplates(verifiedInvs, FileLayoutManager.VERIFIED_FILE_NAME);
        dumpTemplatesToDir(verifiedInvs, FileLayoutManager.VERIFIED_DIR_NAME);
        System.out.println("Dumped "+verifiedInvs.size()+" templates as verified");
        // dumpTemplates(selfPassInvs, FileLayoutManager.SELFPASS_FILE_NAME);
        System.out.println("Dumped "+selfPassInvs.size()+" templates as self-pass" +", while "+selfPassUnhealthyInvs.size()+" templates passed but not healthy");
        dumpTemplates(totalTemplates, FileLayoutManager.TOTAL_FILE_NAME);
        System.out.println("Dumped templates opNum for debugging");
        dumpTemplatesOpNum(totalTemplates, FileLayoutManager.TOTAL_FILE_NAME+"_opnum");
        totalTemplates.removeAll(verifiedInvs);
        dumpTemplates(totalTemplates, FileLayoutManager.DISCARDED_FILE_NAME);
        // dumpDist(passOrInactiveInvMap, FileLayoutManager.DIST_FILE_NAME);
        dumpRank(statsMap, FileLayoutManager.RANK_FILE_NAME);
        System.out.println("Verify finished.");

        long endTime = System.currentTimeMillis();

        System.out.println("Merge took " + (endTime - startTime) + " milliseconds");
    }

    private Map<Integer, Integer> sumMap(Map<Integer, Integer>... maps) {
        return Stream.of(maps)    // Stream<Map<..>>
                .map(Map::entrySet)  // Stream<Set<Map.Entry<..>>
                .flatMap(Collection::stream) // Stream<Map.Entry<..>>
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        Integer::sum));
    }

    public static void main(String[] args)
    {
        ConfigManager.initConfig();

        TemplateValidationMerger merger = new TemplateValidationMerger();
        merger.output_dir = FileLayoutManager.getPathForVerifiedInvOutputDir();
        merger.aggregate();
    }
}