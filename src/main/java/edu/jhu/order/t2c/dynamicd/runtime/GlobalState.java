package edu.jhu.order.t2c.dynamicd.runtime;

public class GlobalState {

    public enum T2CMode {
        BUILD,
        PRODUCTION,
        //ASSERTING,
        //validate if template mutant is legal
        VALIDATION,
        //for T2C unit testing only
        UNITTEST,
        ILLEGAL;
    }
    public static T2CMode mode = T2CMode.PRODUCTION;
    //to avoid recursion when asserting
    public static volatile boolean ifAsserting = false;
    public static volatile boolean testStarted = false;
}
