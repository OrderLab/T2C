package edu.jhu.order.t2c.staticd.algorithm;

import com.google.common.collect.ImmutableList;
import edu.jhu.order.t2c.staticd.option.T2COptions;
import edu.jhu.order.t2c.staticd.unit.Mutation;
import edu.jhu.order.t2c.staticd.util.SootUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Body;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.*;

import java.util.ArrayList;
import java.util.List;

public class MutationTracker {

    private static final Logger LOG = LoggerFactory.getLogger(MutationTracker.class);

    //some config
    public static boolean TRACK_PRIM_TYPE_MUTATION = true;
    public static boolean USE_FIELD_BLACK_LIST = true;
    public static boolean USE_FIELD_WHITE_LIST = true;

    //
    private static Set<SootClass> classBlacklist = new HashSet<>();
    //disabled fields
    private static Set<SootField> fieldBlacklist = new HashSet<>();
    //
    private static Set<SootField> fieldWhitelist = new HashSet<>();

    //some stats
    private static int countPrims = 0;
    private static int countCollections = 0;
    private static Set<String> collectionMethods = new HashSet<>();
    private static Map<SootField, Integer> fieldUsageCount = new HashMap<>();

    //local vars that are pointing to a field
    private List<Value> fieldLocals = new ArrayList<>();
    private Map<Value, FieldRef> fieldLocal2RefMap = new HashMap<>();
    //a map that contains definition stmt for fieldref
    private Map<Value, Stmt> fieldDefStmts = new HashMap<>();

    class UsageInfo {

        Value base;
        SootField field;
        SootMethod op;

        public UsageInfo(Value base, SootField field, SootMethod op) {
            this.base = base;
            this.field = field;
            this.op = op;
        }
    }

    static final ImmutableList<String> mutationOps =
            ImmutableList.of("put", "remove", "empty", "pop", "removeFirst", "putAll", "add", "set",
                    "clear", "push", "putIfAbsent", "=", "arr=", "compareAndSet");

    static final ImmutableList<String> getOps =
            ImmutableList.of("get");

    //even after so many filtering, there would still be some data fields that cannot be filtered out,
    // the naming rule is the last defense here, if the field name contains such keyword we would filter
    // you out,
    static final ImmutableList<String> disabledKeywords =
            ImmutableList.of("bean", "Bean");

    public static void init() {
        loadBlacklistFromExcludedPkg();
        loadBlacklistFromClientEntry();
        loadWhitelistFromMainEntry();
    }

    private static void loadBlacklistFromSingleClass(SootClass c) {
        if (c.getName().startsWith("java.")) {
            return;
        }

        String prefix = T2COptions.getInstance().getSystemPackagePrefix();
        if (!c.getName().startsWith(prefix)) {
            return;
        }

        classBlacklist.add(c);

        //add fields to blacklist
        for (SootField f : c.getFields()) {
            if (SootUtils.ifPrimJavaType(f.getType()) ||
                    SootUtils.ifCollectionJavaType(f.getType())) {
                fieldBlacklist.add(f);
            }
        }
    }

    private static void loadWhitelistFromSingleClass(SootClass c) {
        String prefix = T2COptions.getInstance().getSystemPackagePrefix();
        if(prefix == null) {
            LOG.warn("no prefix for whitelist loaded");
            return;
        }

        if (!c.getName().startsWith(prefix)) {
            return;
        }

        //add fields to whitelist
        for (SootField f : c.getFields()) {
            //if (SootUtils.ifPrimJavaType(f.getType()) ||
            //        SootUtils.ifCollectionJavaType(f.getType()))
            {
                fieldWhitelist.add(f);
            }
        }
    }

    /**
     * some classes e.g. monitoring type (jmx) should be excluded from mutation tracking
     */
    private static void loadBlacklistFromExcludedPkg() {
        String exPkg = T2COptions.getInstance().getExcludedPackage();
        if(exPkg == null) {
            LOG.warn("no blacklist for excluded pkg loaded");
            return;
        }

        for (SootClass c : Scene.v().getApplicationClasses()) {
            //first check if the class itself is in the classBlacklist
            if (c.getName().contains(exPkg)) {
                loadBlacklistFromSingleClass(c);
            }

            //then check if the class implements the interface in the classBlacklist
            //e.g. ConnectionBean implements ZKMBeanInfo
            for (SootClass interf : c.getInterfaces()) {
                if (interf.getName().contains(exPkg)) {
                    loadBlacklistFromSingleClass(c);
                }
            }
        }
    }

