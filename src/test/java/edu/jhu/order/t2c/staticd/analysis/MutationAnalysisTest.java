package edu.jhu.order.t2c.staticd.analysis;

import edu.jhu.order.t2c.staticd.T2CTest;
import edu.jhu.order.t2c.staticd.algorithm.MutationTracker;
import edu.jhu.order.t2c.staticd.cases.MutationSample;
import edu.jhu.order.t2c.staticd.unit.Mutation;
import java.util.List;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


/**
 * test if we can dump correct mutations
 */
public class MutationAnalysisTest extends T2CTest {

    /**
     * test the simplest case
     */
    @Test
    public void testSimple() {

        MutationTracker mutationTracker = new MutationTracker();
        MutationTracker.TRACK_PRIM_TYPE_MUTATION = false;
        MutationTracker.USE_FIELD_WHITE_LIST = false;
        MutationTracker.USE_FIELD_BLACK_LIST = false;

        List<Mutation> mutations = mutationTracker
                .scan(helper.getBody(MutationSample.class.getCanonicalName(), "testSimple"));

        int expected_size = 5;
        Assert.assertTrue(
                "mutation size mismatch! Expected: " + expected_size + ", Actual: " + mutations
                        .size(), mutations.size() == expected_size);


    }

    /**
     * test for primtype
     */
    @Test
    public void testPrimtype() {

        MutationTracker mutationTracker = new MutationTracker();
        MutationTracker.TRACK_PRIM_TYPE_MUTATION = true;
        MutationTracker.USE_FIELD_WHITE_LIST = false;
        MutationTracker.USE_FIELD_BLACK_LIST = false;

        List<Mutation> mutations = mutationTracker
                .scan(helper.getBody(MutationSample.class.getCanonicalName(), "testPrimtype"));

        int expected_size = 3;
        Assert.assertTrue(
                "mutation size mismatch! Expected: " + expected_size + ", Actual: " + mutations
                        .size(), mutations.size() == expected_size);


    }

    /**
     * test after the field is passed to another reference, whether the mutation can still be tracked
     */
    @Test
    public void testRecursive() {

        MutationTracker mutationTracker = new MutationTracker();
        MutationTracker.TRACK_PRIM_TYPE_MUTATION = false;
        MutationTracker.USE_FIELD_WHITE_LIST = false;
        MutationTracker.USE_FIELD_BLACK_LIST = false;

        List<Mutation> mutations = mutationTracker
                .scan(helper.getBody(MutationSample.class.getCanonicalName(), "testRecursive"));

        int expected_size = 2;
        Assert.assertTrue(
                "mutation size mismatch! Expected: " + expected_size + ", Actual: " + mutations
                        .size(), mutations.size() == expected_size);


    }

    /**
     * test whether mutations to an inner collection of a collection-style member would be tracked
     */
    @Test
    public void testNestedCollection() {

        MutationTracker mutationTracker = new MutationTracker();
        MutationTracker.TRACK_PRIM_TYPE_MUTATION = false;
        MutationTracker.USE_FIELD_WHITE_LIST = false;
        MutationTracker.USE_FIELD_BLACK_LIST = false;

        List<Mutation> mutations = mutationTracker
                .scan(helper.getBody(MutationSample.class.getCanonicalName(), "testNestedCollection"));

        int expected_size = 2;
        Assert.assertTrue(
                "mutation size mismatch! Expected: " + expected_size + ", Actual: " + mutations
                        .size(), mutations.size() == expected_size);


    }

    /**
     * test if we pass the data field to another function, we should still be able to track such mutations
     */
    @Ignore
    @Test
    public void testInterProcedureCall() {

        MutationTracker mutationTracker = new MutationTracker();
        MutationTracker.TRACK_PRIM_TYPE_MUTATION = false;
        MutationTracker.USE_FIELD_WHITE_LIST = false;
        MutationTracker.USE_FIELD_BLACK_LIST = false;

        List<Mutation> mutations = mutationTracker
                .scan(helper.getBody(MutationSample.class.getCanonicalName(), "testInterProcedureCall"));

        int expected_size = 1;
        Assert.assertTrue(
                "mutation size mismatch! Expected: " + expected_size + ", Actual: " + mutations
                        .size(), mutations.size() == expected_size);


    }


    /**
     * test if we can correctly support another type of assignments: AtomicReferenceFieldUpdater
     */
    @Test
    public void testAtomicReferenceFieldUpdater() {

        MutationTracker mutationTracker = new MutationTracker();
        MutationTracker.TRACK_PRIM_TYPE_MUTATION = false;
        MutationTracker.USE_FIELD_WHITE_LIST = false;
        MutationTracker.USE_FIELD_BLACK_LIST = false;

        List<Mutation> mutations = mutationTracker
                .scan(helper.getBody(MutationSample.class.getCanonicalName(), "testAtomicReferenceFieldUpdater"));

        int expected_size = 1;
        Assert.assertTrue(
                "mutation size mismatch! Expected: " + expected_size + ", Actual: " + mutations
                        .size(), mutations.size() == expected_size);


    }

    /**
     * test if we can correctly support another type of assignments: AtomicReferenceFieldUpdater
     */
    @Test
    public void testArrayWrite() {

        MutationTracker mutationTracker = new MutationTracker();
        MutationTracker.TRACK_PRIM_TYPE_MUTATION = true;
        MutationTracker.USE_FIELD_WHITE_LIST = false;
        MutationTracker.USE_FIELD_BLACK_LIST = false;

        List<Mutation> mutations = mutationTracker
                .scan(helper.getBody(MutationSample.class.getCanonicalName(), "testArrayWrite"));

        int expected_size = 1;
        Assert.assertTrue(
                "mutation size mismatch! Expected: " + expected_size + ", Actual: " + mutations
                        .size(), mutations.size() == expected_size);


    }
}