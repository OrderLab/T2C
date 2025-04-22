package edu.jhu.order.t2c.staticd.algorithm;

import edu.jhu.order.t2c.staticd.util.SootUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JNopStmt;
import soot.jimple.internal.JimpleLocal;
import soot.tagkit.GenericAttribute;
import soot.tagkit.Tag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.jimple.StringConstant;

import java.util.*;
import java.util.stream.Collectors;

//do an analysis to get the param of generated function, as well as related operations to be put in assertion
public class ParamAnalysis extends BackwardFlowAnalysis<Unit, Set> {
    private static final Logger LOG = LoggerFactory.getLogger(ParamAnalysis.class);

    static AssertFlag assertFlag = new AssertFlag("assertFlag",NullType.v());
    static StopFlag stopFlag = new StopFlag("stopFlag",NullType.v());

    public Set<Value> intermediateLocals = new LinkedHashSet<>();
    public List<Stmt> tobeCopiedStmts = new ArrayList<>();
    public Set<Value> baseArgs = new LinkedHashSet<>();
    public List<Stmt> newLocalAssignStmts = new ArrayList();

    //we now replace the initialParams generation with the inner logic
    @Deprecated
    Set<Value> initialParams = new LinkedHashSet<>();
    Stmt assertStmt = null;
    LocalGenerator lg = null;

    //state var
    boolean ifFlowThroughAssert = false;

    private static final Set<String> SHOULD_INCLUDE_METHODS = new HashSet<String>(Arrays.asList(
    //        new String[] {"org.apache.hadoop.hbase.regionserver.InternalScanner#next"}
    ));

    public ParamAnalysis(Body body, List<Value> initialParams, Stmt assertStmt, LocalGenerator lg) {
        super(new BriefUnitGraph(body));

        this.initialParams = new LinkedHashSet<>(initialParams);
        intermediateLocals.addAll(initialParams);
        this.assertStmt = assertStmt;
        this.lg = lg;
        // some other initializations
        doAnalysis();

        //save the arguments set
        baseArgs = getFlowAfter(body.getUnits().getFirst());
        for(Local local: body.getLocals())
        {
            if(local.getName().equals("this"))
            {
                if(!baseArgs.contains(local))
                    baseArgs.add(local);
            }
        }

        baseArgs.remove(assertFlag);
        intermediateLocals.remove(assertFlag);
        baseArgs.remove(stopFlag);
        intermediateLocals.remove(stopFlag);
    }

    //@Override
    //protected boolean treatTrapHandlersAsEntries(){
    //    return true;
    //}

    @Override
    protected void merge(Set in1, Set in2, Set out) {
        // must analysis => out <- in1 union in2
        // may analysis => out <- in1 intersection in2
        out.clear();
        out.addAll(in1);
        out.addAll(in2);
    }

    @Override
    protected void copy(Set src, Set dest) {
        dest.clear();
        dest.addAll(src);
    }

    @Override
    protected Set newInitialFlow() {
        // return e.g., the empty set
        return new LinkedHashSet();

    }

    @Override
    protected Set entryInitialFlow() {
        //there could be several starting points for backward analysis, e.g. a throw exception could also be that,
        //so we should only start on the asserting branch
        return new LinkedHashSet();
        //return initialParams;
    }

    static class AssertFlag extends JimpleLocal {

        public AssertFlag(String name, Type t) {
            super(name, t);
        }

        @Override
        public String toString() {
            return "AssertFlag{}";
        }
    }

    static class StopFlag extends JimpleLocal {

        public StopFlag(String name, Type t) {
            super(name, t);
        }

        @Override
        public String toString() {
            return "StopFlag{}";
        }
    }

    boolean ifFlowThroughAssert(Set in)
    {
        //return ifFlowThroughAssert;
        return in.contains(assertFlag);
    }

    void setIfFlowThroughAssert(Set out)
    {
        out.add(assertFlag);
        //ifFlowThroughAssert = true;
    }

