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
package edu.jhu.order.t2c.staticd.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class AutoResultIO {

    private String fileDir;
    private String fileName;

    public AutoResultIO(String fileDir, String fileName) {
        this.fileDir = fileDir;
        this.fileName = fileName;
    }

    /**
     * Write the collection of strings into the file.
     *
     * @param list content to write to file
     */
    public void write(Collection<String> list) {
        // write the list to file, one element per line
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileDir, fileName)));
            for (String s : list) {
                writer.write(s + "\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.format("Failed to write to %s %s", fileDir, fileName);
            e.printStackTrace();
            System.exit(1);
        }

    }

    /**
     * Read all the lines in the file and return a collection of the lines that preserve the order
     *
     * @param unique If only unique lines should be returned.
     * @return Collection of (unique) lines in the file
     */
    public Collection<String> read(boolean unique) {
        // read the list of string from file
        List<String> lines = null;
        LinkedHashSet<String> uniqueLines = null;
        if (unique) {
            uniqueLines = new LinkedHashSet<>();
        } else {
            lines = new ArrayList<>();
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(fileDir, fileName)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (unique) {
                    // If only unique lines should be returned, we should add them into a set
                    // the preserves the insertion order.
                    uniqueLines.add(line);
                } else {
                    lines.add(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.format("Failed to read from %s %s", fileDir, fileName);
            e.printStackTrace();
            System.exit(1);
        }
        if (unique) {
            return uniqueLines;
        } else {
            return lines;
        }
    }

}
