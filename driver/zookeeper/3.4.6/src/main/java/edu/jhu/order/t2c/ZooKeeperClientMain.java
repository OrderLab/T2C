/*
 *  @author Chang Lou <chlou@jhu.edu>, Haoze Wu <haoze@jhu.edu>
 *
 *  Copyright (c) 2019, Johns Hopkins University - Order Lab.
 *      All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package edu.jhu.order.t2c;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooKeeperClientMain {
    private static int clientId = -1;
    private static int workerNum = -1;
    private static int entryNum = -1;
    private static int iteration = -1;
    private static String addr = "";
    private static int serverId = -1;
    private static int timeout = -1;
    private static Random rand = new Random(System.nanoTime() + 1);

    public static void run(final String [] args) {
        addr = "localhost:" + Integer.parseInt(args[0]);
        System.out.println("addr="+addr);
        workerNum = Integer.parseInt(args[1]);
        System.out.println("workerNum="+workerNum);
        if(workerNum>20)
        {
            System.out.println("WorkerNum exceeding 20, exit.");
            System.exit(-1);
        }
        entryNum = Integer.parseInt(args[2]);
        System.out.println("entryNum="+entryNum);
        iteration = Integer.parseInt(args[3]);
        System.out.println("iteration="+iteration);
        if (args.length > 4) {
            timeout = Integer.parseInt(args[4]);
            System.out.println("timeout="+timeout);
        } else {
            timeout = 7000;
        }

        try {
            for(int i=0;i<workerNum;++i)
            {
                Thread thread = new Thread(){
                    public void run(){
                        System.out.println("New worker running.");
                        try{

                            doOp();
                        }
                        catch (final Exception e) {}
                    }
                };
                thread.start();
            }
        } catch (final Exception e) {}

    }

    private static int progress = 0;
    private static long t0 = System.nanoTime();

    private static void reply(final String result, final long nano) {
        System.out.println("progress = {}, time = {}"+ (++progress)+( System.nanoTime() - t0));
    }

    private static void create() throws Exception {
        int progress = 0;
        int failure = 0;
        while (progress < entryNum) {
            try (final ZooKeeperClient client = new ZooKeeperClient(addr, timeout)) {
                while (progress < entryNum) {
                    if ((progress + serverId) % 3 != 0) {
                        progress++;
                        continue;
                    }
                    final long nano = System.nanoTime();
                    client.create("/zookeeper/" + progress, "0000".getBytes());
                    reply("success", nano);
                    progress++;
                }
            } catch (final Exception e) {
                failure++;
                System.out.println("ZooKeeper client exception -- " + e);
                e.printStackTrace(System.out);
                if (failure > 3) {
                    return;
                }
                Thread.sleep(20);
            }
        }
    }

    private static void read() throws Exception {
        int progress = 0;
        int failure = 0;
        while (progress < iteration) {
            try (final ZooKeeperClient client = new ZooKeeperClient(addr, timeout)) {
                for (int i = progress; i < iteration; i++) {
                    final long nano = System.nanoTime();
                    client.getData("/zookeeper/" + rand.nextInt(entryNum));
                    reply("success", nano);
                    progress++;
                }
            } catch (final Exception e) {
                failure++;
                System.out.println("ZooKeeper client exception -- " + e);
                if (failure > 3) {
                    return;
                }
                Thread.sleep(20);
            }
        }
    }

    private static final byte[][] data = {"1234".getBytes(), "asdf".getBytes(), "qwer".getBytes()};

    private static void write() throws Exception {
        int progress = 0;
        int failure = 0;
        while (progress < iteration) {
            try (final ZooKeeperClient client = new ZooKeeperClient(addr, timeout)) {
                for (int i = progress; i < iteration; i++) {
                    final long nano = System.nanoTime();
                    client.setData("/zookeeper/" + rand.nextInt(entryNum),
                            data[rand.nextInt(data.length)]);
                    reply("success", nano);
                    progress++;
                }
            } catch (final Exception e) {
                failure++;
                System.out.println("ZooKeeper client exception -- " + e);
                if (failure > 3) {
                    return;
                }
                Thread.sleep(20);
            }
        }
    }

    private static void doOp() throws Exception{
        int progress = 0;
        int failure = 0;
        while (progress < iteration) {
            try (final ZooKeeperClient client = new ZooKeeperClient(addr, timeout)) {
                for (int i = progress; i < iteration; i++) {
                    final long nano = System.nanoTime();
                    int command = rand.nextInt(5);
                    try {
                        switch (command) {
                            case 0: //"create" :
                                client.create("/zookeeper/" + progress, "0000".getBytes());
                                break;
                            case 1: //"read"   :
                                client.getData("/zookeeper/" + rand.nextInt(entryNum));
                                break;
                            case 2: //"write"  :
                                client.setData("/zookeeper/" + rand.nextInt(entryNum),
                                        data[rand.nextInt(data.length)]);
                            break;
                            case 3: //"delete"  :
                                client.delete("/zookeeper/" + rand.nextInt(entryNum));
                                break;
                            case 4: //"closeSession"  :
                                client.close();
                                i = iteration;
                                break;
                            default: System.out.println("undefined command -- " + command);
                        }
                    } catch (final Exception e) {}
                    progress++;
                }
            } catch (final Exception e) {
                failure++;
                System.out.println("ZooKeeper client exception -- " + e);
                if (failure > 3) {
                    return;
                }
                Thread.sleep(20);
            }
        }
    }

    public static void main(final String[] args) {
         run(args);
    }
}