    @Override
    protected void flowThrough(Set in, Unit node, Set out) {
        //this is not good way as the control flow graph could look like:
        //   o
        //  / \
        //  o o
        //  \/
        //  o
        //if(graph.getTails().contains(node))
        //{
        //    LOG.info("tails"+graph.getTails().size());
        //    LOG.info("new entry:"+node);
        //    ifFlowThroughAssert = false;
        //}

        //LOG.info("XXX"+node.toString()+node.getClass().getName());
        //cannot comment this, would cause half of test conversion fails
        if(node instanceof ReturnVoidStmt)
        {
            //always include return statement, otherwise some gotos have no target
            tobeCopiedStmts.add((Stmt)node);
        }

        if(in.contains(stopFlag))
        {
            out.clear();
            out.addAll(in);
            return;
        }

        if (!ifFlowThroughAssert(in)) {
            //encounter an assertion statement
            if(node.equals(assertStmt))
            {
                logSet("in", in);
                LOG.info("stmt: " + node.toString());

                out.clear();
                //out.addAll(initialParams);
                gen(in, node, out, true);

                logSet("out", out);
                intermediateLocals.addAll(out);
                tobeCopiedStmts.add(assertStmt);

                setIfFlowThroughAssert(out);
            }

            //not assert, just simply return
            return;
        }

        //process a flow from an assertion statement
        logSet("in", in);
        LOG.info("stmt: " + node.toString());

        out.clear();
        out.addAll(in);
        //if in is empty, which means
        // 1) this is not a branch starting from asserts
        // 2) all args are resolved but there may still be control flow args if analyzer further
        //TODO: add me back!! this can result over add args
        //if(!in.isEmpty())
            // perform flow from in to out, through node
            // two phases, create new
            if (kill(in, node, out))
                gen(in, node, out);

        logSet("out", out);

        intermediateLocals.addAll(out);
    }

    //delete intermediate locals, if any, return true
    private boolean kill(Set in, Unit node, Set out) {
        Stmt stmt = (Stmt) node;
        if (stmt instanceof AssignStmt) {
            Value ret = ((AssignStmt) stmt).getLeftOp();

            if (((AssignStmt) stmt).getRightOp() instanceof FieldRef) {
                //skip this is a fieldref of **this**, e.g. $i1 = r0.<edu.jhu.order.t2c.staticd.cases.AssertionSample: int c>;
            } else if (((AssignStmt) stmt).getRightOp() instanceof NewExpr) {
                //skip this is newing a var, e.g. r41 = new org.apache.hadoop.fs.Path
                //should also remove
                return out.remove(ret);
            } else if (((AssignStmt) stmt).getRightOp() instanceof StringConstant)
            {

            }
            else if (((AssignStmt) stmt).getLeftOp() instanceof ArrayRef)
            {
                ArrayRef left = (ArrayRef)((AssignStmt) stmt).getLeftOp();
                if(out.contains(left.getBase()))
                    return true;
            }
            else {

                Value rightOp = ((AssignStmt) stmt).getRightOp();
                if(rightOp instanceof InvokeExpr)
                {
                    if(!MethodPurityAnalysis.ifMethodPure(((InvokeExpr)rightOp).getMethod()))
                        out.add(stopFlag);
                }

                return out.remove(ret);
            }
        }
        else if (stmt instanceof IdentityStmt)
        {
            if (((IdentityStmt) stmt).getRightOp() instanceof CaughtExceptionRef)
            {
                return true;
            }
        }
        else if (stmt instanceof InvokeStmt) {
            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
            SootMethod method = invokeExpr.getMethod();
            if (invokeExpr instanceof SpecialInvokeExpr) {
                //handle cases like
                // specialinvoke r41.<org.apache.hadoop.fs.Path: void <init>(org.apache.hadoop.fs.Path,java.lang.String)>($r9, "file1");
                if (invokeExpr.getMethod().getName().equals("<init>")) {
                    //should not return
                    //return out.remove(((SpecialInvokeExpr) invokeExpr).getBase());
                    return true;
                }
            }
            else if (invokeExpr instanceof VirtualInvokeExpr) {
                //if(IMPURE_METHODS.contains(invokeExpr.getMethod().getName()))
                //    return true;

                //include corner cases we found
                String fullName = method.getDeclaringClass().getName()+"#"+(method.getName());
                if(SHOULD_INCLUDE_METHODS.contains(fullName))
                    return true;

                //check if invokeExpr has any common elems compared with in set
                boolean ret = MethodPurityAnalysis.ifMethodPure(invokeExpr.getMethod());
                if(!ret)
                    out.add(stopFlag);
                if(!Collections.disjoint(invokeExpr.getArgs(), in) || in.contains(((VirtualInvokeExpr)invokeExpr).getBase()))
                {
                    return ret;
                }

                //dangerous, consider a better idea
               //return true;
            }
            else if(invokeExpr instanceof InterfaceInvokeExpr)
            {
                //include corner cases we found
                String fullName = method.getDeclaringClass().getName()+"#"+(method.getName());
                if(SHOULD_INCLUDE_METHODS.contains(fullName))
                    return true;

                //check if invokeExpr has any common elems compared with in set
                boolean ret = MethodPurityAnalysis.ifMethodPure(invokeExpr.getMethod());
                if(!ret)
                    out.add(stopFlag);
                if(!Collections.disjoint(invokeExpr.getArgs(), in) || in.contains(((InterfaceInvokeExpr)invokeExpr).getBase()))
                {
                    return ret;
                }

            }
            /*
            else if(invokeExpr instanceof StaticInvokeExpr)
            {
                //include corner cases we found
                String fullName = method.getDeclaringClass().getName()+"#"+(method.getName());
                if(SHOULD_INCLUDE_METHODS.contains(fullName))
                    return true;

                //check if invokeExpr has any common elems compared with in set
                boolean ret = MethodPurityAnalysis.ifMethodPure(invokeExpr.getMethod());
                if(!ret)
                    out.add(stopFlag);
                if(!Collections.disjoint(invokeExpr.getArgs(), in))
                {
                    return ret;
                }

            }*/
        } else if (stmt instanceof IfStmt) {
            return true;
        } else if (stmt instanceof GotoStmt) {
            return true;
        }
        return false;
    }

