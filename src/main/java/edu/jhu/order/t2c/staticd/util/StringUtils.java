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
package edu.jhu.order.t2c.staticd.util;

import java.util.Collection;

/**
 * String manipulation utilities
 */
public class StringUtils {

    /**
     * Concatenate  an array of elements into a string separated by separator
     * @param separator separator between each element
     * @param elements elements to be joined
     * @return
     */
    public static String join(String separator, final String[] elements) {
        if (elements == null) return null;
        final StringBuilder buf = new StringBuilder(elements.length * 16);
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                buf.append(separator);
            }
            buf.append(elements[i]);
        }
        return buf.toString();
    }

    /**
     * Concatenate  a collection of elements into a string separated by separator
     * @param separator separator between each element
     * @param elements elements to be joined
     * @return
     */
    public static String join(String separator, final Collection<String> elements) {
        if (elements == null) return null;
        final StringBuilder buf = new StringBuilder(elements.size() * 16);
        boolean first = true;
        for (String element : elements) {
            if (first)
                first = false;
            else
                buf.append(separator);
            buf.append(element);
        }
        return buf.toString();
    }
}