package edu.jhu.order.t2c.dynamicd.runtime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//relations between constraints, right now we only support binary relation
public class Constraint {

    //warning: currently we disable all types!
    public enum ConstrainType {
        LESSER("LESSER",false),
        GREATER("GREATER",false),
        //for ZK, e.g. /a -> /a/b
        PATH_PARENT("PATH_PARENT",false),
        PATH_CHILDREN("PATH_CHILDREN",false),
        //contains but not equal, more general compared to ZK cases e.g. /a -> /aaa
        CONTAINS_NOT_EQ("CONTAINS_NOT_EQ",false),
        CONTAINED_BY_NOT_EQ("CONTAINED_BY_NOT_EQ",false);

        private final String text;
        private final boolean enabled;

        /**
         * @param text
         */
        ConstrainType(final String text, final boolean enabled) {
            this.text = text;
            this.enabled = enabled;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
        }
    }

    public static boolean FORCE_ENABLE_TYPES_IN_TESTING = false;

    ConstrainType type;
    Symbol leftSym;
    Symbol rightSym;

    public Constraint(ConstrainType type, Symbol leftSym, Symbol rightSym) {
        this.type = type;
        this.leftSym = leftSym;
        this.rightSym = rightSym;
    }

    public boolean check(Map<String, Symbol> map) {
        try {
            //we can configure to disable some types, see definition
            if (!type.enabled && !FORCE_ENABLE_TYPES_IN_TESTING)
            {
                //System.out.println("WARN: skip checking for type: "+type);
                //should return false, otherwise will always infer
                return false;
            }

            if(map==null)
                throw new RuntimeException("IMPOSSIBLE! map is null!");
            if(leftSym.symbolName == null)
                throw new RuntimeException("IMPOSSIBLE! leftSym.symbolName is null!");
            if(rightSym.symbolName == null)
                throw new RuntimeException("IMPOSSIBLE! rightSym.symbolName is null!");

            Object leftVal = map.get(leftSym.symbolName).getObj();
            Object rightVal = map.get(rightSym.symbolName).getObj();

            if (leftVal == null || rightVal == null)
                return false;

            String leftString = leftVal.toString();
            String rightString = rightVal.toString();

            int compared = (leftString).compareTo(rightString);
            //only support string for now
            switch (type) {
                case CONTAINS_NOT_EQ:
                    return (leftString.contains(rightString) && !(leftString.equals(rightString)));
                case CONTAINED_BY_NOT_EQ:
                    return (rightString.contains(leftString) && !(rightString.equals(leftString)));
                case LESSER:
                    return compared > 0;
                case GREATER:
                    return compared < 0;
                case PATH_PARENT:
                    return
                            (rightString.startsWith(leftString)) &&
                                    rightString.length() > leftString.length() &&
                                    rightString.charAt(leftString.length()) == '/';
                case PATH_CHILDREN:
                    return
                            (leftString.startsWith(rightString)) &&
                                    leftString.length() > rightString.length() &&
                                    leftString.charAt(rightString.length()) == '/';
                default:
                    throw new RuntimeException("Not handled!");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<ConstrainType> infer(Symbol leftSym, Symbol rightSym, Map<String, Symbol> map)
    {
        List<ConstrainType> lst = new ArrayList<>();
        Constraint constraint = new Constraint(null, leftSym, rightSym);
        for (ConstrainType t : ConstrainType.values()) {
            constraint.type = t;
            if(constraint.check(map))
                lst.add(t);
        }
        return lst;
    }

    public static List<Constraint> inferAll(Map<String, Symbol> map)
    {
        List<Constraint> lst = new ArrayList<>();
        for (Map.Entry<String, Symbol> entry1 : map.entrySet()) {
            for (Map.Entry<String, Symbol> entry2 : map.entrySet()) {
                if(entry1.getKey().equals(entry2.getKey()))
                    continue;

                for(ConstrainType t: infer(entry1.getValue(), entry2.getValue(),map))
                {
                    Constraint c = new Constraint(t, entry1.getValue(), entry2.getValue());
                    lst.add(c);
                }
            }
        }
        return lst;
    }

    @Override
    public String toString() {
        return "Constraint{" +
                "type=" + type +
                ", leftSym=" + leftSym +
                ", rightSym=" + rightSym +
                '}';
    }
}
