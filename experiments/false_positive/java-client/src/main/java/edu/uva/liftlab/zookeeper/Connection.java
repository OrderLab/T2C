package edu.uva.liftlab.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class Connection {
    ZooKeeper conn;

    String host;
    long sessionId;
    byte[] sessionPwd;

    public Connection(String newHost) throws IOException, InterruptedException {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        conn = new ZooKeeper(newHost, 3000, new Watcher() {
            public void process(WatchedEvent we) {
                if (we.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    connectionLatch.countDown();
                }
            }
        });

        connectionLatch.await();
        host = newHost;
        sessionId = conn.getSessionId();
        sessionPwd = conn.getSessionPasswd();
    }

    public void close() throws InterruptedException {
        if (conn !=null){
            conn.close();
        }
    }

    public synchronized ZooKeeper getConnection() throws IOException, InterruptedException {
        if(!conn.getState().isAlive()){
            CountDownLatch connectionLatch = new CountDownLatch(1);
            conn = new ZooKeeper(host, 3000, new Watcher() {
                public void process(WatchedEvent we) {
                    if (we.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        connectionLatch.countDown();
                    }
                }
            });
            connectionLatch.await();
        }
        sessionId = conn.getSessionId();
        sessionPwd = conn.getSessionPasswd();
        return conn;
    }
}
