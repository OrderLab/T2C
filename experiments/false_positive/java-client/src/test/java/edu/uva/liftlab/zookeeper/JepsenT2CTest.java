package edu.uva.liftlab.zookeeper;

import junit.framework.TestCase;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JepsenT2CTest extends TestCase {
    JepsenT2C client;

    public void setUp() throws Exception {
        super.setUp();
        client = new JepsenT2C();
        client.init();
    }

    public void tearDown() throws Exception {
        Thread.sleep(5000);
        if(client!=null){
            client.close();
        }
    }

    public void testPopulate() throws IOException, InterruptedException, KeeperException {
        int count = 3000;
        client.populate(count);
        assertNotNull(client.read(client.defaultHost, String.valueOf(count-1)));
    }

    public void testCustom() throws IOException, InterruptedException, KeeperException {
        ZooKeeper conn =  client.getHandle("lift11");
        conn.setData("/", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), -1);
        conn.create("/a", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        conn.create("/a/b", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        conn.create("/a/b/v", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        conn.create("/a/b/v/1", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        conn.create("/a/c", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        conn.create("/a/c/v", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        ZKUtil.deleteRecursive(conn, "/a");
        conn.exists("/a", null);
    }

    public void testCustom2() throws IOException, InterruptedException, KeeperException {
        ZooKeeper conn =  client.getHandle("lift11");
        conn.setData("/", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), -1);
        conn.create("/a", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        conn.create("/b", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        conn.create("/c", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        conn.create("/d", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        conn.create("/e", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        conn.create("/f", RandomStringUtils.randomAlphanumeric(3).getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        ZKUtil.deleteRecursive(conn, "/a");
        conn.exists("/a", null);
    }

    public void testCreate() throws IOException, InterruptedException {
        String host = "lift11";
//        assertNotNull(client.create(host, "3000"));
//        assertNotNull(client.read(host, "3000"));
        assertNotNull(client.create(host, "3001"));
//        assertNotNull(client.create(host, "3001/3001"));
        assertNotNull(client.read(host, "3001"));
    }

    public void testUpdate() throws IOException, InterruptedException {
        assertNotNull(client.update(client.defaultHost, "2999"));
    }

    public void testRead() throws IOException, InterruptedException {
        assertNotNull(client.read(client.defaultHost, "2999"));
    }

    public void testDelete() throws IOException, InterruptedException {
        assertEquals(0, client.delete(client.defaultHost, "3000"));
    }

    public void testMulti() throws IOException, InterruptedException, KeeperException {
        assertEquals(2, client.multi(client.defaultHost, "2999"));
    }
}