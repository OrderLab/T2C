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
package edu.jhu.order.t2c.staticd.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.jhu.order.t2c.staticd.transformer.MethodCounter;
import edu.jhu.order.t2c.staticd.transformer.SimpleJimpler;
import edu.jhu.order.t2c.staticd.transformer.StateMutationLogger;
import edu.jhu.order.t2c.staticd.transformer.TestAssertionPacker;
import edu.jhu.order.t2c.staticd.util.SootUtils;
import soot.Transform;
import soot.Transformer;

/**
 * Manage the major analyses in T2C
 */
public class AnalysisManager {
    private Map<String, Transform> analysisMap;
    private Map<String, PhaseInfo> phaseInfoMap;
    private Set<String> enabledAnalysisSet;

    // Information about all the phases available in T2C.
    // You also need to register at edu.jhu.order.t2c.staticd.T2CMain.registerAnalyses()
    private static PhaseInfo[] PHASES  = {
            MethodCounter.PHASE_INFO,
            SimpleJimpler.PHASE_INFO,
            StateMutationLogger.PHASE_INFO,
            TestAssertionPacker.PHASE_INFO

    };
    private static AnalysisManager instance;

    private AnalysisManager() {
        analysisMap = new HashMap<>();
        phaseInfoMap = new HashMap<>();
        for (PhaseInfo info : PHASES) {
            phaseInfoMap.put(info.getFullName(), info);
        }
        enabledAnalysisSet = new HashSet<>();
    }

    public static AnalysisManager getInstance() {
        if (instance == null) {
            instance = new AnalysisManager();
        }
        return instance;
    }

    public Transform getAnalysis(String name) {
        return analysisMap.get(name);
    }

    public boolean isAnalysiEnabled(String name) {
        return enabledAnalysisSet.contains(name);
    }

    public boolean enableAnalysis(String name) {
        if (phaseInfoMap.containsKey(name)) {
            // Enable only it is available
            enabledAnalysisSet.add(name);
            return true;
        }
        return false;
    }

    public Set<String> enabledAnalyses() {
        return enabledAnalysisSet;
    }

    public PhaseInfo getPhaseInfo(String name) {
        return phaseInfoMap.get(name);
    }

    public Iterator<PhaseInfo> phaseInfoIterator() {
        return phaseInfoMap.values().iterator();
    }

    public Transform registerAnalysis(Transformer analysis, PhaseInfo info) {
        Transform phase = SootUtils.addNewTransform(info.getPack(), info.getFullName(), analysis);
        analysisMap.put(info.getFullName(), phase);
        phaseInfoMap.put(info.getFullName(), info);
        return phase;
    }

    public void validateAllRegistered() {
        for (PhaseInfo info : PHASES) {
            if(!analysisMap.containsKey(info.getFullName()))
                throw new RuntimeException(info.getFullName()+" not registered! " +
                        "Should add the analysis in edu.jhu.order.t2c.staticd.T2CMain.registerAnalyses");
        }
    }
}