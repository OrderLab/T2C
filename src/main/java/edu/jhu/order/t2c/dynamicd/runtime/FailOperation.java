package edu.jhu.order.t2c.dynamicd.runtime;

public class FailOperation extends Operation {

    public FailOperation() {
        super("FAIL_ASSERT_OP", "");
    }

    @Override
    void execute() throws Exception {
        throw new RuntimeException("This should not happen!");
    }
}
