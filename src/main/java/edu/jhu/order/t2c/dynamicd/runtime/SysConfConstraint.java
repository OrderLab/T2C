package edu.jhu.order.t2c.dynamicd.runtime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//relations between constraints, right now we only support binary relation
public class SysConfConstraint {

    String key;
    Object value;

    public SysConfConstraint(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public static void addSysConfConstraint(String key, String value)
    {
        TemplateManager.getInstance().currentTemplate.addSysConfigConstraint(key, value);
    }

    public boolean check()
    {
        if(System.getProperty(key) == null)
            return false;

        return System.getProperty(key).equals(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysConfConstraint that = (SysConfConstraint) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}
