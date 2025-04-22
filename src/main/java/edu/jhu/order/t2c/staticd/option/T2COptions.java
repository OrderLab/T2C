/*
 *  @author Ryan Huang <huang@cs.jhu.edu>
 *
 *  The T2C Project
 *
 *  Copyright (c) 2018, Johns Hopkins University - Order Lab.
 *      All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.jhu.order.t2c.staticd.option;

import edu.jhu.order.t2c.staticd.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Parsed command line arguments for the tool.
 */
public class T2COptions {
    private boolean is_help = false;
    private boolean is_soot_help = false;
    private boolean keep_debug = false;
    private boolean keep_validate = true;
    private boolean is_whole_program = true;
    private boolean gen_executable = false;
    private boolean no_output = false;
    private boolean output_jar = true;
    private boolean list_analysis = true;

    private String class_path;
    private List<String> input_list;
    private String main_class;
    //a distributed system like HDFS can have multiple main classes as entries,
    // which would be needed in long running scanning phase
    private List<String> secondary_main_classes;
    private String user_validator_file;
    private String output_dir;
    //mutations happened in this package is not interesting to us
    private String excluded_package;
    //classes used on the client side should be excluded, which means we would recursively
    //add all data structures defined in the client side entry to blacklist
    private String client_side_entry;
    //used to differ whether a class belongs to this system
    private String system_package_prefix;
    private String[] analyses;
    private String[] classes;
    private Map<String, List<String>> phase_options;
    private String[] args;
    // settings that override the values from the config file
    private Properties override_props;

    private static T2COptions instance = new T2COptions();
    public static T2COptions getInstance() {
        return instance;
    }

    private T2COptions() {

    }

    public boolean isHelp() {
        return is_help;
    }

    void setIsHelp(boolean is_help) {
        this.is_help = is_help;
    }

    public boolean isSootHelp() {
        return is_soot_help;
    }

    void setIsSootHelp(boolean is_soot_help) {
        this.is_soot_help = is_soot_help;
    }

    public boolean keepDebug() {
        return keep_debug;
    }

    public boolean keepValidate() {
        return keep_validate;
    }

    void setKeepDebug(boolean keep_debug) {
        this.keep_debug = keep_debug;
    }

    public boolean isWholeProgram() {
        return is_whole_program;
    }

    void setIsWholeProgram(boolean is_whole_program) {
        this.is_whole_program = is_whole_program;
    }

    public boolean genExecutable() {
        return gen_executable;
    }

    void setGenExecutable(boolean gen_executable) {
        this.gen_executable = gen_executable;
    }

    public boolean noOutput() {
        return no_output;
    }
    void setNoOutput(boolean no_output) {
        this.no_output = no_output;
    }

    public boolean isOutputJar() {
        return output_jar;
    }

    void setOutputJar(boolean output_jar) {
        this.output_jar = output_jar;
    }

    public boolean listAnalysis() {
        return list_analysis;
    }

    void setListAnalysis(boolean list_analysis) {
        this.list_analysis = list_analysis;
    }

    /**
     * Get the class path to use
     * @return
     */
    public String getClassPath() {
        return class_path;
    }

    void setClassPath(String class_path) {
        this.class_path = class_path;
    }

    /**
     * Get the client side entry
     * @return
     */
    public String getClientSideEntry() {
        return client_side_entry;
    }

    void setClientSideEntry(String client_side_entry) {
        this.client_side_entry = client_side_entry;
    }


    public String getSystemPackagePrefix() {
        return system_package_prefix;
    }

    void setSystemPackagePrefix(String system_package_prefix) {
        this.system_package_prefix = system_package_prefix;
    }

    /**
     * Get the list of input directories or jars
     * @return
     */
    public List<String> getInputList() {
        return input_list;
    }

    void setInputList(List<String> input_list) {
        this.input_list = input_list;
    }

    public boolean isInputListEmpty() {
        return input_list == null || input_list.isEmpty();
    }

