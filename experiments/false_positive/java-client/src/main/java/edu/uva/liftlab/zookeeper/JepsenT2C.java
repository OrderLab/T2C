package edu.uva.liftlab.zookeeper;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JepsenT2C {
    String defaultHost = "lift15";
    ConcurrentHashMap<String, Connection> sessions = new ConcurrentHashMap<>();

    private static final Random RANDOM = new Random();

    public JepsenT2C() {
    }

    public void init() throws IOException, InterruptedException {
//        sessions.put(defaultHost, new Connection(defaultHost));
    }

    public void close() throws InterruptedException{
        for (Map.Entry<String, Connection> session: sessions.entrySet()){
            session.getValue().close();
        }
        sessions.clear();
    }

    public void populate(int count) throws IOException, InterruptedException, KeeperException {
//        for (int i = 0; i < count; i++) {
//            create(defaultHost, String.valueOf(i));
//            read(defaultHost, String.valueOf(i));
//            Thread.sleep(50);
//        }
        for (int i = 0; i < count; i+=5) {
            create("lift11", String.valueOf(i));
            create("lift12", String.valueOf(i+1));
            create("lift13", String.valueOf(i+2));
            create("lift14", String.valueOf(i+3));
            create("lift15", String.valueOf(i+4));
            read("lift11", String.valueOf(i));
        }
    }

    public ZooKeeper getHandle(String host) throws IOException, InterruptedException {
        synchronized (this){
            if(!sessions.containsKey(host)){
                sessions.put(host, new Connection(host));
            }
        }
        return sessions.get(host).getConnection();
    }

    public String create(String host, String op) throws IOException, InterruptedException {
        ZooKeeper conn = getHandle(host);
        try{
            CreateMode mode = CreateMode.PERSISTENT;
            if(RANDOM.nextBoolean()){
                mode = CreateMode.EPHEMERAL;
            }
            // return path
            return conn.create("/node"+op, RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException e){
            return null;
        }
    }

    public Integer update(String host, String op) throws IOException, InterruptedException {
        ZooKeeper conn = getHandle(host);
        try{
            Stat stat = conn.exists("/node"+op, false);
            if(stat==null){
                create(host, op);
                read(host, op);
                return -1;
            } else {
                // return version
                return conn.setData("/node"+op, RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), -1).getVersion();
            }
        } catch (KeeperException e){
            return null;
        }
    }

    public String read(String host, String op) throws IOException, InterruptedException {
        ZooKeeper conn = getHandle(host);
        try{
            Stat stat = new Stat();
            byte[] b = conn.getData("/node"+op, null, stat);
            // return data
            return new String(b, StandardCharsets.UTF_8);
        } catch (KeeperException e) {
            return null;
        }
    }

    public Integer getChildren(String host, String op) throws IOException, InterruptedException {
        ZooKeeper conn = getHandle(host);
        try{
            List<String> children = conn.getChildren("/node"+op, null);

            // return data
            return children.size();
        } catch (KeeperException e) {
            return null;
        }
    }

    public int delete(String host, String op) throws IOException, InterruptedException {
        ZooKeeper conn = getHandle(host);
        try{
            conn.delete("/node"+op, -1);
            return 0;
        } catch (KeeperException e) {
            return -1;
        }
    }

    public int multi(String host, String op) throws IOException, InterruptedException, KeeperException {
        ZooKeeper conn = getHandle(host);
        try {
            // Make sure node exists
            update(host, op);

            // Start multi
            Transaction t = conn.transaction();
            t.delete("/node"+op, -1);
            t.create("/node"+op, RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            List<OpResult> opResults = t.commit();
            return opResults.size();
        } catch (KeeperException e) {
            return -1;
        }
    }
}
