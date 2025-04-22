package edu.jhu.order.t2c.dynamicd.runtime;

import edu.jhu.order.t2c.staticd.util.T2CConfig;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.util.Arrays;

public class ConfigManager {
    public static String CONFIG_FILE_NAME = "t2cconfig.properties";

    public static String SYSTEM_PACKAGE_PREFIX_KEY = "system_package_prefix";
    public static String TEST_CLASS_NAME_REGEX_KEY = "test_class_name_regex";
    public static String SPECIFIED_TEST_CLASS_LIST_KEY = "specified_test_class_list";
    public static String EXCLUDED_TEST_METHOD_LIST_KEY = "excluded_test_method_list";
    public static String OP_INTERFACE_CLASS_LIST_KEY = "op_interface_class_list";
    public static String OP_INTERFACE_METHOD_LIST = "op_interface_method_list";
    public static String JVM_ARGS_FOR_TESTS_KEY = "jvm_args_for_tests";

    public static String VERIFY_ABORT_AFTER_N_TESTS_KEY = "verify_abort_after_N_tests";
    public static String IF_ENABLE_MUTATE_KEY = "if_enable_mutate";
    public static String IF_DO_PURITY_ANALYSIS_KEY = "if_do_purity_analysis";
    
    public static String RATE_LIMIT_WHITELIST = "rate_limit_whitelist";
    public static String RATE_LIMIT_REFRESH = "rate_limit_refresh";
    public static String RATE_LIMIT_SIZE = "rate_limit_size";

    public static String SYSTEM_PATH = System.getProperty("t2c.target_system_abs_path");

    public static Configuration config;

    public static void initConfig()
    {
        //init config
        String confname = System.getProperty("conf");
        if(confname==null && T2CConfig.getInstance().conf!=null)
            CONFIG_FILE_NAME = T2CConfig.getInstance().conf;
        if(confname!=null)
            CONFIG_FILE_NAME = confname;

        try
        {
            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                    new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                            .configure(params.properties()
                                    .setFileName(CONFIG_FILE_NAME)
                                    .setListDelimiterHandler(new DefaultListDelimiterHandler(',')));
            config = builder.getConfiguration();
            String prefix = config.getString(SYSTEM_PACKAGE_PREFIX_KEY);
            String regex = config.getString(TEST_CLASS_NAME_REGEX_KEY);
            // trim double quote
            config.setProperty(JVM_ARGS_FOR_TESTS_KEY,
                config.getString(JVM_ARGS_FOR_TESTS_KEY).replaceAll("^\"|\"$", ""));
            String jvm_args = config.getString(JVM_ARGS_FOR_TESTS_KEY);
            String[] specifiedClassList = config.getStringArray(SPECIFIED_TEST_CLASS_LIST_KEY);
            String[] excludedMethodList = config.getStringArray(EXCLUDED_TEST_METHOD_LIST_KEY);
            String[] interfaceClassList = config.getStringArray(OP_INTERFACE_CLASS_LIST_KEY);
            String[] interfaceMethodList = config.getStringArray(OP_INTERFACE_METHOD_LIST);
            System.out.println(SYSTEM_PACKAGE_PREFIX_KEY+":"+prefix);
            System.out.println(TEST_CLASS_NAME_REGEX_KEY+":"+regex);
            System.out.println(JVM_ARGS_FOR_TESTS_KEY+":"+jvm_args);
            System.out.println(SPECIFIED_TEST_CLASS_LIST_KEY+":"+ Arrays.toString(specifiedClassList));
            System.out.println(EXCLUDED_TEST_METHOD_LIST_KEY+":"+Arrays.toString(excludedMethodList));
            System.out.println(OP_INTERFACE_CLASS_LIST_KEY+":"+Arrays.toString(interfaceClassList));
            System.out.println(OP_INTERFACE_METHOD_LIST+":"+Arrays.toString(interfaceMethodList));
            Boolean if_enable_mutate = config.getBoolean(IF_ENABLE_MUTATE_KEY);
            int verify_abort_after_N_tests = config.getInt(VERIFY_ABORT_AFTER_N_TESTS_KEY);
            System.out.println(VERIFY_ABORT_AFTER_N_TESTS_KEY+":"+(verify_abort_after_N_tests));
            System.out.println(IF_ENABLE_MUTATE_KEY+":"+(if_enable_mutate));
            Boolean if_do_purity_analysis = config.getBoolean(IF_DO_PURITY_ANALYSIS_KEY);
            System.out.println(IF_DO_PURITY_ANALYSIS_KEY+":"+(if_do_purity_analysis));

        }
        catch (ConfigurationException cex)
        {
            cex.printStackTrace();
            System.exit(-1);
        }
    }
}