    private void gen(Set in, Unit node, Set out) {
        gen(in,node,out,false);
    }
    private void gen(Set in, Unit node, Set out, boolean ifAssert) {
        Stmt stmt = (Stmt) node;

        if (stmt instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) stmt;
            Value rightOp = assignStmt.getRightOp();
            if (rightOp instanceof BinopExpr) {
                Value op1 = ((BinopExpr) rightOp).getOp1();
                Value op2 = ((BinopExpr) rightOp).getOp2();

                Value newOp1 = getLocal(op1);
                Value newOp2 = getLocal(op2);
                if (newOp1 != null) {
                    ((BinopExpr) rightOp).setOp1(newOp1);
                    out.add(newOp1);
                }
                if (newOp2 != null) {
                    ((BinopExpr) rightOp).setOp2(newOp2);
                    out.add(newOp2);
                }

            } else if (rightOp instanceof InvokeExpr) {
                InvokeExpr invokeExpr = ((InvokeExpr)rightOp);
                String mName = invokeExpr.getMethod().getName();
                if (!(mName.equals("<init>") || MethodPurityAnalysis.ifMethodPure(invokeExpr.getMethod()))) {
                    // for cassandra
                    if (!(invokeExpr.getArg(0) instanceof StringConstant && ((StringConstant) invokeExpr.getArg(0)).value.toLowerCase().contains("select"))){
                        // add the deleted operator back
                        out.add(assignStmt.getLeftOp());
                        return;
                    }
                }

                List<Value> args = ((InvokeExpr) rightOp).getArgs();
                for (int i = 0; i < args.size(); ++i) {
                    Value value = args.get(i);
                    Value newVal = getLocal(value);
                    if (newVal != null) {
                        ((InvokeExpr) rightOp).setArg(i, newVal);
                        out.add(newVal);
                    }
                }

                if (rightOp instanceof VirtualInvokeExpr) {
                    Value value = ((VirtualInvokeExpr) rightOp).getBase();
                    Value newVal = getLocal(value);
                    if (newVal != null)
                        out.add(newVal);
                }
                else if (rightOp instanceof InterfaceInvokeExpr) {
                    Value value = ((InterfaceInvokeExpr) rightOp).getBase();
                    Value newVal = getLocal(value);
                    if (newVal != null)
                        out.add(newVal);
                }

            }
            //$i2 = lengthof r40;
            else if (rightOp instanceof LengthExpr) {
                Value value = ((LengthExpr) rightOp).getOp();
                Value newVal = getLocal(value);
                if (newVal != null) {
                    ((LengthExpr) rightOp).setOp(newVal);
                    out.add(newVal);
                }

            } else if (rightOp instanceof CastExpr) {
                Value value = ((CastExpr) rightOp).getOp();
                Value newVal = getLocal(value);
                if (newVal != null) {
                    ((CastExpr) rightOp).setOp(newVal);
                    out.add(newVal);
                }

            } else if (rightOp instanceof JimpleLocal) {
                out.add(rightOp);
            } else if (rightOp instanceof ArrayRef)
            {
                out.add(((ArrayRef) rightOp).getBase());
                Value index = ((ArrayRef) rightOp).getIndex();
                if(!(index instanceof Constant))
                    out.add(index);
            }  else  if(rightOp instanceof NewExpr)
            {
                //do nothing
            } else if (rightOp instanceof InstanceOfExpr)
            {
                out.add(((InstanceOfExpr) rightOp).getOp());
            } else if (rightOp instanceof NewArrayExpr)
            {
                Value size = ((NewArrayExpr) rightOp).getSize();
                if(!(size instanceof Constant))
                    out.add(size);
            }
            else
            {
                LOG.warn("rightOp not generate anything!"+rightOp);
            }


            tobeCopiedStmts.add(stmt);
        }
        else if (stmt instanceof IdentityStmt)
        {
            Value rightOp = ((IdentityStmt)stmt).getRightOp();
            if(rightOp instanceof CaughtExceptionRef)
            {
                out.add(((IdentityStmt) stmt).getLeftOp());
                tobeCopiedStmts.add(stmt);
            }
        }
        else if (stmt instanceof InvokeStmt) {
            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
            if (invokeExpr instanceof SpecialInvokeExpr) {
                //handle cases like
                // specialinvoke r41.<org.apache.hadoop.fs.Path: void <init>(org.apache.hadoop.fs.Path,java.lang.String)>($r9, "file1");
                String mName = invokeExpr.getMethod().getName();
                if (mName.equals("<init>") ||
                        ((!Collections.disjoint(invokeExpr.getArgs(),in) &&
                        MethodPurityAnalysis.ifMethodPure(invokeExpr.getMethod()))))
                {
                    Value base = ((SpecialInvokeExpr) invokeExpr).getBase();
                    Value newBase = getLocal(base);
                    if(newBase!=null)
                    {
                        ((SpecialInvokeExpr) invokeExpr).setBase(newBase);
                        out.add(newBase);
                    }

                    List<Value> args = invokeExpr.getArgs();
                    for (int i = 0; i < args.size(); ++i) {
                        Value value = args.get(i);
                        Value newVal = getLocal(value);
                        if (newVal != null) {
                            invokeExpr.setArg(i, newVal);
                            out.add(newVal);
                        }
                    }

                    tobeCopiedStmts.add(stmt);
                }
            } else if (invokeExpr instanceof VirtualInvokeExpr)
            {
                //dangerous, consider a better idea
                VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;
                if((!Collections.disjoint(virtualInvokeExpr.getArgs(),in) ||
                        in.contains(virtualInvokeExpr.getBase()))
                                && MethodPurityAnalysis.ifMethodPure(virtualInvokeExpr.getMethod()))
                {
                    out.add(virtualInvokeExpr.getBase());
                    for(Value arg: virtualInvokeExpr.getArgs())
                        if(arg instanceof Local)
                            out.add(arg);

                    tobeCopiedStmts.add(stmt);
                }
            } else if (invokeExpr instanceof InterfaceInvokeExpr)
            {
                //dangerous, consider a better idea
                InterfaceInvokeExpr interfaceInvokeExpr = (InterfaceInvokeExpr) invokeExpr;
                if((!Collections.disjoint(interfaceInvokeExpr.getArgs(),in) ||
                        in.contains(interfaceInvokeExpr.getBase()))
                        && MethodPurityAnalysis.ifMethodPure(interfaceInvokeExpr.getMethod()))
                {
                    out.add(interfaceInvokeExpr.getBase());
                    for(Value arg: interfaceInvokeExpr.getArgs())
                        if(arg instanceof Local)
                            out.add(arg);

                    tobeCopiedStmts.add(stmt);
                }
            } else if (invokeExpr instanceof StaticInvokeExpr) {
                if (ifAssert ||
                        ((!Collections.disjoint(invokeExpr.getArgs(),in) &&
                                MethodPurityAnalysis.ifMethodPure(invokeExpr.getMethod()))))
                {
                    List<Value> args = invokeExpr.getArgs();
                    for (int i = 0; i < args.size(); ++i) {
                        Value value = args.get(i);
                        Value newVal = getLocal(value);
                        if (newVal != null) {
                            invokeExpr.setArg(i, newVal);
                            out.add(newVal);
                        }
                    }

                    tobeCopiedStmts.add(stmt);
                }
            }
        } else if (stmt instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) stmt;
            for (ValueBox box : ifStmt.getCondition().getUseBoxes())
            {
                LOG.info("box: "+box.toString());
                if (box.getValue() instanceof JimpleLocal)
                    out.add(box.getValue());
            }
            //tobeCopiedStmts.add(ifStmt.getTarget());
            tobeCopiedStmts.add(stmt);

        } else if (stmt instanceof GotoStmt) {
            GotoStmt gotoStmt = (GotoStmt) stmt;
            //tobeCopiedStmts.add((Stmt)gotoStmt.getTarget());
            tobeCopiedStmts.add(stmt);

        }

    }

    private Value getLocal(Value value) {
        if (value instanceof JimpleLocal)
            return value;
        if (value instanceof NullConstant)
        {
            return null;
        }
        if (value instanceof Constant) {
            Value newLocal = lg.generateLocal(value.getType());
            newLocalAssignStmts.add(Jimple.v().newAssignStmt(newLocal, value));
            return newLocal;
        }

        return null;
    }

    private void logSet(String setName, Set set) {
        LOG.info(setName + ":");
        String outStr = (String) set.stream()
                .map(x -> x.toString())
                .collect(Collectors.joining(", "));
        LOG.info("\t" + outStr);
    }
}
