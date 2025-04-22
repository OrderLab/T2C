package edu.jhu.order.t2c.dynamicd.util;

/**
 * Trie Data structure implementation without any libraries
 *
 * @author Dheeraj Kumar Barnwal (https://github.com/dheeraj92)
 *
 * https://github.com/TheAlgorithms/Java/blob/master/src/main/java/com/thealgorithms/datastructures/trees/TrieImp.java
 */

import edu.jhu.order.t2c.dynamicd.runtime.CheckerTemplate;
import edu.jhu.order.t2c.dynamicd.runtime.EventList;
import edu.jhu.order.t2c.dynamicd.runtime.Operation;
import edu.jhu.order.t2c.dynamicd.runtime.T2CHelper;

import java.util.*;

public class Trie {

    public class TrieNode {

        Map<Operation, TrieNode> child;
        Set<CheckerTemplate> end;

        public TrieNode() {
            child = new HashMap<>();
            end = new HashSet<>();
        }
    }

    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }
    
    public void insert(CheckerTemplate template){
        T2CHelper.prodLogInfo("Trie inserting "+template.templateSource);
        TrieNode currentNode = root;
        Integer index = template.getOperations().size();
        ListIterator<Operation> listIterator = template.getOperations().listIterator(template.getOperations().size());
        while(listIterator.hasPrevious()){
            Operation op = listIterator.previous();
            TrieNode node = currentNode.child.get(op);
            if (node == null){
                node = new TrieNode();
                currentNode.child.put(op, node);
            }
            if (template.getTriggerIndex().get(index)!=null && !template.getTriggerIndex().get(index).isEmpty()){
                node.end.add(template);
            }
            currentNode = node;
            index--;
        }
        currentNode.end.add(template);
    }

    // Map<Template, Map<assertionIdx, matchingOp>>
//    public Map<CheckerTemplate, Map<Integer, List<Operation>>> forwardSearch(List<Operation> operations) {
//        // Make sure the list is already reversed
//        Map<CheckerTemplate, Map<Integer, List<Operation>>> retVal = new HashMap<>();
//        List<Operation> opList = new ArrayList<>();
//        Integer index = 0;
//
//        TrieNode currentNode = root;
//        for (Operation operation : operations) {
//            index += 1;
//            TrieNode node = currentNode.child.get(operation);
//            if (node == null) {
//                return retVal;
//            }
//            opList.add(operation);
//            if (node.end != null) {
//                List<Integer> assertionIdxList = node.end.getTriggerIndex().get(index);
//                Map<Integer, List<Operation>> nestedMap = new HashMap<>();
//                for (Integer assertionIdx : assertionIdxList) {
//                    nestedMap.put(assertionIdx, new ArrayList<>(opList));
//                }
//                retVal.put(node.end, nestedMap);
//            }
//            currentNode = node;
//        }
//
//        return retVal;
//    }

    // Map<Template, Map<assertionIdx, matchingOp>> backward
    public Map<CheckerTemplate, Map<Integer, List<Operation>>> search(List<Operation> operations) {
        Map<CheckerTemplate, Map<Integer, List<Operation>>> retVal = new HashMap<>();
        List<Operation> opList = new ArrayList<>();
        Integer index = 0;

        TrieNode currentNode = root;
        for (Operation op: operations){
            index += 1;
            TrieNode node = currentNode.child.get(op);
            if (node == null){
                return retVal;
            }
            opList.add(0,op);
            if (!node.end.isEmpty()){
                for (CheckerTemplate template: node.end){
                    List<Integer> assertionIdxList = template.getTriggerIndex().get(index);
                    if (assertionIdxList!=null && !assertionIdxList.isEmpty()){
                        Map<Integer, List<Operation>> nestedMap = new HashMap<>();
                        for (Integer assertionIdx: assertionIdxList){
                            nestedMap.put(assertionIdx, new ArrayList<>(opList));
                        }
                        retVal.put(template, nestedMap);
                    }
                }
            }
            currentNode = node;
        }

        return retVal;
    }

//    public Map<CheckerTemplate, Map<Integer, List<Operation>>> bufferSearch(EventList<Operation> traces, int idx) {
//        Map<CheckerTemplate, Map<Integer, List<Operation>>> retVal = new HashMap<>();
//        List<Operation> opList = new ArrayList<>();
//        Integer index = 0;
//
//        TrieNode currentNode = root;
//        for (int i = idx; i > Math.max(-1, idx- traces.capacity()); i--) {
//            index += 1;
//            Operation op = traces.get(i);
//            if (op==null){
//                return retVal;
//            }
//            TrieNode node = currentNode.child.get(op);
//            if (node == null){
//                return retVal;
//            }
//            opList.add(op);
//            if (node.end!=null){
//                List<Integer> assertionIdxList = node.end.getTriggerIndex().get(index);
//                Map<Integer, List<Operation>> nestedMap = new HashMap<>();
//                for (Integer assertionIdx: assertionIdxList){
//                    nestedMap.put(assertionIdx, new ArrayList<>(opList));
//                }
//                retVal.put(node.end, nestedMap);
//            }
//            currentNode = node;
//        }
//
//        return retVal;
//    }
}