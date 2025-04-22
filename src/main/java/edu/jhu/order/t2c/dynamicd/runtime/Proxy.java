package edu.jhu.order.t2c.dynamicd.runtime;

import java.util.ArrayList;
import java.util.List;

public class Proxy {
    static List<String> quorumList = new ArrayList<>();

    public static List<String> getQuorumList()
    {
        return quorumList;
    }

    public static void registerQuorumPeer(String hostport)
    {
        quorumList.add(hostport);
    }

}
