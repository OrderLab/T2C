package edu.uva.liftlab.hbase;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class JepsenT2C {
    Configuration masterConfig = HBaseConfiguration.create();
    Connection masterConn;
    ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>();

    Table currentTable;

    String defaultHost = "lift11";
    TableName tName = TableName.valueOf("usertable");
    byte[] columnFamilyBytes = Bytes.toBytes("family");
    int rowCount = 10000;
    int serverCount = 5;

    public JepsenT2C() throws IOException {
        masterConfig.set("hbase.rootdir", "hdfs://"+defaultHost+":8020/hbase");
        masterConfig.set("hbase.zookeeper.quorum", defaultHost);
        UserGroupInformation.setLoginUser(UserGroupInformation.createRemoteUser("vqx2dc"));
        System.setProperty("HADOOP_USER_NAME", "vqx2dc");
        System.setProperty("hadoop.home.dir", "/localtmp/detection/hadoop/hadoop-dist/target/hadoop-3.1.2");
        masterConn = ConnectionFactory.createConnection(masterConfig);
//        String path = this.getClass()
//                .getClassLoader()
//                .getResource("hbase-site.xml")
//                .getPath();
//        config.addResource(new Path(path));

        try {
            HBaseAdmin.available(masterConfig);
        } catch (IOException e) {
            System.out.println("HBase is not running." + e.getMessage());
            throw e;
        }

        Admin admin = masterConn.getAdmin();

        if(admin.tableExists(tName)){
            this.currentTable = masterConn.getTable(tName);
        }

    }

    public void close() throws IOException {
        if(masterConn!=null && !masterConn.isClosed()) {
            masterConn.close();
        }

    }

    public Connection getHandler(String host) throws IOException {
        synchronized (this) {
            if(!connections.containsKey(host)){
                Configuration config = HBaseConfiguration.create();
                config.set("hbase.rootdir", "hdfs://"+host+":8020/hbase");
                config.set("hbase.zookeeper.quorum", defaultHost);
                connections.put(host, ConnectionFactory.createConnection(config));
            }
        }
        return connections.get(host);
    }

    public void populate() throws IOException {
        try{
            Admin admin = masterConn.getAdmin();
            if(admin.tableExists(tName)){
                admin.deleteTable(tName);
            }

            ColumnFamilyDescriptor colDesc = ColumnFamilyDescriptorBuilder.newBuilder(columnFamilyBytes).build();
            TableDescriptor tableDesc = TableDescriptorBuilder.newBuilder(tName).setColumnFamily(colDesc).build();
            admin.createTable(tableDesc, Util.splitGen(rowCount-1   , 10*serverCount));
        } catch(Exception e){
            //
        }

        this.currentTable = masterConn.getTable(tName);
        for (int i = 0; i < rowCount; i++) {
            write(defaultHost,i);
        }
    }

    public int read(String host, int key) throws IOException {
        Table localTable = getHandler(host).getTable(tName);
        int cellCount = 0;

        Get g = new Get(Bytes.toBytes(key));
        g.addFamily(columnFamilyBytes);
        Result r = localTable.get(g);

        if (r.isEmpty()){
            return cellCount;
        }

        while (r.advance()){
            cellCount+=1;
        }

        return cellCount;
    }

    public void write(String host, int key) throws IOException {
        Table localTable = getHandler(host).getTable(tName);
        Put p = new Put(Bytes.toBytes(key));
        for (int i = 0; i < 3; i++) {
            p.addColumn(columnFamilyBytes, Bytes.toBytes("field"+i), Bytes.toBytes(RandomStringUtils.randomAlphanumeric(10)));
        }
        localTable.put(p);
    }
}
