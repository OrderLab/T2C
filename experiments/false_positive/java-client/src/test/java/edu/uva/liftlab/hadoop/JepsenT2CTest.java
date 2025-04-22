package edu.uva.liftlab.hadoop;

import junit.framework.TestCase;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.IOException;

public class JepsenT2CTest extends TestCase {
    JepsenT2C client;

    public void setUp() throws Exception {
        super.setUp();
        client = new JepsenT2C();
    }

    public void tearDown() throws Exception {
        if(client!=null){
            client.close();
        }
    }

    public void testPopulate() throws IOException {
//        client.populate();
        RemoteIterator<LocatedFileStatus> files =  client.fileSysMaster.listFiles(new Path("/benchmarks"), true);
        int count = 0;
        while (files.hasNext()){
            count+=1;
            files.next();
        }
        assertEquals(client.numFiles, count);
    }

    public void testRead() throws IOException {
        int totalExceptions = client.read("lift11",50);
        assertTrue(totalExceptions == 0);
    }

    public void testWrite() throws IOException {
        int totalExceptions = client.write("lift11" ,50);
        assertTrue(totalExceptions == 0);
    }
}