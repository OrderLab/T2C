package edu.uva.liftlab.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import junit.framework.TestCase;
public class JepsenT2CTest extends TestCase {
    JepsenT2C csClient;

    public void setUp() throws Exception {
        super.setUp();
        csClient = new JepsenT2C();
        csClient.populate(3000);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if(csClient!=null){
            csClient.close();
        }
    }

    public void testPopulate() {
        csClient.populate(3000);
        ResultSet rs =  csClient.clusterSession.execute("SELECT * FROM usertable");
        assertEquals(3000, rs.all().size());
    }


    public void testRead() throws Exception {
        String retVal = csClient.read("node0", 1500, 8);
        assertNotNull(retVal);
    }

    public void testWrite() throws Exception {
        csClient.update("node0", 1500);
    }
}