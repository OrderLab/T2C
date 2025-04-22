package edu.uva.liftlab.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Copied from NNBenchWithoutMR
public class JepsenT2C {
    FileSystem fileSysMaster;
    ConcurrentHashMap<String, FileSystem> fileSystems = new ConcurrentHashMap<>();

    String baseDir = "/benchmarks";
    Path taskDir;

    long numFiles = 100;
    long blocksPerFile = 160;
    long bytesPerBlock = 1048576;
    long bytesPerFile = bytesPerBlock * blocksPerFile;

    byte[] buffer;
    short replicationFactorPerFile = 3;

    public JepsenT2C() throws IOException, URISyntaxException {
        Configuration config = new Configuration();
        config.set("fs.defaultFS", "hdfs://lift11:9000");
        config.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        config.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
        config.addResource("/localtmp/invivo/hadoop/hadoop-dist/target/hadoop-3.2.2/etc/hadoop/core-site.xml");
        config.addResource("/localtmp/invivo/hadoop/hadoop-dist/target/hadoop-3.2.2/etc/hadoop/hdfs-site.xml");
        UserGroupInformation.setLoginUser(UserGroupInformation.createRemoteUser("vqx2dc"));
        System.setProperty("HADOOP_USER_NAME", "vqx2dc");
        System.setProperty("hadoop.home.dir", "/localtmp/invivo/hadoop/hadoop-dist/target/hadoop-3.2.2");
        fileSysMaster = FileSystem.get(config);
        String uniqueId = java.net.InetAddress.getLocalHost().getHostName();
        taskDir = new Path(baseDir, uniqueId);

        if(!fileSysMaster.mkdirs(taskDir)){
            throw new IOException("Mkdirs failed to create " + taskDir);
        }

        bytesPerFile = bytesPerBlock * blocksPerFile;
        buffer = new byte[(int) Math.min(bytesPerFile, 32768L)];
    }

    public void close() throws IOException {
        if (fileSysMaster!=null){
            fileSysMaster.close();
        }
        for (Map.Entry<String, FileSystem> handle: fileSystems.entrySet()){
            handle.getValue().close();
        }
    }

    public void populate() throws IOException {
        for (int i = 0; i < numFiles; i++) {
            write("lift11", i);
        }
    }

    public FileSystem getHandle(String host) throws IOException {
        synchronized (this) {
            if(!fileSystems.containsKey(host)){
                Configuration config = new Configuration();
                config.set("fs.defaultFS", "hdfs://"+host+":9000");
                config.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
                config.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
                config.addResource("/localtmp/invivo/hadoop/hadoop-dist/target/hadoop-3.2.2/etc/hadoop/core-site.xml");
                config.addResource("/localtmp/invivo/hadoop/hadoop-dist/target/hadoop-3.2.2/etc/hadoop/hdfs-site.xml");
                UserGroupInformation.setLoginUser(UserGroupInformation.createRemoteUser("vqx2dc"));
                System.setProperty("HADOOP_USER_NAME", "vqx2dc");
                System.setProperty("hadoop.home.dir", "/localtmp/invivo/hadoop/hadoop-dist/target/hadoop-3.2.2");
                fileSystems.put(host, FileSystem.get(config));
            }
        }

        return fileSystems.get(host);
    }

    public int read(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        int totalExceptions = 0;
        FSDataInputStream in;
        try {
            in = fileSys.open(new Path(taskDir, "" + index), 512);
            long toBeRead = bytesPerFile;
            while (toBeRead > 0) {
                int nbytes = (int) Math.min(buffer.length, toBeRead);
                toBeRead -= nbytes;
                try { // only try once && we don't care about a number of bytes read
                    in.read(buffer, 0, nbytes);
                } catch (IOException ioe) {
                    totalExceptions++;
                }
            }
            in.close();
        } catch (IOException ioe) {
            totalExceptions++;
        }
        return totalExceptions;
    }

    public int write(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        int totalExceptions = 0;
        FSDataOutputStream out = null;
        boolean success;
        do { // create file until succeeds or max exceptions reached
            try {
                out = fileSys.create(
                        new Path(taskDir, "" + index), true, 512,
                        replicationFactorPerFile, bytesPerBlock);
                success = true;
            } catch (IOException ioe) {
                success=false;
                totalExceptions++;
            }
        } while (!success);
        long toBeWritten = bytesPerFile;
        while (toBeWritten > 0) {
            int nbytes = (int) Math.min(buffer.length, toBeWritten);
            toBeWritten -= nbytes;
            try { // only try once
                out.write(buffer, 0, nbytes);
            } catch (IOException ioe) {
                totalExceptions++;
            }
        }
        do { // close file until succeeds
            try {
                out.close();
                success = true;
            } catch (IOException ioe) {
                success=false;
                totalExceptions++;
            }
        } while (!success);
        return totalExceptions;
    }

    public boolean delete(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        return fileSys.delete(new Path(taskDir, "" + index), true);
    }

    public boolean truncate(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        return fileSys.truncate(new Path(taskDir, "" + index), bytesPerBlock/2);
    }

    public boolean mkdirs(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        return fileSys.mkdirs(new Path(taskDir, "dir" + index));
    }

    public int ls(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        RemoteIterator<LocatedFileStatus> files =  fileSys.listFiles(taskDir, false);
        int count = 0;
        while (files.hasNext()){
            count+=1;
            files.next();
        }
        return count;
    }

    public int chksum(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        return fileSys.getFileChecksum(new Path(taskDir, "" + index)).getLength();
    }

    public void rename(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        fileSys.rename(new Path(taskDir, "" + index), new Path(taskDir, "" + index));
    }

    public void symlinkCreate(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        fileSys.createSymlink(new Path(taskDir, "" + index), new Path(taskDir, "link" + index), true);
    }

    public void snapshotCreate(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        fileSys.createSnapshot(new Path(taskDir, "" + index));
    }

    public void snapshotDelete(String host, int index) throws IOException {
        FileSystem fileSys = getHandle(host);
        fileSys.deleteSnapshot(new Path(taskDir, "" + index), "snap"+index);
    }
}
