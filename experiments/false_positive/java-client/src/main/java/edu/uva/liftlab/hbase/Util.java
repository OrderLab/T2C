package edu.uva.liftlab.hbase;

import org.apache.hadoop.hbase.util.Bytes;

import java.util.ArrayList;
import java.util.List;

// https://stackoverflow.com/questions/28165833/how-can-i-pre-split-a-table-in-hbase
// https://stackoverflow.com/questions/40277405/split-command-in-hbase-shell
public class Util {
    public static byte[][] splitGen(int rowCount, int nSplit){
        List<byte[]> retVal = new ArrayList<>();
        for (int i = 1; i <= nSplit; i++) {
            retVal.add(Bytes.toBytes(
                    (int)Math.floor(1000+ (i * ((double) (rowCount - 1000) / nSplit)))
            ));
        }
        
        return retVal.toArray(new byte[0][]);
    }
    public static byte[][] splitGen(int rowCount, int nSplit, String prefix){
        List<byte[]> retVal = new ArrayList<>();
        for (int i = 1; i <= nSplit; i++) {
            retVal.add(Bytes.toBytes(
                    prefix+(int)Math.floor(1000+ (i * ((double) (rowCount - 1000) / nSplit)))
            ));
        }

        return retVal.toArray(new byte[0][]);
    }
}
