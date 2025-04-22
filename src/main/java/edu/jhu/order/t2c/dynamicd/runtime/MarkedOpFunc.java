package edu.jhu.order.t2c.dynamicd.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//how to use:
//copy jar t2c-1.0-SNAPSHOT.jar (somehow at this moment the -with-dependency jar has incorrect service list) to
    //e.g. zookeeper 3.4.11 -> build/lib/

//and add
//  import edu.jhu.order.t2c.dynamicd.runtime.MarkedOp;
//
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MarkedOpFunc {

    String value() default "illegal";

    String opName = null;


}
