package edu.jhu.order.t2c.staticd.cases;

import org.junit.*;
import java.lang.Object;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

public class AssertionSampleZK {

    class DisconnectableZooKeeper
    {
        DisconnectableZooKeeper()
        {

        }

        void create(String a, byte[] b, int c, int d)
        {
            return;
        }

        Object exists(String a, boolean b)
        {
            return new Object();
        }
    }

    DisconnectableZooKeeper createClient(int TIMEOUT)
    {
        return new DisconnectableZooKeeper();
    }

    @Test
    public void testSimple() throws Exception {

        final int TIMEOUT = 5000;
        DisconnectableZooKeeper zk = createClient(TIMEOUT);
        zk.create("/stestaa", new byte[0], 1, 1);
        Assert.assertTrue(zk.exists("/stestaa", false) != null);
        //Assert.assertNotNull(zk.exists("/stestaa", false));
    }

    public static void main(String[] args) {
        AssertionSample sample = new AssertionSample();
        try {
            if (args[0].equals("testSimple"))
                sample.testSimple();
            else
                throw new RuntimeException("illgeal arguments!");
        } catch (Exception ex) {
            throw new RuntimeException(ex.getCause().toString());
        }
    }
}
