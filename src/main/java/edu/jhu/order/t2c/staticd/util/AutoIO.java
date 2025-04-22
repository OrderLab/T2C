package edu.jhu.order.t2c.staticd.util;

/*
 *  @author Ziyan Wang <zywong159@gmail.com>
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import soot.SourceLocator;

public class AutoIO {

    //some random output useful for development
    public static final String RANDOM_FILE = "t2c-random";

    private boolean initialized;
    private String fileDir;
    private PrintWriter randomWriter;

    private static AutoIO instance;

    private AutoIO() {
        initialized = false;
    }

    public static AutoIO getInstance() {
        if (instance == null) {
            instance = new AutoIO();
        }
        return instance;
    }

    public boolean initialize() {
        if (!initialized) {
            // The output dir will only be configured properly after Soot parses the
            // arguments. So we should not putting this code region into a static initializer.
            fileDir = SourceLocator.v().getOutputDir();
            try {
                randomWriter = createPrintWriter(fileDir, RANDOM_FILE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            initialized = true;
        }
        return true;
    }

    public boolean closeAll() {
        if (randomWriter != null) {
            randomWriter.close();
        }
        return true;
    }

    private PrintWriter createPrintWriter(String fileDir, String fileName)
            throws FileNotFoundException {
        File file = new File(fileDir, fileName);
        file.getParentFile().mkdirs();
        return new PrintWriter(new FileOutputStream(file, true), true);
    }

    //generally the usage of this function should be cleaned up after testing
    public void writeToRandomWriter(String s) {
        randomWriter.println(s);
    }
}