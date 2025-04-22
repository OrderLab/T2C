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
package edu.jhu.order.t2c.staticd;

import edu.jhu.order.t2c.staticd.analysis.PhaseInfo;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Body;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;

/**
 * the helper class is used to load recipes for test cases
 * reasons explained in T2CTest
 */
public class TestHelper extends SceneTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(TestHelper.class);

    public static final PhaseInfo PHASE_INFO = new PhaseInfo("wjtp", "testhelper",
            "store recipe info for later analyze", true, false);

    private Map<String, Map<String, Body>> bodyMap = new HashMap<>();

    protected void internalTransform(String phaseName, Map<String, String> options) {
        for (SootClass c : Scene.v().getApplicationClasses()) {
            if (c.getName().startsWith("edu.jhu.order.t2c.staticd.cases.")) {
                LOG.info("Recording class for test case " + c.getName());
                Map<String, Body> map = new HashMap<>();
                for (SootMethod m : c.getMethods()) {
                    if (m.hasActiveBody()) {
                        LOG.info("Recording method body of test method " + m.getName());
                        map.put(m.getName(), m.retrieveActiveBody());
                    }
                }
                bodyMap.put(c.getName(), map);
            }
        }
    }

    public Body getBody(String className, String methodName) {
        if (bodyMap.containsKey(className)) {
            Map<String, Body> map = bodyMap.get(className);
            if (map.containsKey(methodName)) {
                return map.get(methodName);
            }
        }
        throw new RuntimeException("cannot find needed class " + className + " " + methodName);
    }

    public void updateBody(String className, String methodName, Body body) {
        if (bodyMap.containsKey(className)) {
            Map<String, Body> map = bodyMap.get(className);
            if (map.containsKey(methodName)) {
                map.put(methodName, body);
                return;
            }
        }
        throw new RuntimeException("cannot find needed class " + className + " " + methodName);
    }

    public void recoverAllMethodBody()
    {
        for (SootClass c : Scene.v().getApplicationClasses()) {
            if (c.getName().startsWith("edu.jhu.order.t2c.staticd.cases.")) {
                Map<String, Body> map = bodyMap.get(c.getName());
                for (SootMethod m : c.getMethods()) {
                    if(map.containsKey(m.getName()))
                    {
                        LOG.info("Recovering method body of test method " + m.getName());
                        m.setActiveBody(map.get(m.getName()));
                    }
                }
            }
        }
    }
}