    /**
     * Get the main class to start analysis
     * @return
     */
    public String getMainClass() {
        return main_class;
    }

    void setMainClass(String main_class) {
        this.main_class = main_class;
    }

    /**
     * Get the list of secondary main classes
     * @return
     */
    public List<String> getSecondaryMainClassList() {
        return secondary_main_classes;
    }

    void setSecondaryMainClassList(List<String> secondary_main_classes) {
        this.secondary_main_classes = secondary_main_classes;
    }

    public boolean isSecondaryMainClassListEmpty() {
        return secondary_main_classes == null || secondary_main_classes.isEmpty();
    }

    /**
     * Get the output directory
     * @return
     */
    public String getOutputDir() {
        return output_dir;
    }

    void setOutputDir(String output_dir) {
        this.output_dir = output_dir;
    }

    /**
     * Get the excluded package name
     * @return
     */
    public String getExcludedPackage() {
        return excluded_package;
    }

    void setExcludedPackage(String excluded_package) {
        this.excluded_package = excluded_package;
    }

    /**
     * Get the output directory
     * @return
     */
    public String getUserValidatorFile() {
        return user_validator_file;
    }

    void setUserValidatorFile(String user_validator_file) {
        this.user_validator_file = user_validator_file;
    }

    /**
     * Get the list of analyses to execute
     * @return
     */
    public String[] getAnalyses() {
        return analyses;
    }

    void setAnalyses(String[] analyses) {
        this.analyses = analyses;
    }

    /**
     * Get the list of class names to be analyzed
     * @return
     */
    public String[] getClasses() {
        return classes;
    }

    public void setClasses(String[] classes) {
        this.classes = classes;
    }

    /**
     * Get the list of options passed to one or more Soot phases
     * @return
     */
    public Map<String, List<String>> getPhaseOptions() {
        return phase_options;
    }

    public void setPhaseOptions(Map<String, List<String>> phase_options) {
        this.phase_options = phase_options;
    }

    /**
     * Get the properties that are specified through the command line. They
     * will override the settings in the config file.
     * @return
     */
    public Properties getOverrideProperties() {
        return override_props;
    }

    public void setOverrideProperties(Properties override_props) {
        this.override_props = override_props;
    }

    /**
     * Get the list of non-optional arguments, e.g., the ones passed after -- to Soot
     * @return
     */
    public String[] getArgs() {
        return args;
    }

    void setArgs(String[] args) {
        this.args = args;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("- keep_debug? ").append(keep_debug).append("\n");
        sb.append("- whole_program? ").append(is_whole_program).append("\n");
        sb.append("- gen_executable? ").append(gen_executable).append("\n");
        sb.append("- no_output? ").append(no_output).append("\n");
        sb.append("- output_jar? ").append(output_jar).append("\n");
        sb.append("- list_analysis? ").append(list_analysis).append("\n");
        sb.append("- class_path: ").append(class_path).append("\n");
        sb.append("- client_side_entry: ").append(client_side_entry).append("\n");
        sb.append("- system_package_prefix: ").append(system_package_prefix).append("\n");
        sb.append("- input_list: ").append(StringUtils.join(",", input_list))
                .append("\n");
        sb.append("- main_class: ").append(main_class).append("\n");
        sb.append("- output_dir: ").append(output_dir).append("\n");
        sb.append("- excluded_package: ").append(excluded_package).append("\n");
        sb.append("- analyses: ").append(StringUtils.join(",", analyses)).append("\n");
        sb.append("- classes: ").append(StringUtils.join(",", classes)).append("\n");
        sb.append("- phase_options: ");
        if (phase_options != null) {
            for (Map.Entry<String, List<String>> entry : phase_options.entrySet()) {
                sb.append(entry.getKey()).append(" ").append(StringUtils.join(",",
                        entry.getValue())).append("; ");
            }
            sb.append("\n");
        } else {
            sb.append("null\n");
        }

        sb.append("- ARGS: ").append(StringUtils.join(" ", args)).append("\n");
        return sb.toString();
    }
}
