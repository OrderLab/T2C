package edu.jhu.order.t2c.dynamicd.util;

import edu.jhu.order.t2c.dynamicd.runtime.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchRunnable implements Runnable {
    private final EventList<Operation> historyTraces;
    private final Trie trieObj;
    private int idx = 0;

    public MatchRunnable(EventList<Operation> traces, Trie trie){
        historyTraces = traces;
        trieObj = trie;
    }

    @Override
    public void run() {
//        try{
//            while (true){
//                while (idx>=historyTraces.size()){
//                    Thread.sleep(50);
//                }
//
//                CircularBuffer.RevDumpResult<Operation> temp = historyTraces.revDump();
//                if (idx<temp.size-historyTraces.capacity()){
//                    idx = temp.size-historyTraces.capacity();
//                }
//                while (idx<temp.size){
//                    try {
//                        Map<CheckerTemplate.TemplateWithMap, Integer> matchedTemplates = CheckerTemplate.match(temp.buffer.subList(temp.size-1-idx, temp.buffer.size()), trieObj);
//                        if (matchedTemplates.size() > 0) {
//                            // T2CHelper.prodLogInfo(matchedTemplates.size() + " Templates matched!");
//                            for (CheckerTemplate.TemplateWithMap matchedTemplate : matchedTemplates.keySet()) {
//                                Integer index= matchedTemplates.get(matchedTemplate);
//                                matchedTemplate.check(index);
//                            }
//                        }
//                    } catch (Exception e){
//                        T2CHelper.prodLogInfo("Exception "+idx+" "+e.getMessage());
//                        e.printStackTrace(new PrintWriter(T2CHelper.getInstance().getProdlogInfoInternal()));
//                    }
//
//                    idx++;
//                }
//            }
//        } catch (InterruptedException e){
//            T2CHelper.prodLogInfo("Thread crash");
//        }
    }
}
