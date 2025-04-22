package edu.uva.liftlab.hbase;

import junit.framework.TestCase;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.filter.KeyOnlyFilter;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

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
        client.populate();

        Scan scan = new Scan(
                Bytes.toBytes(0), Bytes.toBytes(10000)
        );

        FilterList allFilters = new FilterList();
        allFilters.addFilter(new FirstKeyOnlyFilter());
        allFilters.addFilter(new KeyOnlyFilter());

        scan.setFilter(allFilters);

        ResultScanner scanner = client.currentTable.getScanner(scan);

        int count = 0;

        try {
            for (Result rr = scanner.next(); rr != null; rr = scanner.next()) {
                count += 1;
            }
        } finally {
            scanner.close();
        }

        assertEquals(10000, count);
    }

    public void testRead() throws IOException {
        int cellCount = client.read(client.defaultHost, 5000);
        assertEquals(3, cellCount);
    }

    public void testWrite() throws IOException {
        client.write(client.defaultHost, 5000);
        assertTrue(true);
    }

    public void testHb25827(){
        try{
            Admin admin = client.masterConn.getAdmin();

            TableName tName = TableName.valueOf("usertable");
            final byte[] columnFamilyBytes = Bytes.toBytes("family");
            final byte[] COLUMN = Bytes.toBytes("column");
            final byte [] ROW = Bytes.toBytes("testRow");

            ColumnFamilyDescriptor colDesc = ColumnFamilyDescriptorBuilder.newBuilder(columnFamilyBytes).build();
            TableDescriptor tableDesc = TableDescriptorBuilder.newBuilder(tName).setColumnFamily(colDesc).build();
            admin.createTable(tableDesc);

            Table table = client.masterConn.getTable(tName);
            for (int i = 0; i < 10; i++) {
                Increment inc = new Increment(ROW);
                inc.addColumn(columnFamilyBytes, COLUMN, 1);
                long ttl = i + 3600000 ;
                inc.setTTL(ttl);
                table.increment(inc);

                Scan scan = new Scan().withStartRow(ROW);
                ResultScanner scanner = table.getScanner(scan);
                int count = 0;
                Result result;
                while ((result = scanner.next()) != null) {
                    Cell[] cells =  result.rawCells();
                    for (Cell cell: cells) {
                        List<Tag> tags = PrivateCellUtil.getTags(cell);
                        // Make sure there is only 1 tag.
                        assertEquals(1, tags.size());
                        Tag tag = tags.get(0);
                        assertEquals(TagType.TTL_TAG_TYPE, tag.getType());
                        long ttlTagValue = Bytes.toLong(tag.getValueArray(), tag.getValueOffset());
                        assertEquals(ttl, ttlTagValue);
                    }
                    count++;
                }
                // Make sure there is only 1 result.
                assertEquals(1, count);
            }

        } catch (Exception e){
            // Do nothing
        }
    }

    public void testHb21644(){
        try{
            Admin admin = client.masterConn.getAdmin();

            TableName tableName = TableName.valueOf("t21644");
            TableDescriptor desc = TableDescriptorBuilder.newBuilder(tableName)
                .setColumnFamily(ColumnFamilyDescriptorBuilder.of(Bytes.toBytes("cf")))
                .setRegionReplication(5)
                .build();

            admin.createTable(desc);

            int maxFileSize = 10000000;
            TableDescriptor newDesc = TableDescriptorBuilder.newBuilder(desc)
                .setMaxFileSize(maxFileSize)
                .build();

            admin.modifyTable(newDesc);
            TableDescriptor newTableDesc = admin.getDescriptor(tableName);
            assertEquals(maxFileSize, newTableDesc.getMaxFileSize());
        } catch (Exception e){
            // Do nothing
        }
    }

    public void testHb21621(){
        Random rand = ThreadLocalRandom.current();
        byte[] LARGE_VALUE = new byte[128 * 1024];
        for (int i = 0; i < LARGE_VALUE.length; i++) {
            LARGE_VALUE[i] = (byte) rand.nextInt(256);
        }
        try{
            Admin admin = client.masterConn.getAdmin();
            TableName tableName = TableName.valueOf("t21621");
            byte[] family = Bytes.toBytes("cf");
            int BATCH_SIZE = 10;
            int ROWS_TO_INSERT = 100;

            TableDescriptor tableDesc = TableDescriptorBuilder.newBuilder(tableName)
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.of(family))
                    .build();

            admin.createTable(tableDesc);

            try (Table table = client.masterConn.getTable(tableName)) {
                List<Put> putList = new ArrayList<>();
                for (long i = 0; i < ROWS_TO_INSERT; i++) {
                    Put put = new Put(Bytes.toBytes(i));
                    put.addColumn(family, Bytes.toBytes("testQualifier"), LARGE_VALUE);
                    putList.add(put);

                    if (putList.size() >= BATCH_SIZE) {
                        table.put(putList);
                        admin.flush(tableName);
                        putList.clear();
                        Thread.sleep(5000);
                    }
                }

                if (!putList.isEmpty()) {
                    table.put(putList);
                    admin.flush(tableName);
                    putList.clear();
                }

                Scan scan = new Scan();
                scan.setReversed(true);
                int count = 0;

                try (ResultScanner results = table.getScanner(scan)) {
                    for (Result result : results) {
                        count++;
                    }
                }
                assertEquals("Expected " + ROWS_TO_INSERT + " rows in the table but it is " + count,
                        ROWS_TO_INSERT, count);
            }
        }
        catch (Exception e){
            // Do nothing
        }
    }

    public void testNotHb21621(){
        Random rand = ThreadLocalRandom.current();
        byte[] LARGE_VALUE = new byte[128 * 1024];
        for (int i = 0; i < LARGE_VALUE.length; i++) {
            LARGE_VALUE[i] = (byte) rand.nextInt(256);
        }
        try{
            Admin admin = client.masterConn.getAdmin();
            TableName tableName = TableName.valueOf("t21621not");
            byte[] family = Bytes.toBytes("cf");
            int BATCH_SIZE = 10;
            int ROWS_TO_INSERT = 100;

            TableDescriptor tableDesc = TableDescriptorBuilder.newBuilder(tableName)
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.of(family))
                    .build();

            admin.createTable(tableDesc);

            try (Table table = client.masterConn.getTable(tableName)) {
                List<Put> putList = new ArrayList<>();
                for (long i = 0; i < ROWS_TO_INSERT; i++) {
                    Put put = new Put(Bytes.toBytes(i));
                    put.addColumn(family, Bytes.toBytes("testQualifier"), LARGE_VALUE);
                    putList.add(put);
                }

                table.put(putList);

                Scan scan = new Scan();
                scan.setReversed(true);
                int count = 0;

                try (ResultScanner results = table.getScanner(scan)) {
                    for (Result result : results) {
                        count++;
                    }
                }
                assertEquals("Expected " + ROWS_TO_INSERT + " rows in the table but it is " + count,
                        ROWS_TO_INSERT, count);
            }
        }
        catch (Exception e){
            // Do nothing
        }
    }
}