package edu.jhu.order.t2c.staticd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.jhu.order.t2c.staticd.analysis.AnalysisManager;
import edu.jhu.order.t2c.staticd.option.OptionParser;
import edu.jhu.order.t2c.staticd.option.T2COptions;
import soot.options.Options;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class T2CTest {

    private static final Logger LOG = LoggerFactory.getLogger(T2CTest.class);

    protected static T2CMain analyzer;
    protected static TestHelper helper;

    @BeforeClass
    public static void setup() throws Exception {
        // we execute main analyzer to load our test recipes into sootclass
        // however, some info would be missing after the whole process is finished,
        // e.g. java.lang.RuntimeException: No method source set for method xxx
        // we either save the info when we still have it, or we transform test case as a transformer
        // and run it, we chose the first one due to implementation complexity
        helper = new TestHelper();

        String args[] = {"-o", "sootTestOutput", "-a", TestHelper.PHASE_INFO.getFullName(),
                "-i", "target/test-classes", "-p", "jb", "use-original-names:true"};

        // TestHelper is a test-specific transformer and thus not registered in the main program.
        // Explicitly register it here.

        AnalysisManager.getInstance().registerAnalysis(helper, TestHelper.PHASE_INFO);

        OptionParser parser = new OptionParser();
        T2COptions options = parser.parse(args);

        //force to enable validate
        Options.v().set_validate(options.keepValidate());

        analyzer = new T2CMain(options);

        assertTrue(analyzer.initialize());

        // The enabled analyses at this point must be only containing the TestHelper
        assertEquals(AnalysisManager.getInstance().enabledAnalyses().size(), 1);
        assertTrue(AnalysisManager.getInstance().enabledAnalyses().contains(TestHelper.PHASE_INFO.getFullName()));

        // Run the TestHelper analysis to retrieve the method body correctly.
        assertTrue(analyzer.run());
        LOG.info("Finished setting up the test case");
    }

    @AfterClass
    public static void teardown() {
        analyzer.reset();
        LOG.info("Tearing down the test case");
    }
}