    private static void loadBlacklistFromClientEntry() {
        String clientEntry = T2COptions.getInstance().getClientSideEntry();
        if(clientEntry == null) {
            LOG.warn("no blacklist for client loaded");
            return;
        }

        SootClass entryClass = Scene.v().loadClassAndSupport(clientEntry);

        Set<SootClass> historySet = new HashSet<>();

        Queue<SootClass> queue = new LinkedList<>();
        queue.offer(entryClass);
        while (!queue.isEmpty()) {
            SootClass parent = queue.poll();

            if (historySet.contains(parent)) {
                continue;
            }

            historySet.add(parent);
            loadBlacklistFromSingleClass(parent);

            if (parent.hasSuperclass()) {
                LOG.debug("blacklist trace: " + parent.getName() + "->" + parent.getSuperclass()
                        .getName());
                queue.offer(parent.getSuperclass());
            }

            for (SootField field : parent.getFields()) {
                if (field.getType() instanceof RefType) {
                    if (((RefType) field.getType()).getClassName().startsWith("java.")) {
                        continue;
                    }

                    SootClass c = ((RefType) field.getType()).getSootClass();
                    LOG.debug("blacklist trace: " + parent.getName() + "->" + c.getName());
                    queue.offer(c);
                }
            }
        }
    }

    private static void loadWhitelistFromMainEntry() {
        String mainEntry = T2COptions.getInstance().getMainClass();
        SootClass entryClass = Scene.v().loadClassAndSupport(mainEntry);

        Set<SootClass> historySet = new HashSet<>();
        Queue<SootClass> queue = new LinkedList<>();
        queue.offer(entryClass);
        while (!queue.isEmpty()) {
            SootClass parent = queue.poll();

            if (historySet.contains(parent)) {
                continue;
            }

            if (parent.getName().startsWith("java.")) {
                continue;
            }

            if (parent.getName().equals("org.apache.zookeeper.ZooKeeper") ||
                    parent.getName().equals("org.apache.zookeeper.server.ZooKeeperThread")) {
                continue;
            }

            historySet.add(parent);
            loadWhitelistFromSingleClass(parent);

            //should includes fields in superclass as well
            if (parent.hasSuperclass()) {
                LOG.debug("whitelist trace: " + parent.getName() + "->" + parent.getSuperclass()
                        .getName());
                queue.offer(parent.getSuperclass());
            }

            for (SootClass c : SootUtils.getSubClass(parent)) {
                LOG.debug("whitelist trace: " + parent.getName() + "->" + c.getName());
                queue.offer(c);
            }

            for (SootClass c : SootUtils.getImpls(parent)) {
                LOG.debug("whitelist trace: " + parent.getName() + "->" + c.getName());
                queue.offer(c);
            }

            for (SootField field : parent.getFields()) {
                if (field.getType() instanceof RefType) {

                    SootClass c = ((RefType) field.getType()).getSootClass();
                    queue.offer(c);
                    LOG.debug("whitelist trace: " + parent.getName() + "->" + c.getName());
                }
            }
        }
    }

