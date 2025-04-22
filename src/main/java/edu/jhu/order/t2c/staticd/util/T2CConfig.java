/*
 *  @author Chang Lou <chlou@jhu.edu>
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
package edu.jhu.order.t2c.staticd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import edu.jhu.order.t2c.dynamicd.runtime.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * global config loader
 *
 * TODO: this class is ugly and fragile.
 *
 */
public class T2CConfig {
    private static final Logger LOG = LoggerFactory.getLogger(T2CConfig.class);

    static final String CONFIG_DIR_PROPERTY = "t2c.conf.dir";
    static final String DEFAULT_CONFIG_DIR = "conf";
    static final String CONFIG_FILENAME = "t2c.prop";

    /* Keys for the static config parameters */
    public static final String TEST_KEY = "test";
    public static final String TEST_NAME_KEY = "test_name";
    public static final String TEST_METHOD_KEY = "test_method";
    public static final String CONF_KEY = "conf";

    /* Default settings for the static config */
    public static final boolean TEST_DEFAULT = false;

    public boolean test;
    public String test_name;
    public String test_method;
    public String conf;

    // if the config file has been loaded already;
    private boolean loaded;

    private static T2CConfig instance = null;

    public static T2CConfig getInstance() {
        if (instance == null) {
            instance = new T2CConfig();
        }
        return instance;
    }
    private T2CConfig() {
        loaded = false;

        // set the settings to default before loading
        test = TEST_DEFAULT;
    }

    /**
     * Load the config file and initialize the config instance.
     *
     * @return
     */
    public boolean load() {
        if (loaded)
            return true;
        InputStream input = null;
        String conf_dir = System.getProperty(CONFIG_DIR_PROPERTY, DEFAULT_CONFIG_DIR);
        File conf_file = new File(conf_dir, CONFIG_FILENAME);
        if (!conf_file.exists()) {
            LOG.error(("Could not find config file"));
            return false;
        }
        Properties prop = new Properties();
        try {
            input = new FileInputStream(conf_file);
            // load a properties file
            prop.load(input);
            LOG.info("T2CConfig loaded.");
            LOG.info(prop.toString());
            //prop.list(System.out);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        parseProperties(prop);
        // done loading
        loaded = true;
        return true;
    }

    /**
     * Given a list of properties, convert them to the T2C config parameter values.
     *
     * @param prop
     */
    public void parseProperties(Properties prop) {
        if (prop == null)
            return;
        String val = prop.getProperty(TEST_KEY);
        // only override default if the setting is present
        if (val != null)
            test = propToBoolean(TEST_KEY, val);

        val = prop.getProperty(TEST_NAME_KEY);
        if (val != null)
            test_name = (val);

        val = prop.getProperty(TEST_METHOD_KEY);
        if (val != null)
            test_method = (val);

        val = prop.getProperty(CONF_KEY);
        if (val != null)
        {
            conf = (val);
            System.setProperty("conf",conf);

            //if config file specified, load it
            ConfigManager.initConfig();
        }
    }

    /**
     * Convert a string property value into boolean.
     *
     * @param key
     * @param val
     * @return
     */
    private static boolean propToBoolean(String key, String val) {
        if (val.equalsIgnoreCase("true"))
            return true;
        else if (val.equalsIgnoreCase("false"))
            return false;
        throw new IllegalArgumentException("invalid setting for config " + key + ": " + val);
    }

    /**
     * Convert a string property value into int.
     *
     * @param key
     * @param val
     * @return
     */
    private static int propToInt(String key, String val) {
        try {
            int ret = Integer.parseInt(val);

            return ret;
        }catch (Exception ex)
        {
            throw new IllegalArgumentException("invalid setting for config " + key + ": " + val);
        }
    }
}