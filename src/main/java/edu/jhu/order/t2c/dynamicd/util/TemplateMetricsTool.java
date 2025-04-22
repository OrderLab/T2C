package edu.jhu.order.t2c.dynamicd.util;

import edu.jhu.order.t2c.dynamicd.runtime.CheckerTemplate;
import edu.jhu.order.t2c.dynamicd.runtime.T2CHelper;
import edu.jhu.order.t2c.dynamicd.runtime.T2CHelper.MiniLogger;
import edu.jhu.order.t2c.dynamicd.runtime.TemplateManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TemplateMetricsTool {
    private static List<CheckerTemplate> builtinTemplates = new ArrayList<>();

    private static void opNumberDistributionAnalysis() {
        System.out.println("Run opNumberDistributionAnalysis:");
        List<Integer> list = new ArrayList<>();
        System.out.println("Highlighted templates:");
        for(CheckerTemplate template: builtinTemplates)
        {
            list.add(template.getOperations().size());
            if(template.getOperations().size()>=10)
            {
                System.out.println(template.templateSource+": "+template.getOperations().size());
            }
        }
        System.out.println("Summary: ");
        System.out.println(Arrays.toString(list.toArray()));
    }

    private static void runAnalysis() {
        opNumberDistributionAnalysis();

    }

    public static void main(String... args) {

        System.out.println("T2C TemplateMetricsTool v1.0");
        System.out.println("Args:");
        for(String arg:args)
            System.out.println(arg);

        if(args.length<1)
        {
            System.err.println("Bad input: no args for template folder");
            System.err.println("Usage: java -cp target/t2c-1.0-SNAPSHOT-jar-with-dependencies.jar edu.jhu.order.t2c.dynamicd.util.TemplateMetricsTool [workdir]");
            System.exit(-1);
        }

        String templateDir = args[0];

        TemplateManager.loadTemplates(templateDir, builtinTemplates, new MiniLogger() {
            @Override
            public void logInfo(String str) {
                T2CHelper.globalLogInfo(str);
            }
        });

        runAnalysis();
    }
}
