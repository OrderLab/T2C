package edu.jhu.order.t2c.dynamicd.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.jhu.order.t2c.dynamicd.runtime.CheckerTemplate.gsonPrettyPrinter;

public class Symbol {
    //CHANG: disable the json serialization for now to compare the impact
    final static boolean ENABLE_JSON_TO_STRING = true;

    static final String SYMBOL_CHARACTER = "$";

    static Set<String> simpleSerializeTypes = new HashSet<String>(){{
        add("org.apache.hadoop.hbase.master.HMaster");
        add("org.apache.hadoop.hbase.security.UserProvider");
        add("org.apache.hadoop.conf.Configuration");
        add("org.apache.hadoop.hbase.zookeeper.ZKWatcher");
    }};

    public String symbolName;
    //reference saving that object
    public transient Object obj;
    //hardcoded value for obj, for non-serializable object this is a hashcode for comparison
    public int hashcode;
    public String typeName;
    //for printable value, we print it out for debugging purpose
    public String strVal;
    //if this symbol is a system workload, we should assign a key to it
    //public String sysWorkloadKey;

    private static GsonBuilder gsonBuilder = new GsonBuilder()
            .registerTypeAdapter(Operation.OpType.class, InterfaceSerializer.interfaceSerializer(Operation.OpTypeBasicImpl.class))
            .setPrettyPrinting();

    private static Gson gsonObj = gsonBuilder.setExclusionStrategies(new CheckerTemplate.ClassExclusionStrategy())
            .create();

    //if the symbol contains sub symbols
    public Set<String> subSymbols = new HashSet<>();

    public Symbol(String symbolName, Object obj) {
        this.symbolName = symbolName;
        this.obj = obj;
        //this.sysWorkloadKey = null;

        if(obj == null)
        {
            this.typeName = "null";
            this.hashcode = -1;
        }
        else
        {
            this.typeName = obj.getClass().getName();
            //hashcode for string and integer should be specially handled, otherwise
            //cannot be matched
            if(obj instanceof Integer)
                this.hashcode = (Integer)obj;
            else if(obj instanceof String)
                this.hashcode = obj.hashCode();
            else
                this.hashcode = System.identityHashCode(obj);

            if(ENABLE_JSON_TO_STRING)
            {
                try{
                    //there are different ways to serialize the obj, the cheap way (toString) and expensive way (gson)
                    //some types of objects may experience issues when using gson to serialize, that's why we need to
                    //do a filtering
                    if(simpleSerializeTypes.contains(this.typeName)){
                        this.strVal = obj.toString();
                    }  else {
                        this.strVal = gsonObj.toJson(obj);
//                        this.strVal = gsonPrettyPrinter.toJson(obj);
                    }
                }
                catch (Throwable ex)
                {
                    //System.out.println("[INFO] Serializing fails for "+this.typeName);
                    this.strVal = obj.toString();
                }
            }
            else {
                this.strVal = obj.toString();
            }
        }
    }

    public static boolean isSymbol(String str)
    {
        return str.startsWith(SYMBOL_CHARACTER);
    }

    public Object getObj()
    {
        return obj;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Symbol symbol = (Symbol) o;

        return hashcode == symbol.hashcode;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "symbolName='" + symbolName + '\'' +
                ", hashcode=" + hashcode +
                ", typeName='" + typeName + '\'' +
                //", strVal='"+strVal+'\''+
                //", sysWorkloadKey='" + sysWorkloadKey + '\'' +
                '}';
    }

    public static class SymbolBuilder {
        AtomicInteger index;
        Map<String, Symbol> symbolMap = new HashMap<>();
        CheckerTemplate.ConstantCache cache;

        public SymbolBuilder(AtomicInteger index, CheckerTemplate.ConstantCache cache) {
            //T2CHelper.globalLogInfo("#### new symbol builder, index is "+index);
            //System.out.println("#### new symbol builder, index is "+index);
            this.index = index;
            this.cache = cache;
        }

        public String getSymbol(Object obj) {
            //skip ignored values, this is useful when we force some numbers to be constant
            if(cache.cswitch)
            {
                if(cache.constantPool.contains(obj))
                {
                    T2CHelper.globalLogInfo("CONSTANT:"+obj);
                    return (String)obj;
                }
            }

            String symbolName = SYMBOL_CHARACTER + index;
            index.incrementAndGet();
            Symbol symbol = new Symbol(symbolName, obj);
            //T2CHelper.globalLogInfo("#### add symbol name "+symbolName);
            //System.out.println("#### add symbol name "+symbolName);
            symbolMap.put(symbolName, symbol);

            return symbolName;
        }

        public String[] convertArray(Object obj)
        {

            if(obj instanceof Object[])
            {
                int length = ((Object[]) obj).length;
                String[] symbols = new String[length];
                for(int i=0;i<length;++i)
                {
                    Integer subobj = (Integer) ((Object [])obj)[i];
                    symbols[i] = getSymbol(subobj.toString());
                }
                return symbols;
            }
            else
                throw new RuntimeException("Type not supported");
        }

        public String[][] convert2DArray(Object obj)
        {

            if(obj instanceof Object[][])
            {
                int length = ((Object[]) obj).length;
                String[][] symbols = new String[length][];
                for(int i=0;i<length;++i)
                {
                    Object subobj = ((Object [])obj)[i];
                    symbols[i] = convertArray(subobj);
                }
                return symbols;
            }
            else
                throw new RuntimeException("Type not supported");
        }

        public Map<String, Symbol> getSymbolMap() {
            return symbolMap;
        }
    }


}
