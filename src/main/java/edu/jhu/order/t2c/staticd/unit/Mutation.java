package edu.jhu.order.t2c.staticd.unit;

import edu.jhu.order.t2c.staticd.util.SootUtils;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.Stmt;

public class Mutation {

    public SootMethod op;
    public SootField field;
    public Stmt instrumentStmt;

    //public List<Value> args;

    public String defStmt;
    public String usageStmt;

    public Mutation(SootMethod op, SootField field, Stmt instrumentStmt, String def,
            String usage) {
        this.op = op;
        this.field = field;
        this.instrumentStmt = instrumentStmt;
        this.defStmt = def;
        this.usageStmt = usage;
    }

    @Override
    public String toString() {

        return SootUtils.getMethodSignature(op) + "->" + field.getDeclaringClass().getName() + "@"
                + field.getName();
    }
}
