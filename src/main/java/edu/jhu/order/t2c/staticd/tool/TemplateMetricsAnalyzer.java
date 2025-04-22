package edu.jhu.order.t2c.staticd.tool;

import edu.jhu.order.t2c.dynamicd.runtime.CheckerTemplate;
import edu.jhu.order.t2c.dynamicd.runtime.T2CHelper;
import edu.jhu.order.t2c.dynamicd.runtime.TemplateManager;

import java.util.ArrayList;
import java.util.List;

public class TemplateMetricsAnalyzer {
    public static void collectGlobalMetrics(String dirPath)
    {
        List<CheckerTemplate> generatedTemplates = new ArrayList<>();
        TemplateManager.loadTemplates(dirPath, generatedTemplates, new T2CHelper.MiniLogger() {
            @Override
            public void logInfo(String str) {
                T2CHelper.globalLogInfo(str);
            }
        });

        int succCount=0, noopCount = 0, noassertCount=0;
        for(CheckerTemplate template:generatedTemplates)
        {
            if(template.warnings.size()==0)
            {
                succCount++;
                T2CHelper.getInstance().testwopListLog(template.templateSource);
            }
            else{
                if(template.warnings.contains(CheckerTemplate.TemplateWarning.NO_TRIGGERING_OP))
                    noopCount++;
                else
                    T2CHelper.getInstance().testwopListLog(template.templateSource);

                if(template.warnings.contains(CheckerTemplate.TemplateWarning.NO_VALID_ASSERTION))
                    noassertCount++;
            }
        }

        T2CHelper.globalLogInfo("Generated "+generatedTemplates.size()+" templates in total."
                +" Including "+succCount+" healthy templates and "+(generatedTemplates.size()-succCount)
                + " unhealthy ones. Unhealthy ones breakdowns: "
                + "\n\t NO_TRIGGERING_OP: "+noopCount
                + "\n\t NO_VALID_ASSERTION: "+noassertCount);
        T2CHelper.globalLogInfo("(Note that all templates in "+ dirPath
                +" will be automatically analyzed.");
        T2CHelper.globalLogInfo("Available ratio is "
                +(succCount*1.0/(generatedTemplates.size()-succCount))*100
                + "%. ");

    }

    public static void main(String[] args) {
        if(args.length<1)
        {
            System.err.println("ERROR: Missing args! Abort.");
            for(String arg: args)
                System.err.println(arg);
            System.exit(-1);
        }

        collectGlobalMetrics(args[0]);
        System.out.println("Finished. Details are dumped in "+T2CHelper.getInstance().globalLogFileName);
    }
}
