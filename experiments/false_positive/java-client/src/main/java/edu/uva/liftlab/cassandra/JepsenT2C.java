package edu.uva.liftlab.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.protocol.internal.request.Batch;
import org.apache.commons.lang3.RandomStringUtils;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JepsenT2C {
    CqlSessionBuilder clusterSessionBuilder;
    CqlSession clusterSession;
    List<String> hosts = Arrays.asList("lift11","lift13","lift14","lift15");
    Integer rowCount = 3000;
    Duration timeout = Duration.ofMinutes(3);

    ConcurrentHashMap<String, CqlSession> sessions = new ConcurrentHashMap<>();
    PreparedStatement rStmt;
    PreparedStatement wStmt;

    public JepsenT2C() {
        clusterSessionBuilder = CqlSession.builder().withLocalDatacenter("datacenter1");
        for (String host: hosts){
            clusterSessionBuilder.addContactPoint(new InetSocketAddress(host, 9042));
        }

    }

    public JepsenT2C(List<String> newHosts){
        hosts = newHosts;
        clusterSessionBuilder = CqlSession.builder().withLocalDatacenter("datacenter1");
        for (String host: hosts){
            clusterSessionBuilder.addContactPoint(new InetSocketAddress(host, 9042));
        }
    }

    public void close(){
        if (clusterSession!=null){
            clusterSession.close();
        }
        for (Map.Entry<String, CqlSession> session: sessions.entrySet()){
            session.getValue().close();
        }
    }

    public void populate(int count){
        rowCount = count;

        if(clusterSession!=null){
            clusterSession.close();
        }

        CqlSession tempSession = clusterSessionBuilder.build();
        tempSession.execute(SimpleStatement.newInstance("DROP KEYSPACE IF EXISTS jepsen").setTimeout(timeout));
        tempSession.execute(SimpleStatement.newInstance("CREATE KEYSPACE jepsen WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3}").setTimeout(timeout));
        tempSession.close();

        clusterSession = clusterSessionBuilder.withKeyspace("jepsen").build();
        clusterSession.execute(SimpleStatement.newInstance("CREATE TABLE usertable ( j_id varchar primary key, field0 varchar, field1 varchar, field2 varchar, field3 varchar, field4 varchar, field5 varchar, field6 varchar, field7 varchar, field8 varchar, field9 varchar)").setTimeout(timeout));

        SimpleStatement insertStmt = SimpleStatement.builder("INSERT INTO usertable (j_id, field0, field1, field2, field3, field4, field5, field6, field7, field8, field9) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .setConsistencyLevel(DefaultConsistencyLevel.QUORUM)
                .setTimeout(timeout)
                .build();
        PreparedStatement preparedInsertStmt = clusterSession.prepare(insertStmt);

//        int batchCount = 5;
//        for (int z = 0; z < Math.floorDiv(count, batchCount); z++) {
//            BatchStatement batchStmt = BatchStatement.newInstance(DefaultBatchType.LOGGED).setTimeout(timeout);
//
//            for (int i = 0; i < Math.min(batchCount, count-(z*batchCount)); i++) {
//                ArrayList<String> params = new ArrayList<>();
//                params.add(String.valueOf(i + (z*batchCount)));
//                for (int j = 0; j < 10; j++) {
//                    params.add(RandomStringUtils.randomAlphanumeric(10));
//                }
//                batchStmt.add(preparedInsertStmt.bind()
//                        .setString(0, params.get(0))
//                        .setString(1, params.get(1))
//                        .setString(2, params.get(2))
//                        .setString(3, params.get(3))
//                        .setString(4, params.get(4))
//                        .setString(5, params.get(5))
//                        .setString(6, params.get(6))
//                        .setString(7, params.get(7))
//                        .setString(8, params.get(8))
//                        .setString(9, params.get(9))
//                        .setString(10, params.get(10)));
//            }
//
//            clusterSession.execute(batchStmt);
//        }

        for (int i = 0; i < count; i++) {
            try {
                ArrayList<String> params = new ArrayList<>();
                params.add(String.valueOf(i));
                for (int j = 0; j < 10; j++) {
                    params.add(RandomStringUtils.randomAlphanumeric(10));
                }
                clusterSession.execute(
                        preparedInsertStmt.bind()
                                .setString(0, params.get(0))
                                .setString(1, params.get(1))
                                .setString(2, params.get(2))
                                .setString(3, params.get(3))
                                .setString(4, params.get(4))
                                .setString(5, params.get(5))
                                .setString(6, params.get(6))
                                .setString(7, params.get(7))
                                .setString(8, params.get(8))
                                .setString(9, params.get(9))
                                .setString(10, params.get(10))
                );
            
                Thread.sleep(200);
            } catch (Exception e) {
                // TODO: handle exception
            }
            
        }

        SimpleStatement rSimple = SimpleStatement.builder("SELECT * FROM usertable WHERE j_id= ? LIMIT 1")
                .setConsistencyLevel(DefaultConsistencyLevel.ONE)
                .setTimeout(timeout)
                .build();
        rStmt = clusterSession.prepare(rSimple);

        SimpleStatement wSimple = SimpleStatement.builder("INSERT INTO usertable (j_id, field0, field1, field2, field3, field4, field5, field6, field7, field8, field9) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .setConsistencyLevel(DefaultConsistencyLevel.ONE)
                .setTimeout(timeout)
                .build();
        wStmt = clusterSession.prepare(wSimple);
    }

    public CqlSession getSession(String host){
        synchronized (this) {
            if(!sessions.containsKey(host)){
                sessions.put(host, CqlSession.builder().addContactPoint(new InetSocketAddress(host, 9042)).withLocalDatacenter("datacenter1").build());
            }
        }

        return sessions.get(host);
    }

    public String read(String host, int key, int field) throws Exception {
        if(key>=rowCount || field>9){
             throw new Exception("Invalid row");
        }
        CqlSession session = getSession(host);
        Row result = session.execute(rStmt.bind(String.valueOf(key))).one();
        if (result==null){
            update(host, key);
            return String.valueOf(-1);
        }

        if(field==-1){
            return result.getString("j_id");
        }

        return result.getString("field"+field);
    }

    public void update(String host, int key) throws Exception {
        if(key>=rowCount){
            throw new Exception("Invalid row");
        }

        CqlSession session = getSession(host);
        ArrayList<String> params = new ArrayList<>();
        params.add(String.valueOf(key));
        for (int j = 0; j < 10; j++) {
            params.add(RandomStringUtils.randomAlphanumeric(10));
        }
        session.execute(wStmt.bind()
                .setString(0, params.get(0))
                .setString(1, params.get(1))
                .setString(2, params.get(2))
                .setString(3, params.get(3))
                .setString(4, params.get(4))
                .setString(5, params.get(5))
                .setString(6, params.get(6))
                .setString(7, params.get(7))
                .setString(8, params.get(8))
                .setString(9, params.get(9))
                .setString(10, params.get(10)));
    }
}
