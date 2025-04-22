package edu.jhu.order.t2c.dynamic;

import edu.jhu.order.t2c.dynamicd.runtime.Constraint;
import edu.jhu.order.t2c.dynamicd.runtime.Symbol;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.jhu.order.t2c.dynamicd.runtime.Constraint.FORCE_ENABLE_TYPES_IN_TESTING;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConstraintTest {

    @BeforeClass
    public static void setup()
    {
        FORCE_ENABLE_TYPES_IN_TESTING = true;
    }

    @Test
    public void testInfer() {
        Map<String, Symbol> map = new HashMap<String, Symbol>(){{
            put("a", new Symbol("a","/a"));
            put("b", new Symbol("b","/b"));
            put("c", new Symbol("c","/a/b"));
        }};

        List<Constraint.ConstrainType> lst = Constraint.infer(map.get("a"),map.get("c"),map);
        assertTrue(lst.contains(Constraint.ConstrainType.CONTAINED_BY_NOT_EQ));
        assertTrue(lst.contains(Constraint.ConstrainType.PATH_PARENT));
        assertFalse(lst.contains(Constraint.ConstrainType.PATH_CHILDREN));

    }



    @Test
    public void testCheck() {
        Map<String, Symbol> map = new HashMap<String, Symbol>(){{
            put("a", new Symbol("a","/a"));
            put("b", new Symbol("b","/b"));
            put("c", new Symbol("c","/a/b"));
        }};

        Constraint c1 = new Constraint(Constraint.ConstrainType.PATH_PARENT, map.get("a"),map.get("c"));
        assertTrue(c1.check(map));
        Constraint c2 = new Constraint(Constraint.ConstrainType.PATH_PARENT, map.get("c"),map.get("a"));
        assertFalse(c2.check(map));
        Constraint c3 = new Constraint(Constraint.ConstrainType.PATH_CHILDREN, map.get("c"),map.get("a"));
        assertTrue(c3.check(map));
        Constraint c4 = new Constraint(Constraint.ConstrainType.PATH_PARENT, map.get("a"),map.get("b"));
        assertFalse(c4.check(map));
    }

}
