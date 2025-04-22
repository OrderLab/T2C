package edu.uva.liftlab.hbase;

import junit.framework.TestCase;
import org.apache.hadoop.hbase.util.Bytes;

public class UtilTest extends TestCase {

    public void testSplitGen() {
        byte[][] result = Util.splitGen(9999, 10*20);
        assertEquals(Bytes.toString(Bytes.toBytes(1044)), Bytes.toString(result[0]));
        assertEquals(Bytes.toString(Bytes.toBytes(1089)), Bytes.toString(result[1]));
        assertEquals(Bytes.toString(Bytes.toBytes(1134)), Bytes.toString(result[2]));
        assertEquals(Bytes.toString(Bytes.toBytes(1179)), Bytes.toString(result[3]));
        assertEquals(Bytes.toString(Bytes.toBytes(9999)), Bytes.toString(result[(result.length)-1]));
    }

    public void testSplitGenPrefix() {
        byte[][] result = Util.splitGen(9999, 10*20, "user");
        assertEquals(Bytes.toString(Bytes.toBytes("user"+1044)), Bytes.toString(result[0]));
        assertEquals(Bytes.toString(Bytes.toBytes("user"+1089)), Bytes.toString(result[1]));
        assertEquals(Bytes.toString(Bytes.toBytes("user"+1134)), Bytes.toString(result[2]));
        assertEquals(Bytes.toString(Bytes.toBytes("user"+1179)), Bytes.toString(result[3]));
        assertEquals(Bytes.toString(Bytes.toBytes("user"+9999)), Bytes.toString(result[(result.length)-1]));
    }
}