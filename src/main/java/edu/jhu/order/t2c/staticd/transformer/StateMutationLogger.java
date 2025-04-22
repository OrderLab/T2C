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

import edu.jhu.order.t2c.staticd.algorithm.MutationTracker;
import edu.jhu.order.t2c.staticd.analysis.PhaseInfo;
import edu.jhu.order.t2c.staticd.unit.Mutation;
import edu.jhu.order.t2c.staticd.util.SootUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;

import java.util.List;
import java.util.Map;

/**
 * A simple analysis to count method number
 */
public class StateMutationLogger extends SceneTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(StateMutationLogger.class);

    public static final PhaseInfo PHASE_INFO = new PhaseInfo("wjtp", "mutationlogger",
            "Log state mutation for analysis", true, false);

    protected void internalTransform(String phaseName, Map<String, String> options) {
        MutationTracker.init();

        int totalMutation = 0;

        for (SootClass c : Scene.v().getApplicationClasses()) {
            if (c.getName().contains("T2CHelper")) {
                //we don't want to cause a recursive call here (stackoverflow)
                //this happens because right now we already pre-installed helper in zookeeper
                //later we would inject helper at end of execution so no such problem any more
                continue;
            }

            Scene.v().loadClassAndSupport(c.getName());
            List<SootMethod> methods = c.getMethods();
            for (SootMethod method : methods) {
                if (!method.hasActiveBody()) {
                    continue;
                }

                //we encounter some verifier errors when parsing cassandra class, e.g.
                //java.util.concurrent.ExecutionException: java.lang.VerifyError:
                //  (class: org/apache/cassandra/io/compress/CompressionMetadata, method: readChunkOffsets
                //  signature: (Ljava/io/DataInput;)Lorg/apache/cassandra/io/util/Memory;)
                //   Accessing value from uninitialized register 4
                //when checking the class file, it shows this function (could not be decompiled)
                //solution: I tried to skip here, but it seems more like a soot issue
                // later I just did what I did in the autowatchdog, replace with a unprocessed one
                //if(c.getName().equals("org.apache.cassandra.io.compress.CompressionMetadata") &&
                //        method.getName().equals("readChunkOffsets"))
                //    continue;

                Body body = method.retrieveActiveBody();

                MutationTracker tracker = new MutationTracker();
                List<Mutation> mutations = tracker.scan(body);

                //for now we just inject a printf for debugging, later we would feed these to
                // in-mem database to transform
                LOG.debug("MUTATION METHOD: "+SootUtils.getMethodSignature(method));
                for (Mutation mutation : mutations) {
                    body.getUnits().insertBefore(
                            SootUtils.getPrintfStmt(body.getUnits(), mutation.toString()),
                            mutation.instrumentStmt);
                    totalMutation++;
                    LOG.debug("MUTATION: "+mutation.toString());
                }
            }
        }
        LOG.info("Total mutation:" + totalMutation);

        MutationTracker.dumpStats();
    }


}

