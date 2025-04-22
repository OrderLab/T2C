package edu.jhu.order.t2c.dynamicd.runtime;

import com.google.gson.*;
import com.google.gson.annotations.Expose;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.jhu.order.t2c.dynamicd.runtime.Assertion.AssertStatus;
import edu.jhu.order.t2c.dynamicd.runtime.Operation.OpType;
import edu.jhu.order.t2c.dynamicd.runtime.Operation.OpTypeBasicImpl;
import edu.jhu.order.t2c.dynamicd.util.Trie;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static edu.jhu.order.t2c.dynamicd.runtime.Symbol.ENABLE_JSON_TO_STRING;

public class CheckerTemplate {
    
    public static class VerifyStats {
        public int passNum=-1;
        public int inactiveNum=-1;
        public int failNum=-1;

        public VerifyStats(int passNum, int inactiveNum, int failNum) {
            this.passNum = passNum;
            this.inactiveNum = inactiveNum;
            this.failNum = failNum;
        }

        @Override
        public String toString() {
            return "VerifyStats{" +
                    "passNum=" + passNum +
                    ", inactiveNum=" + inactiveNum +
                    ", failNum=" + failNum +
                    '}';
        }
    }

    public enum TemplateState {
        PASS,
        INACTIVE,
        FAIL,
        ILLEGAL;
    }

    public static boolean ifStateNotFail(TemplateState state)
    {
        return state.equals(TemplateState.PASS) || state.equals(TemplateState.INACTIVE);
    }

    public enum TemplateWarning {
        NO_VALID_ASSERTION("NO_VALID_ASSERTION"),
        NO_TRIGGERING_OP("NO_TRIGGERING_OP");

        private final String text;

        /**
         * @param text
         */
        TemplateWarning(final String text) {
            this.text = text;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
        }

    }

    public static class TemplateWithMap {
        CheckerTemplate template;
        Map<String, Symbol> updatedSymMap = new HashMap<>();

        public TemplateWithMap(CheckerTemplate template, Map<String, Symbol> updatedSymMap) {
            this.template = template;
            this.updatedSymMap = updatedSymMap;
        }

        public void check(int index) {
            Assertion assertion = template.assertions.get(index);
            if (!assertion.status.equals(AssertStatus.NORMAL)) {
                RuntimeTracer.getInstance().skip.incrementAndGet();
//                T2CHelper.prodLogInfo("Skip abnormal assertion");
                return;
            }

            assertion.substitute(updatedSymMap);
            assertion.check(template);
        }
    }

    //for symbol builder to ignore some constants
    public static class ConstantCache
    {
        Set<String> constantPool = new HashSet<>();
        boolean cswitch = false;

        //this is for temporarily ignoring some values to be passed to generate symbols
        public void addConstant(String val)
        {
            constantPool.add(val);
        }

        public void set()
        {
            cswitch = true;
        }

        public void reset()
        {
            cswitch = false;
            constantPool.clear();
        }
    }

    public static class ClassExclusionStrategy implements ExclusionStrategy {
        private Class<?> excludedClass;

//        ClassExclusionStrategy(Class<?> excludedClass) {
//            this.excludedClass = excludedClass;
//        }

        // Exclude attribute that has the same class as the excluded class
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            return excludedClass.equals(fieldAttributes.getDeclaredClass());
        }

