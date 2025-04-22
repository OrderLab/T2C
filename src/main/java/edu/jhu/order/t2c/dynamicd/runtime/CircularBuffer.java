package edu.jhu.order.t2c.dynamicd.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

//Modified based on https://github.com/asgeirn/circular-buffer/blob/master/src/main/java/no/twingine/CircularBuffer.java

/**
 * A lock-free thread safe circular fixed length buffer.
 *
 * Uses an AtomicLong as index counter and an AtomicReferenceArray to hold the references to the values.
 *
 * When the buffer is full, the oldest item is overwritten.
 *
 */
public class CircularBuffer<T> implements EventList<T> {
    private final int capacity;
    private final AtomicReferenceArray<T> buffer;
    private AtomicInteger index = new AtomicInteger(0);

    public CircularBuffer(Class<T> clazz, int cap){
        assert cap > 0 : "Size must be positive";
        this.capacity = cap;

        buffer = new AtomicReferenceArray<T>(this.capacity);
        Class[] cArg = new Class[2];
        cArg[0] = String.class;
        cArg[1] = String.class;
        try{
            for(int i=0;i<cap;++i)
            {
                buffer.set(i, clazz.getDeclaredConstructor(cArg).newInstance(null,null));
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public static class RevDumpResult<T> {
        public final Integer size;
        public final List<T> buffer;

        public RevDumpResult(Integer siz, List<T> buf){
            size = siz;
            buffer = buf;
        }

        public RevDumpResult(){
            size = 0;
            buffer = new ArrayList<>();
        }
    }

    public int capacity(){
        return capacity;
    }

    // Yes it's not thread-safe
    public int size() {
        return index.get();
    }

    public int add(T item) {
        assert item != null : "Item must be non-null";
        T t = buffer.get((int) (index.getAndIncrement() % capacity));
    
        ((Operation) t).optypeStr = ((Operation) item).optypeStr;
        ((Operation) t).opTree = ((Operation) item).opTree;
        ((Operation) t).symbolMap = ((Operation) item).symbolMap;
        ((Operation) t).flag = ((Operation) item).flag;
        return index.get();
    }

    public T get() {
        return buffer.get(index.get() % capacity);
    }

    public T get(int i) {
        return buffer.get(i % capacity);
    }

    public synchronized RevDumpResult<T> revDump(int idx) {
        // return reverse traces
        ArrayList<T> retVal = new ArrayList<>();
        while (idx >= 0 && retVal.size() < capacity){
            retVal.add(buffer.get(idx % capacity));
            idx -= 1;
        }
        return new RevDumpResult<>(index.get(), retVal);
    }

    public synchronized RevDumpResult<T> revDump() {
        // return reverse traces
        return new RevDumpResult<>();
        
//        if (index==0){
//            return new RevDumpResult<>();
//        }
//        int tempIdx = index-1;
//        ArrayList<T> retVal = new ArrayList<>();
//        while (tempIdx >= 0 && retVal.size() < capacity){
//            retVal.add(buffer.get(tempIdx % capacity));
//            tempIdx -= 1;
//        }
//        return new RevDumpResult<>(index, retVal);
    }

    public synchronized List<T> dumpAll(){
        return new ArrayList<>();
//        return new ArrayList<>(buffer);
    }

    public synchronized void clear(){
        index.set(0);
    }
}
