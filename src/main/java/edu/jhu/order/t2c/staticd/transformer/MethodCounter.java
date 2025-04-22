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
package edu.jhu.order.t2c.staticd.transformer;

import edu.jhu.order.t2c.staticd.analysis.PhaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;

import java.util.List;
import java.util.Map;

/**
 * A simple analysis to count method number
 */
public class MethodCounter extends SceneTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(MethodCounter.class);

    public static final PhaseInfo PHASE_INFO = new PhaseInfo("wjtp", "countmethod",
            "Count number of methods in a subject software", true, false);

    protected void internalTransform(String phaseName, Map<String, String> options) {
        for (SootClass c : Scene.v().getApplicationClasses()) {
            List<SootMethod> methods = c.getMethods();
            for (SootMethod method : methods) {
                LOG.info(c.getName()+":"+method.getName());
            }
        }
    }
}