    private void checkDefs(Stmt stmt) {
        //first check if this stmt defines new field reference (important states)
        //e.g. $r1 = this.<org.apache.zookeeper.server.DataTree:
        // java.util.concurrent.ConcurrentHashMap nodes>
        if (stmt instanceof AssignStmt) {
            //we need to consider two scenarios here
            //1)simple assign from field: map.put();
            //2) cast: HashSet<String> newSet2 = (HashSet<String>) newSet;
            //3) nested: nestedMap.get(0).put(1,"foo");

            //check 2)
            if (((AssignStmt) stmt).getRightOp() instanceof CastExpr) {
                Value fieldLocalCandidate = ((AssignStmt) stmt).getLeftOp();
                Value base = ((CastExpr) (((AssignStmt) stmt).getRightOp())).getOp();
                if (!fieldLocals.contains(base)) {
                    return;
                }

                fieldLocal2RefMap.put(fieldLocalCandidate, fieldLocal2RefMap.get(base));
                fieldLocals.add(fieldLocalCandidate);
                fieldDefStmts.put(fieldLocalCandidate, stmt);
                return;
            }

            //check 3)
            //e.g. $r2 = interfaceinvoke $r0.<java.util.Map: java.lang.Object get(java.lang.Object)>($r1);
            if (stmt.containsInvokeExpr()) {
                Value fieldLocalCandidate = ((AssignStmt) stmt).getLeftOp();
                Type candidateType = fieldLocalCandidate.getType();
                Value base = SootUtils.getBaseFromInvokerExpr(stmt);
                //skip static invoke
                if (base == null) {
                    return;
                }

                if (SootUtils.ifCollectionJavaType(base.getType())) {
                    //base must appear before
                    if (!fieldLocals.contains(base)) {
                        return;
                    }

                    if (getOps.contains(stmt.getInvokeExpr().getMethod().getName())) {
                        fieldLocal2RefMap.put(fieldLocalCandidate, fieldLocal2RefMap.get(base));
                        fieldLocals.add(fieldLocalCandidate);
                        fieldDefStmts.put(fieldLocalCandidate, stmt);
                        return;
                    }
                }
            }

            //check 1)
            if (stmt.containsFieldRef()) {
                //LOG.info(((AssignStmt) stmt).getLeftOp().getType().toString());
                // we would add $r1 here to list
                Value fieldLocalCandidate = ((AssignStmt) stmt).getLeftOp();

                Type candidateType = fieldLocalCandidate.getType();
                //we do not deal with complex types -- they should be handled in their own classes
                //if (SootUtils.ifPrimJavaType(candidateType) ||
                //        SootUtils.ifCollectionJavaType(candidateType)
                //        )
                {

                    //check if mutated states come from classBlacklist class
                    //e.g. org.apache.zookeeper.jmx.MBeanRegistry#mapBean2Path
                    if (classBlacklist
                            .contains(stmt.getFieldRef().getField().getDeclaringClass())) {
                        return;
                    }

                    //check if the field name contains something we disabled, e.g. bean
                    // if so we add the missing field to blacklist and run away
                    String fieldName = stmt.getFieldRef().getField().getName();
                    if (USE_FIELD_BLACK_LIST) {
                        for (String keyword : disabledKeywords) {
                            if (fieldName.contains(keyword)) {
                                //fieldBlacklist.add(stmt.getFieldRef().getField());
                                return;
                            }
                        }
                    }

                    //VERY IMPORTANT!
                    // our rule is: if the field is in the whitelist but not in the blacklist,
                    // we include it
                    if (USE_FIELD_BLACK_LIST && fieldBlacklist
                            .contains(stmt.getFieldRef().getField())) {
                        return;
                    }

                    if (USE_FIELD_WHITE_LIST && !fieldWhitelist
                            .contains(stmt.getFieldRef().getField())) {
                        return;
                    }

                    fieldLocal2RefMap.put(fieldLocalCandidate, stmt.getFieldRef());
                    fieldLocals.add(fieldLocalCandidate);
                    fieldDefStmts.put(fieldLocalCandidate, stmt);
                }
            }
        }
    }

