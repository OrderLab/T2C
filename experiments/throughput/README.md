# Throughput experiment

For this experiment, you need to disable prod log to achieve maximum throughput by doing
1. Modify `DISABLE_PRODINFO` to `true` in src/main/java/edu/jhu/order/t2c/dynamicd/runtime/T2CHelper.java
2. Rerun T2C compile, recover_tests, and retrofit