        // Do not exclude the object itself
        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
            excludedClass = aClass;
            return false;
        }
    }

    //Gson helper instance
    public static Gson gsonPrettyPrinter = new GsonBuilder()
            .registerTypeAdapter(OpType.class, InterfaceSerializer.interfaceSerializer(OpTypeBasicImpl.class))
            .setPrettyPrinting().create();

    //a global counter for lastest id
    static AtomicInteger symId = new AtomicInteger(0);
    static ConstantCache cache = new ConstantCache();

    //which test case contributes to this template
    public String templateSource = "";

    //generated template could be problematic, thus we need to provide warning messages to show this is indeed
    //problematic, to make it convenience to collect global info
    public Set<TemplateWarning> warnings = new HashSet<>();

    //a global merged symbol map
    Map<String, Symbol> symMap = new HashMap<>();
    List<Constraint> constraints = new ArrayList<>();
    public List<Operation> operations = new ArrayList<>();
    public List<Assertion> assertions = new ArrayList<>();
    //see location in Assertion
    public Map<Integer, List<Integer>> triggerIndex = new HashMap<Integer, List<Integer>>();
    //some tests should only apply to certain system configs, this has nothing to do with **constraints** above
    Set<SysConfConstraint> sysconfConstraints = new HashSet<>();

    //this is to save Query for next assert op
    @Expose(serialize = false, deserialize = false)
    String cachedQuery;
    Operation cachedQueryOp;

    public CheckerTemplate(String templateSource) {
        this.templateSource = templateSource;
    }

    public synchronized void addOp(Operation op) {
        if(op.optypeStr.equals("ASSERTFAIL"))
        {
            Assertion assertion = new Assertion(Assertion.AssertType.FAIL, null);
            assertion.assertQueryOp = new FailOperation();
            assertion.assertionLocation = assertions.size() + 1;
            if(!operations.isEmpty()){
                assertion.lastTriggerOpHash = operations.get(operations.size()-1).hashCode();
                operations.get(operations.size() - 1).relatedAssertions.add(assertions.size() + 1);
            }           
            assertions.add(assertion);
            //operations.get(operations.size()-1);
        }
        else{
            operations.add(op);
            //System.out.println("#### now operations size is "+operations.size());
        }
    }

    public synchronized void addAssertion(Assertion assertion) {
        //load cached Query
        //assertion.addAssertQuery(cachedQueryOp);
        //System.out.println("operations.size()"+operations.size());
	assertion.assertionLocation = assertions.size() + 1;
	if(!operations.isEmpty()){
	    assertion.lastTriggerOpHash = operations.get(operations.size()-1).hashCode();
	    operations.get(operations.size() - 1).relatedAssertions.add(assertions.size() + 1);
	}  
        assertions.add(assertion);
    }

    public void addAssertQueryOp(Operation queryOp) {
        //Query come first and get cached
        cachedQueryOp = queryOp;
    }

    public void addConstraints(Constraint constraint) {
        constraints.add(constraint);
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public List<Assertion> getAssertions() {
        return assertions;
    }

    public Map<String, Symbol> getSymMap() {
        return symMap;
    }

    public Map<Integer, List<Integer>> getTriggerIndex(){
        return triggerIndex;
    }

    /*
    public Symbol searchSymbolInMap(String val) {
        for(Symbol symbol:symMap.values())
            if(symbol.val.equals(val))
                return symbol;
        return null;
    }
    */

    void truncateOpGeneral()
    {
        //first clip on the template to only keep the operations within two flags
        List<Operation> clippedOps = new ArrayList<>();

        boolean shouldAdd = false;
        for (Operation op : operations) {
            if (op.optypeStr.equals(Operation.OpTypeBasicImpl.TESTBODYBEGIN.toString())) {
                shouldAdd = true;
                continue;
            }

            if (op.optypeStr.equals(Operation.OpTypeBasicImpl.TESTBODYEND.toString())) {
                break;
            }

            if (shouldAdd)
                clippedOps.add(op);
        }
        //if TESTBODYBEGIN never appears, operations will remain not truncated
        if(shouldAdd)
            operations = clippedOps;
    }

    void truncateOpCustomized()
    {
        //zk: remove a pair of create, close sessions if no operation between
        List<Operation> clippedOps = new ArrayList<>();

        for (int i=0;i<operations.size()-1;++i) {
            Operation op = operations.get(i);
            Operation opNext = operations.get(i+1);
            if (op.optypeStr.equals("createSession")){// && opNext.optypeStr.equals("closeSession")) {
                //i++;
                continue;
            }

            clippedOps.add(op);
        }
        operations = clippedOps;
    }

    void truncateOp()
    {
        truncateOpGeneral();
        //truncateOpCustomized();
    }

    void truncateAssert()
    {
        List<Assertion> clippedAsserts = new ArrayList<>();
        boolean shouldAdd = false;
        for (Assertion assertion : assertions) {
            if (assertion.assertType.equals(Assertion.AssertType.TESTBODYBEGIN)){
                shouldAdd = true;
                continue;
            }

            if (assertion.assertType.equals(Assertion.AssertType.TESTBODYEND)) {
                break;
            }

            //if we failed to catch operation binded to this assertion, we would have a problem,
            //in this case we need to mark for such assertions
            if(assertion.assertQueryOp==null)
            {
                assertion.status = AssertStatus.MISSING_OP;
            }
            //else if(assertion.expected == null)
            //{
            //    assertion.status = AssertStatus.MISSING_EXPECTED;
            //}

            if (shouldAdd)
                clippedAsserts.add(assertion);
        }
        assertions = clippedAsserts;

    }

    //a template would look like this after init:
    //op1
    //BEGINTESTBODY
    //op2
    //ENDTESTBODY
    //op3
    // we need to clean it up by only keeping op2
    public synchronized void truncate() {
        truncateOp();
        truncateAssert();
    }

    public synchronized String serializeJson() {
        return gsonPrettyPrinter.toJson(this);
    }

    public synchronized void serializeJson(JsonWriter writer) {
        gsonPrettyPrinter.toJson(this,this.getClass(),writer);
    }

    static CheckerTemplate deserializeJson(String json) {
        return gsonPrettyPrinter.fromJson(json, CheckerTemplate.class);
    }

    static CheckerTemplate deserializeJson(JsonReader jsonReader) {
        return gsonPrettyPrinter.fromJson(jsonReader, CheckerTemplate.class);
    }

    private boolean checkConstraints(Map<String, Symbol> map)
    {
        for(Constraint constrain: constraints)
            if(!constrain.check(map)) return false;

        return true;
    }

    Boolean sysConfCache = null;
    private boolean checkSystemConfigConstraints()
    {
        if(sysConfCache!=null){
            return sysConfCache;
        }
        //this can happen is loaded from disk
        if(sysconfConstraints == null){
            sysConfCache = true;
            return true;
        }

        for(SysConfConstraint constrain: sysconfConstraints)
            if(!constrain.check()){
                sysConfCache = false;
                return false;
            }

        sysConfCache = true;
        return true;
    }

    public synchronized void mergeSymbolMap() {
        symMap = new HashMap<>();
        for (Operation op : operations)
            symMap.putAll(op.symbolMap);
        //for (Assertion assertion : assertions)
        //{
            //if(assertion.assertQueryOp!=null)
            //    symMap.putAll(assertion.assertQueryOp.symbolMap);
            //if(assertion.expected!=null)
            //    symMap.putAll(assertion.expected.symbolMap);
        //}

        //now this is a critical step, we want to merge symbols referring to same value
        //right now the policy is we would merge symbols with string type with same values
        //we do not include concrete number so this should be safe
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (String key : symMap.keySet()) {
            String val = Integer.toString(symMap.get(key).hashcode);
            if (!map.containsKey(val)) {
                map.put(val, new ArrayList<String>());
            }
            map.get(val).add(key);
        }
        for (String key : map.keySet()) {
            //redirect all alias to the first one
            String first = map.get(key).get(0);
            Symbol firstSym = symMap.get(first);
            for (int i = 1; i < map.get(key).size(); ++i) {
                //previously we just redirect but now we would some optimization here:
                //we remove symbols pointing to the same
                //symMap.put(map.get(key).get(i),firstSym);
                removeAlias(map.get(key).get(i), firstSym.symbolName);
            }
        }
        //to support inferring relations between primitives and objects, we also check if such pattern happens:
        //op1(workload1)
        //op2(workload2) -> workload2 is an object of {workload1, workload3}

        //retrieve a string set
        Map<String, Symbol> constantSet = new HashMap<String, Symbol>();
        for(Map.Entry<String, Symbol> entry: symMap.entrySet())
        {
            if(entry.getValue().typeName.equals("java.lang.String"))
                constantSet.put(entry.getValue().strVal,entry.getValue());
        }
        //find matchings in objects or arrays
        if(ENABLE_JSON_TO_STRING)
        {
            try{
                for(Map.Entry<String, Symbol> entry: symMap.entrySet())
                {
                    String symName = entry.getKey();
                    Symbol symbol = entry.getValue();
                    for(String constant: constantSet.keySet())
                    {
                        if(symbol.hashCode()!=-1 && !symbol.typeName.equals("java.lang.String")
                                && symbol.strVal.contains(constant))
                        {

                            JsonElement parentElem=
                                    JsonParser.parseString(symbol.strVal);
                            JsonElement newElem = JsonParser.parseString(constantSet.get(constant).symbolName);
                            if(parentElem.isJsonArray())
                            {
                                for (int i = 0; i < parentElem.getAsJsonArray().size(); ++i) {
                                    replaceAliasInJsonElemFromArray(symbol.subSymbols,parentElem, i, parentElem.getAsJsonArray().get(i), constant, newElem);
                                }
                            }
                            else if (parentElem.isJsonObject()) {
                                for (Map.Entry<String, JsonElement> entryj : parentElem.getAsJsonObject().entrySet()) {
                                    replaceAliasInJsonElemFromObject(symbol.subSymbols,parentElem, entryj.getKey(), entryj.getValue(), constant, newElem);
                                }
                            }
                            symbol.strVal = gsonPrettyPrinter.toJson(parentElem);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                throw ex;
            }
        }


        //handle names in assertions here, because we don't want to remove symbol in assert2 just because
        //it appeared in assert1, such symbol cannot be derived from traces
        for(Assertion assertion : assertions)
        {
            if(assertion.assertQueryOp!=null)
            {
                Map<String, Symbol> copiedSymbolMap = new HashMap<>(assertion.assertQueryOp.symbolMap);
                for(Map.Entry<String, Symbol>  entry:copiedSymbolMap.entrySet())
                {
                    String val = Integer.toString(entry.getValue().hashcode);
                    if(map.containsKey(val))
                    {
                        String first = map.get(val).get(0);
                        Symbol firstSym = symMap.get(first);
                        removeAlias(entry.getKey(), firstSym.symbolName);
                    }
                }
            }
        }

        symId.set(0);
    }

    //a template can be successfully generated but incomplete to execute, we need to check here
    public void checkStatus() {
        if(operations.size()==0)
        {
            warnings.add(TemplateWarning.NO_TRIGGERING_OP);
        }

        int validCount = 0;
        for(Assertion assertion:assertions)
            if(assertion.status.equals(AssertStatus.NORMAL))
            {
                validCount++;
            }
        if(validCount==0)
            warnings.add(TemplateWarning.NO_VALID_ASSERTION);

    }

    public void removeAlias(String oldSym, String newSym) {
        for (Operation op : operations)
            op.removeAlias(oldSym, newSym);
        for (Assertion assertion : assertions)
        {
            if(assertion.assertQueryOp != null)
                assertion.assertQueryOp.removeAlias(oldSym,newSym);
            //if(assertion.expected != null)
            //    assertion.expected.removeAlias(oldSym, newSym);
        }
        Operation.removeAlias(symMap, oldSym, newSym);
    }

    //public void replaceAliasInJsonElemFromArray(JsonElement parentElem, int index, JsonElement oldElem, String constant, JsonElement newElem)
    //{
    //    if(oldElem.isJsonPrimitive() && ((JsonPrimitive)oldElem).isString())
    //    {
    //        if(constant.equals("\""+oldElem.getAsString()+"\""))
    //        {
    //            parentElem.getAsJsonArray().set(index,newElem);
    //        }
    //    }
    //    else if (oldElem.isJsonArray()){
    //        for(int i=0;i<oldElem.getAsJsonArray().size();++i)
    //            replaceAliasInJsonElemFromArray(oldElem,i,oldElem.getAsJsonArray().get(i),constant,newElem);
    //    }
    //    else if(oldElem.isJsonObject()){
    //        for(Map.Entry<String, JsonElement> entry: oldElem.getAsJsonObject().entrySet())
    //            replaceAliasInJsonElemFromObject(oldElem,entry.getKey(),entry.getValue(),constant,newElem);
    //    }
    //}
    //
    //public void replaceAliasInJsonElemFromObject(JsonElement parentElem, String key, JsonElement oldElem, String constant, JsonElement newElem)
    //{
    //        if(oldElem.isJsonPrimitive() && ((JsonPrimitive)oldElem).isString())
    //        {
    //            if(constant.equals("\""+oldElem.getAsString()+"\""))
    //            {
    //                parentElem.getAsJsonObject().add(key,newElem);
    //            }
    //        }
    //        else if (oldElem.isJsonArray()){
    //            for(int i=0;i<oldElem.getAsJsonArray().size();++i)
    //                replaceAliasInJsonElemFromArray(oldElem,i,oldElem.getAsJsonArray().get(i),constant,newElem);
    //        }
    //        else if(oldElem.isJsonObject()){
    //            for(Map.Entry<String, JsonElement> entry: oldElem.getAsJsonObject().entrySet())
    //                replaceAliasInJsonElemFromObject(oldElem,entry.getKey(),entry.getValue(),constant,newElem);
    //        }
    //}

    public void replaceAliasInJsonElemFromArray(Set<String> subSymbols,JsonElement parentElem, int index, JsonElement oldElem, String constant, JsonElement newElem) {
        replaceAliasInJsonElement(subSymbols, parentElem,oldElem, constant, newElem, (parent, newElement) -> parent.getAsJsonArray().set(index, newElement));
    }

    public void replaceAliasInJsonElemFromObject(Set<String> subSymbols,JsonElement parentElem, String key, JsonElement oldElem, String constant, JsonElement newElem) {
        replaceAliasInJsonElement(subSymbols, parentElem,oldElem, constant, newElem, (parent, newElement) -> parent.getAsJsonObject().add(key, newElement));
    }

    private void replaceAliasInJsonElement(Set<String> subSymbols,JsonElement parentElem,JsonElement oldElem, String constant, JsonElement newElem, BiConsumer<JsonElement, JsonElement> replacer) {
        if (oldElem.isJsonPrimitive() && ((JsonPrimitive) oldElem).isString()) {
            if (constant.equals("\"" + oldElem.getAsString() + "\"")) {
                replacer.accept(parentElem, newElem);
                subSymbols.add(newElem.toString().replace("\"", ""));
            }
        } else if (oldElem.isJsonArray()) {
            for (int i = 0; i < oldElem.getAsJsonArray().size(); ++i) {
                replaceAliasInJsonElemFromArray(subSymbols,oldElem, i, oldElem.getAsJsonArray().get(i), constant, newElem);
            }
        } else if (oldElem.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : oldElem.getAsJsonObject().entrySet()) {
                replaceAliasInJsonElemFromObject(subSymbols, oldElem, entry.getKey(), entry.getValue(), constant, newElem);
            }
        }
    }

    //return the list of matched traces, or null if unmatched
    public void compare(EventList<Operation> traces, boolean isOwnTest, List<Operation> matched, HashSet<Integer> matched_traces, Map<Integer, List<Operation>> retMap) {
        if(operations.isEmpty())
        {
            //template is empty
            return;
        }

        for(Map.Entry<Integer, List<Integer>> entry: triggerIndex.entrySet())
        {
            //match last n elems
            int len = entry.getKey();
            //note this is not func ID, this id starts from 0
            List<Integer> assertIDs = entry.getValue();
            int index = traces.size() - len;

            //if traces even not have enough operations, just skip
            if (len > traces.size()) {
                //T2CHelper.prodLogInfo("size not match: "+templateSource+"'s "+assertIDs+" have "+len
                //       +", while traces has " +traces.size());
                continue;
            }

            // reset the reusable list
            matched.clear();

            if(!isOwnTest || len > 30 || (len == 2 && !operations.get(0).optypeStr.equals("createSessionRequest") && !operations.get(1).optypeStr.equals("createSessionRequest")))
                for (int i = 0; i < len; ++i) {
                    Operation trace = traces.get(index + i);
                    if (!operations.get(i).equals(trace)) {
                        //T2CHelper.prodLogInfo("operation not match: "+operations.get(i).toString()+" != "
                        //        +trace.toString());
                        matched.clear();
                        break;
                    }
                    matched.add(trace);
                }
            else{
                matched_traces.clear();
                for(int i = 0; i < len; ++i){
                    int j;
                    Operation op = operations.get(i);
                    for(j = index; j < traces.size(); ++j){
                        if(op.equals(traces.get(j)) && !matched_traces.contains(j)){
                            T2CHelper.prodLogInfo("#### when matching its own template, add an op into matched traces: No."+(i+1)+"/"+len+" in template, No."+(j-index+1)+" in traces");
                            matched.add(traces.get(j));
                            matched_traces.add(j);
                            break;
                        }
                    }
                    if(j == traces.size()){
                        if(op.optypeStr.equals("createSessionRequest")){
                            //T2CHelper.prodLogInfo("#### try to match createSessionRequest");
                            for(int k = 0; k<index; ++k){
                                if(op.equals(traces.get(k))){
                                    //T2CHelper.prodLogInfo("#### add createSessionRequest out of range");
                                    matched.add(traces.get(k));
                                    j = k;
                                    break;
                                }
                            }
                        }
                        if(j == traces.size()){
                            T2CHelper.prodLogInfo("#### clear own match, now i = " + i);
                            matched.clear();
                            break;
                        }
                    }
                }
            }
            if(!matched.isEmpty())
            {
                for(int i = 0; i<assertIDs.size();++i)
                    retMap.put(assertIDs.get(i),matched);
            }
        }
    }

    public void computeEditDistance(List<Operation> traces) {
        int result = editDist(operations,traces.subList(Math.max(traces.size()-operations.size(),0),traces.size()),operations.size(),traces.size());
        T2CHelper.prodLogInfo("Edit distance is "+result);
    }

    private int editDist(List<Operation> list1, List<Operation> list2, int m, int n)
    {
        if (m == 0)
            return n;
        if (n == 0)
            return m;
        if (list1.get(m - 1).equals(list2.get(n - 1)))
            return editDist(list1, list2, m - 1, n - 1);

        return 1
                + Math.min(editDist(list1, list2, m, n - 1), // Insert
                Math.min(editDist(list1, list2, m - 1, n), // Remove
                editDist(list1, list2, m - 1,
                        n - 1)) // Replace
        );
    }

    public Map<String, Symbol> trySubstitute(List<Operation> traces, Map<String, Symbol> map) {
        for(int i=0;i<traces.size();++i)
        {
            Operation trace = traces.get(i);
            Operation op = operations.get(i);

            Map<String, Symbol> comparedMap = op.compare(trace);
            //T2CHelper.prodLogInfo("#### op is:" + gsonPrettyPrinter.toJson(op));
            //T2CHelper.prodLogInfo("#### trace is:"+gsonPrettyPrinter.toJson(trace));
            if(comparedMap==null)
            {
                //T2CHelper.prodLogInfo("#### comparedMap is null");
                //System.err.println(gsonPrettyPrinter.toJson(op));
                //System.err.println(gsonPrettyPrinter.toJson(trace));
                //conflicts and we abort
                return null;
            }
            else
            {
                for(String newkey:comparedMap.keySet())
                {
                    if(map.containsKey(newkey))
                    {
                        //if the symbol cannot merge
                        if(!map.get(newkey).equals(comparedMap.get(newkey)))
                        {
                            //T2CHelper.prodLogInfo("#### can't merge newkey: "+ newkey);
                            return null;
                        }
                    }
                    else
                        map.put(newkey,comparedMap.get(newkey));
                }
            }
        }

        return map;
    }

    public static void match(EventList<Operation> traces, List<CheckerTemplate> candidates, List<Operation> matched, HashSet<Integer> matched_traces, Map<Integer, List<Operation>> matchedCheckers, Map<String, Symbol> updatedMap, Map<CheckerTemplate.TemplateWithMap, Integer> retMap) {
        T2CHelper.prodLogInfo("Try matching with "+candidates.size()+" templates, trace size is "+ traces.size());
        //by default the strategy returns all matched templates
        for (CheckerTemplate template : candidates) {
            if(!template.checkSystemConfigConstraints())
            {
                //T2CHelper.prodLogInfo("Template "+ template.templateSource+" failed to pass system config constraint test.");
                continue;
            }

            //do initial test
            boolean isOwnTest = false;
            //T2CHelper.prodLogInfo("#### templateSource: "+template.templateSource+", lastTestName: "+T2CHelper.getInstance().lastTestName);
            if(template.templateSource.equals(T2CHelper.getInstance().lastTestName)){
                //T2CHelper.prodLogInfo("#### now match its own template");
                isOwnTest = true;
            }

            matchedCheckers.clear();
            matched.clear();
            matched_traces.clear();
            template.compare(traces, isOwnTest, matched, matched_traces, matchedCheckers);
            //for testing
            //template.computeEditDistance(traces);

            if(matchedCheckers == null)
                continue;
            if(!matchedCheckers.isEmpty()){
                // T2CHelper.prodLogInfo("Template "+ template.templateSource+" passed initial checks, to be subtituted.");
                for(Map.Entry<Integer, List<Operation>> entry: matchedCheckers.entrySet())
                {
                    int index = entry.getKey();

                    int thisCustomizedOpId = template.assertions.get(index).assertQueryOp.customizedOpID;

                    // Increase quota when appropriate
                    long now = System.currentTimeMillis();
                    if(RuntimeTracer.getInstance().rateLimiter.containsKey(thisCustomizedOpId) &&
                            (now-RuntimeTracer.getInstance().rateRefresher.get(thisCustomizedOpId)) > RuntimeTracer.getInstance().rateLimitRefresh &&
                            RuntimeTracer.getInstance().rateLimiter.get(thisCustomizedOpId).get() < RuntimeTracer.getInstance().rateLimitSize){
                        RuntimeTracer.getInstance().rateLimiter.get(thisCustomizedOpId).incrementAndGet();
                        RuntimeTracer.getInstance().rateRefresher.put(thisCustomizedOpId, now);
                    }

                    // Skip if exceeding rate limit
                    if(GlobalState.mode.equals(GlobalState.T2CMode.PRODUCTION) && RuntimeTracer.getInstance().rateLimiter.containsKey(thisCustomizedOpId) &&
                            RuntimeTracer.getInstance().rateLimiter.get(thisCustomizedOpId).get() == 0){
                        continue;
                    }

                    // Put if not in rate limit map yet
                    if(!RuntimeTracer.getInstance().rateLimiter.containsKey(thisCustomizedOpId)){
                        RuntimeTracer.getInstance().rateLimiter.put(thisCustomizedOpId, new AtomicInteger(RuntimeTracer.getInstance().rateLimitSize));
                        RuntimeTracer.getInstance().rateRefresher.put(thisCustomizedOpId, now);
                    }

                    List<Operation> matchedTraces = entry.getValue();

                    // if length and type match, we move forward
                    updatedMap.clear();
                    template.trySubstitute(matchedTraces, updatedMap);

                    //for testing, when you want to just match on operation level
                    boolean IGNORE_SYMBOL_MATCHING = false;
                    if(!IGNORE_SYMBOL_MATCHING)
                    {
                        //conflicts happened
                        if(updatedMap==null){
                            //T2CHelper.prodLogInfo("Template "+ template.templateSource+" failed to be subtituted.");
                            continue;
                        }
                        //final check: see if constraints are satisfied
                        if(!template.checkConstraints(updatedMap))
                        {
                            //T2CHelper.prodLogInfo("Template "+ template.templateSource+" failed to pass constraint test.");
                            continue;
                        }
                        //T2CHelper.prodLogInfo("Template "+ template.templateSource+" succeed to be added to check.");
                    }

                    //add all symbols from global symbol map if not
                    template.symMap.forEach(updatedMap::putIfAbsent);

                    TemplateWithMap map = new TemplateWithMap(template, updatedMap);
                    retMap.put(map, index);

                    // Reduce quota
                    RuntimeTracer.getInstance().rateLimiter.get(thisCustomizedOpId).decrementAndGet();
                }
            }
        }
    }

    public static void match(List<Operation> traces, Trie trieObj, Map<String, Symbol> updatedMap, Map<CheckerTemplate.TemplateWithMap, Integer> retMap) {
        //T2CHelper.prodLogInfo("Try matching "+traces.size());
        //template, Map<assertionIdx, matchedOp>
        // Map<CheckerTemplate, Map<Integer, List<Operation>>> matchedCheckers = trieObj.search(traces.drain(traces.index()));
        Map<CheckerTemplate, Map<Integer, List<Operation>>> matchedCheckers = trieObj.search(traces);

        String opStr = " OPSTR "+traces.size()+" ";
        String searchStr = " SEARCHSTR "+matchedCheckers.size()+" ";
        String retStr = " RETSTR ";
        for(Operation op: traces){
            opStr += op.getOptypeStr()+"<-";
        }

        if (matchedCheckers.isEmpty()){
            return;
        }

        for (Map.Entry<CheckerTemplate, Map<Integer, List<Operation>>> outerEntry: matchedCheckers.entrySet()){
            CheckerTemplate template = outerEntry.getKey();
            searchStr += template.templateSource+"-";

            for (Map.Entry<Integer, List<Operation>> innerEntry: outerEntry.getValue().entrySet()){
                int index = innerEntry.getKey();
                List<Operation> matchedTraces = innerEntry.getValue();

                // if length and type match, we move forward
                template.trySubstitute(matchedTraces, updatedMap);
                //for testing, when you want to just match on operation level
                boolean IGNORE_SYMBOL_MATCHING = false;
                if(!IGNORE_SYMBOL_MATCHING)
                {
                    //conflicts happened
                    if(updatedMap==null){
                        //T2CHelper.prodLogInfo("Template "+ template.templateSource+" failed to be subtituted.");
                        continue;
                    }
                    //final check: see if constraints are satisfied
                    if(!template.checkConstraints(updatedMap))
                    {
                        //T2CHelper.prodLogInfo("Template "+ template.templateSource+" failed to pass constraint test.");
                        continue;
                    }
                    //T2CHelper.prodLogInfo("Template "+ template.templateSource+" succeed to be added to check.");
                }

                //add all symbols from global symbol map if not
                template.symMap.forEach(updatedMap::putIfAbsent);

                TemplateWithMap map = new TemplateWithMap(template, updatedMap);
                retMap.put(map, index);
                retStr += template.templateSource+"-";
            }
        }
        retStr += " "+retMap.size();

        T2CHelper.prodLogInfo(opStr+searchStr+retStr);
    }

    public CheckerTemplate clone()
    {
        CheckerTemplate template = new CheckerTemplate(templateSource);
        template.operations = new ArrayList<>(operations);
        template.assertions = new ArrayList<>(assertions);
        template.constraints = new ArrayList<>(constraints);
        template.symMap = new HashMap<>(symMap);
        return template;
    }

    public void addSysConfigConstraint(String key, String value)
    {
        sysconfConstraints.add(new SysConfConstraint(key, value));
    }
}