    private UsageInfo checkUsage(Stmt stmt) {

        //now checks the usage
        //currently we support two types of mutations:
        // 1) modificationsto map, set, list
        // 2) set field: foo.bar = 1
        // 3) AtomicReferenceFieldUpdater
        // 4) array type: $r0[0] = 1;

        Value base = null;
        SootMethod op = null;
        //1)
        if (stmt.containsInvokeExpr()) {
            //e.g. virtualinvoke $r8.<java.util.concurrent.ConcurrentHashMap:
            // java.lang.Object put(java.lang.Object,java.lang.Object)>(path, $r7);
            // also consider
            // xx = a.put();
            base = SootUtils.getBaseFromInvokerExpr(stmt);
            op = stmt.getInvokeExpr().getMethod();

            //check classBlacklist again
            //essentially we are checking for any args type comes from classBlacklist
            //consider org.apache.zookeeper.server.ServerCnxnFactory#connectionBeans
            // this one is a map of connectionBean but we can not tell at defs,
            // so we have to wait until when using it to check the arg types
            for (Value arg : stmt.getInvokeExpr().getArgs()) {
                Type argType = arg.getType();
                if (argType instanceof RefType) {
                    if (classBlacklist.contains(((RefType) argType).getSootClass())) {
                        return null;
                    }
                }
            }
        }

        //2) && 4)
        if (base == null && stmt instanceof AssignStmt) {
            base = ((AssignStmt) stmt).getLeftOp();

            //4)
            if(base instanceof ArrayRef)
            {
                base = ((ArrayRef) base).getBase();

                //surely assignment has no sootmethod, we creates an artfical one
                op = new SootMethod("arr=", new ArrayList<>(), VoidType.v());
            }
            else {

                //2)
                // we do not count set to lists, only count prim types
                if (!SootUtils.ifPrimJavaType(base.getType())) {
                    return null;
                }

                if (!(base instanceof FieldRef))
                    return null;
            }

            //surely assignment has no sootmethod, we creates an artfical one
            op = new SootMethod("=", new ArrayList<>(), VoidType.v());
            op.setDeclaringClass(new SootClass("T2C"));
            op.setDeclared(true);

        }

        //3)
        if (base == null && stmt.containsInvokeExpr()) {
            base = SootUtils.getBaseFromInvokerExpr(stmt);
            op = stmt.getInvokeExpr().getMethod();

            if(base != null) {
                if (base.getType() instanceof RefType) {
                    if (((RefType) (base.getType())).getSootClass().getName()
                            .equals("java.util.concurrent.atomic.AtomicReferenceFieldUpdater")
                            && op.getName().equals("compareAndSet")) {
                        //return the second arg
                        base = stmt.getInvokeExpr().getArg(1);

                    }
                }
            }
        }

        //if base does not exist in refs, no need to bother
        if (base == null || !fieldLocals.contains(base) ||
                !mutationOps.contains(op.getName())) {
            return null;
        }

        if (op.getName().equals("=")) {
            if (!TRACK_PRIM_TYPE_MUTATION) {
                return null;
            }

            countPrims++;
        } else {
            countCollections++;
            collectionMethods.add(op.getName());
        }
        incrementRefUsage(fieldLocal2RefMap.get(base).getField());

        return new UsageInfo(base, fieldLocal2RefMap.get(base).getField(), op);
    }

    private void incrementRefUsage(SootField field) {
        if (fieldUsageCount.containsKey(field)) {
            fieldUsageCount.put(field, fieldUsageCount.get(field) + 1);
        } else {
            fieldUsageCount.put(field, 1);
        }
    }

    /**
     * get mutations
     */
    public List<Mutation> scan(Body body) {
        List<Mutation> mutations = new ArrayList<>();
        try {
            for (Unit unit : body.getUnits()) {
                Stmt stmt = (Stmt) unit;

                checkDefs(stmt);
                UsageInfo usageInfo = checkUsage(stmt);

                if (usageInfo == null) {
                    continue;
                }
                //add def
                Stmt defStmt = fieldDefStmts.get(usageInfo.base);
                //add usage
                Stmt usageStmt = stmt;

                mutations.add(new Mutation(usageInfo.op,
                        usageInfo.field,
                        usageStmt,
                        defStmt.toString(),
                        usageStmt.toString()));

            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return mutations;
    }


    public static void dumpStats() {
        LOG.info("countPrims:" + countPrims);
        LOG.info("countCollections:" + countCollections);
        LOG.info("collectionMethods:" + collectionMethods.size());
        //for (String methodName : collectionMethods) {
        //    LOG.info("\t" + methodName);
        //}

        //for (SootField field : JavaUtils.sortByValue(fieldUsageCount).keySet()) {
        //    LOG.info(field.getName() + "@" + field.getDeclaringClass().getName() + " "
        //            + fieldUsageCount.get(field).toString());
        //}

        LOG.info("classBlacklist:" + classBlacklist.size());
        for (SootClass c : classBlacklist) {
            LOG.info("\t" + c.getName());
        }

        LOG.info("fieldBlacklist:" + fieldBlacklist.size());
        for (SootField f : fieldBlacklist) {
            LOG.info("\t" + f.getDeclaringClass().getName() + "@" + f.getName());
        }

        LOG.info("fieldWhitelist:" + fieldWhitelist.size());
        for (SootField f : fieldWhitelist) {
            LOG.info("\t" + f.getDeclaringClass().getName() + "@" + f.getName());
        }

        Set<SootField> intersection = new HashSet<>(fieldBlacklist); // use the copy constructor
        intersection.retainAll(fieldWhitelist);
        Set<SootClass> classSet = new HashSet<>();
        LOG.info("intersection:" + intersection.size());
        for (SootField f : intersection) {
            LOG.info("\t" + f.getDeclaringClass().getName() + "@" + f.getName());
            classSet.add(f.getDeclaringClass());
        }
        LOG.info("intersection classSet:" + classSet.size());
        for (SootClass c : classSet) {
            LOG.info("\t" + c.getName());
        }
    }
}
