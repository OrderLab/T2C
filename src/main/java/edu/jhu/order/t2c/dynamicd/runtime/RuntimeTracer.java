package edu.jhu.order.t2c.dynamicd.runtime;

import edu.jhu.order.t2c.dynamicd.runtime.T2CHelper.MiniLogger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

//tracing operations in the production systems
public class RuntimeTracer {
    public static String templateDir = GlobalState.mode.equals(GlobalState.T2CMode.PRODUCTION)
            ? ConfigManager.SYSTEM_PATH + "/templates_in"
            : "./templates_in";

    //right now we maintain a full list of history operations, in the future we may want to
    //refactor to be a moving window or other more efficient data structures
    //update: now using CircularBuffer
    public final EventList<Operation> historyTraces = new CircularBuffer<Operation>(Operation.class,512);
    //built-in templates, they should be loaded from offline test case constructions
    public List<CheckerTemplate> builtinTemplates = new ArrayList<>();
    // public Trie trieObj = new Trie();

    public Queue<Future<?>> futures = new ConcurrentLinkedQueue<>();
    public ExecutorService executor = Executors.newFixedThreadPool(24); // change according to the proc number
    public AtomicInteger execCountdown = new AtomicInteger(10);

    // to avoid we trigger same checker too many times in validation, use this to filter
    public Set<Integer> executedAssertionID = new HashSet<>();

    private final ExecutorService appendExecutor = Executors.newSingleThreadExecutor();
    public ConcurrentHashMap<Integer, AtomicInteger> rateLimiter = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Integer, Long> rateRefresher = new ConcurrentHashMap<>();
    public String[] rateLimitWhitelist = ConfigManager.config.getStringArray(ConfigManager.RATE_LIMIT_WHITELIST);
    public Integer rateLimitSize = ConfigManager.config.getInt(ConfigManager.RATE_LIMIT_SIZE, 3);
    public long rateLimitRefresh = ConfigManager.config.getInt(ConfigManager.RATE_LIMIT_REFRESH, 15)* 1_000L;

    // private Thread matchThread;

    public AtomicInteger success = new AtomicInteger(0);
    public AtomicInteger fail = new AtomicInteger(0);
    public AtomicInteger skip = new AtomicInteger(0);
    public ConcurrentHashMap<String, Long> failMap = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Long> successMap = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Long> skipMap = new ConcurrentHashMap<>();

    Map<Integer, List<Operation>> matchedCheckers = new HashMap<>();
    Map<String, Symbol> updatedMap = new HashMap<>();
    Map<CheckerTemplate.TemplateWithMap, Integer> matchedTemplates = new HashMap<>();
    List<Operation> matched = new ArrayList<>();
    HashSet<Integer> matched_traces = new HashSet<>();

    public void loadTemplates()
    {
        TemplateManager.loadTemplates(templateDir, builtinTemplates, new MiniLogger() {
            @Override
            public void logInfo(String str) {
                T2CHelper.prodLogInfo(str);
            }
        });
    }

    public RuntimeTracer() throws InstantiationException, IllegalAccessException {
        loadTemplates();
        //SysWorkloadPool.initPool();
        SysWorkloadPool.registerTypes();
//        for (CheckerTemplate template: builtinTemplates){
//            trieObj.insert(template);
//        }
//
//        matchThread = new Thread(new MatchRunnable(historyTraces, trieObj));
//        matchThread.start();
        //System.out.println("#### runtime tracer init success");
    }

    static RuntimeTracer tracer = null;

    public static RuntimeTracer getInstance() {
        try{
            if(tracer==null)
                tracer = new RuntimeTracer();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(-1);
        }

        return tracer;
    }

    public CheckerTemplate getTemplate(String templateName)
    {
        for(CheckerTemplate template:builtinTemplates)
        {
            if(template.templateSource.equals(templateName))
                return template;
        }
        return null;
    }

   public class AppendTask implements Runnable {
        Operation op;
        public AppendTask(Operation item) {
            op = item;
        }

        public void run() {
            T2CHelper.prodLogInfo("Added "+op.getOptypeStr()+" size "+ historyTraces.add(op));
            matchedCheckers.clear();
            updatedMap.clear();
            matchedTemplates.clear();
            matched.clear();
            matched_traces.clear();
            CheckerTemplate.match(historyTraces, builtinTemplates, matched, matched_traces, matchedCheckers, updatedMap, matchedTemplates);
            if (!matchedTemplates.isEmpty()) {
                T2CHelper.prodLogInfo(matchedTemplates.size() + " Templates matched!");
                for (CheckerTemplate.TemplateWithMap matchedTemplate : matchedTemplates.keySet()) {
                    Integer index= matchedTemplates.get(matchedTemplate);
                    matchedTemplate.check(index);
                }
            }
        }
   }

    //synchronized
    public void addTrace(Operation op) {
        // T2CHelper.prodLogInfo("******* addTrace for operation: " + op.optypeStr + " *******");

        //we should skip validation purpose select
        if (!op.ifQueryOp) {
            RuntimeTracer.getInstance().appendExecutor.submit(new AppendTask(op));
        }
    }
}
