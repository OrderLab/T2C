package edu.jhu.order.t2c.dynamicd.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface EventList<T> {

    public int add(T t);

    public int capacity();

    public int size();

    public T get(int index);
    
    public void clear();

    public CircularBuffer.RevDumpResult<T> revDump(int index);
    public CircularBuffer.RevDumpResult<T> revDump();

    public List<T> dumpAll();
}
